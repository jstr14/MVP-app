import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

initializeApp();

const db = getFirestore();

/**
 * Fires every time a vote is cast in final_votes.
 * When all participants have voted, calculates the MVP winner and
 * transitions the event to FINISHED.
 *
 * Tiebreaker chain (applied in order):
 *   1. Most votes
 *   2. Highest cumulative points from points_log
 *   3. Most unique nominators (distinct authorIds in points_log)
 *   4. Most recent note received (latest timestamp in points_log)
 */
export const resolveGalaWinner = onDocumentCreated(
  {
    document: "events/{eventId}/final_votes/{voterId}",
    region: "europe-southwest1",
  },
  async (event) => {
    const { eventId } = event.params;
    const eventRef = db.collection("events").doc(eventId);
    const eventDoc = await eventRef.get();
    const eventData = eventDoc.data();

    if (!eventData) return;
    if (eventData.status !== "VOTING_PHASE") return;
    if (eventData.mvpId) return; // already resolved by a concurrent trigger

    const participants: string[] = eventData.participants ?? [];
    const totalParticipants = participants.length;

    const votesSnapshot = await db
      .collection("events")
      .doc(eventId)
      .collection("final_votes")
      .get();

    if (votesSnapshot.size < totalParticipants) return;

    // Step 1 — tally votes
    const voteTally: Record<string, number> = {};
    for (const doc of votesSnapshot.docs) {
      const votedForId = doc.data().votedForCandidateId as string;
      voteTally[votedForId] = (voteTally[votedForId] ?? 0) + 1;
    }

    const maxVotes = Math.max(...Object.values(voteTally));
    let candidates = Object.entries(voteTally)
      .filter(([, votes]) => votes === maxVotes)
      .map(([userId]) => userId);

    if (candidates.length === 1) {
      await finish(eventRef, candidates[0]);
      return;
    }

    // Fetch all points_log notes targeting any of the tied candidates
    const notesSnapshot = await db
      .collection("events")
      .doc(eventId)
      .collection("points_log")
      .where("targetUserId", "in", candidates)
      .get();

    // Step 2 — most cumulative points
    const pointsMap: Record<string, number> = {};
    for (const doc of notesSnapshot.docs) {
      const { targetUserId, pointsAwarded } = doc.data();
      pointsMap[targetUserId] = (pointsMap[targetUserId] ?? 0) + (pointsAwarded ?? 0);
    }
    const maxPoints = Math.max(...candidates.map((id) => pointsMap[id] ?? 0));
    candidates = candidates.filter((id) => (pointsMap[id] ?? 0) === maxPoints);

    if (candidates.length === 1) {
      await finish(eventRef, candidates[0]);
      return;
    }

    // Step 3 — most unique nominators
    const nominatorsMap: Record<string, Set<string>> = {};
    for (const doc of notesSnapshot.docs) {
      const { targetUserId, authorId } = doc.data();
      if (!nominatorsMap[targetUserId]) nominatorsMap[targetUserId] = new Set();
      nominatorsMap[targetUserId].add(authorId);
    }
    const maxNominators = Math.max(
      ...candidates.map((id) => nominatorsMap[id]?.size ?? 0)
    );
    candidates = candidates.filter(
      (id) => (nominatorsMap[id]?.size ?? 0) === maxNominators
    );

    if (candidates.length === 1) {
      await finish(eventRef, candidates[0]);
      return;
    }

    // Step 4 — most recent note received (latest timestamp)
    const latestTimestampMap: Record<string, number> = {};
    for (const doc of notesSnapshot.docs) {
      const { targetUserId, timestamp } = doc.data();
      const ts = typeof timestamp === "number" ? timestamp : timestamp?.toMillis?.() ?? 0;
      if ((latestTimestampMap[targetUserId] ?? 0) < ts) {
        latestTimestampMap[targetUserId] = ts;
      }
    }
    const winner = candidates.reduce((a, b) =>
      (latestTimestampMap[a] ?? 0) >= (latestTimestampMap[b] ?? 0) ? a : b
    );

    await finish(eventRef, winner);
  }
);

/**
 * Fires when an event document is updated.
 * When status transitions to FINISHED, checks every participant's prediction
 * against the final mvpId and awards Oracle / Triple badges.
 *
 * Oracle: projectedMvpId === mvpId  → stats.oraclePredictionsCorrect++
 * Triple: tripleParticipantId === mvpId → stats.tripleBetsCorrect++
 */
export const resolvePredictions = onDocumentUpdated(
  {
    document: "events/{eventId}",
    region: "europe-southwest1",
  },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;
    if (after.status !== "FINISHED") return;
    if (before.status === "FINISHED") return; // guard against duplicate triggers

    const mvpId = after.mvpId as string | undefined;
    if (!mvpId) return;

    const { eventId } = event.params;

    const predictionsSnapshot = await db
      .collection("events")
      .doc(eventId)
      .collection("predictions")
      .get();

    // Collect badge info per user — start with MVP
    type BadgeSet = { mvp: boolean; oracle: boolean; triple: boolean; reactor: boolean; totalPlayer: boolean };
    const userBadges: Record<string, BadgeSet> = {
      [mvpId]: { mvp: true, oracle: false, triple: false, reactor: false, totalPlayer: false },
    };

    for (const doc of predictionsSnapshot.docs) {
      const prediction = doc.data();
      const userId = doc.id;
      const oracle = prediction.projectedMvpId === mvpId;
      const triple = prediction.tripleParticipantId === mvpId;
      if (!oracle && !triple) continue;
      const existing = userBadges[userId] ?? { mvp: false, oracle: false, triple: false, reactor: false, totalPlayer: false };
      userBadges[userId] = { ...existing, oracle: existing.oracle || oracle, triple: existing.triple || triple };
    }

    // Count reactions given per participant across all points_log notes
    const pointsLogSnapshot = await db
      .collection("events")
      .doc(eventId)
      .collection("points_log")
      .get();

    const reactionCounts: Record<string, number> = {};
    for (const noteDoc of pointsLogSnapshot.docs) {
      const reactions = noteDoc.data().reactions as Record<string, string[]> | undefined;
      if (!reactions) continue;
      for (const userIds of Object.values(reactions)) {
        for (const userId of userIds) {
          reactionCounts[userId] = (reactionCounts[userId] ?? 0) + 1;
        }
      }
    }

    const MINIMUM_REACTIONS = 20;
    const maxReactions = Object.values(reactionCounts).length > 0
      ? Math.max(...Object.values(reactionCounts))
      : 0;

    if (maxReactions >= MINIMUM_REACTIONS) {
      const reactors = Object.entries(reactionCounts)
        .filter(([, count]) => count === maxReactions)
        .map(([userId]) => userId);
      for (const reactorId of reactors) {
        const existing = userBadges[reactorId] ?? { mvp: false, oracle: false, triple: false, reactor: false, totalPlayer: false };
        userBadges[reactorId] = { ...existing, reactor: true };
      }
    }

    // Jugador Total (totalPlayer): (MVP + Reactor) OR (Triple + Reactor) in the same event
    for (const [userId, badges] of Object.entries(userBadges)) {
      if (badges.reactor && (badges.mvp || badges.triple)) {
        userBadges[userId] = { ...badges, totalPlayer: true };
      }
    }

    const batch = db.batch();

    for (const [userId, badges] of Object.entries(userBadges)) {
      const userRef = db.collection("users").doc(userId);

      // Aggregate stats
      const statsUpdate: Record<string, unknown> = {};
      if (badges.mvp) statsUpdate["stats.lifetimeMvps"] = FieldValue.increment(1);
      if (badges.oracle) statsUpdate["stats.oraclePredictionsCorrect"] = FieldValue.increment(1);
      if (badges.triple) statsUpdate["stats.tripleBetsCorrect"] = FieldValue.increment(1);
      if (badges.reactor) statsUpdate["stats.reactorWins"] = FieldValue.increment(1);
      if (badges.totalPlayer) statsUpdate["stats.totalPlayerWins"] = FieldValue.increment(1);
      if (Object.keys(statsUpdate).length > 0) batch.update(userRef, statsUpdate);

      // Per-event badge record
      batch.set(userRef.collection("badges").doc(eventId), {
        eventId,
        eventTitle: after.title ?? "",
        startDate: after.startDate ?? null,
        endDate: after.endDate ?? null,
        mvp: badges.mvp,
        oracle: badges.oracle,
        triple: badges.triple,
        reactor: badges.reactor,
        totalPlayer: badges.totalPlayer,
      });
    }

    await batch.commit();
  }
);

async function finish(
  eventRef: FirebaseFirestore.DocumentReference,
  winnerId: string
): Promise<void> {
  await eventRef.update({
    mvpId: winnerId,
    status: "FINISHED",
  });
}

/**
 * Fires when a new emergency_request document is created.
 * Sends an FCM push notification (alarm sound channel) to all participants
 * except the triggerer.
 */
export const notifyEmergency = onDocumentCreated(
  {
    document: "events/{eventId}/emergency_requests/{requestId}",
    region: "europe-southwest1",
  },
  async (event) => {
    const { eventId } = event.params;
    const data = event.data?.data();
    if (!data) return;

    const triggeredById = data.triggeredById as string;

    const eventDoc = await db.collection("events").doc(eventId).get();
    const eventData = eventDoc.data();
    if (!eventData) return;

    const participants: string[] = eventData.participants ?? [];
    const otherParticipants = participants.filter((id) => id !== triggeredById);
    if (otherParticipants.length === 0) return;

    const triggererDoc = await db.collection("users").doc(triggeredById).get();
    const triggererName = (triggererDoc.data()?.name as string | undefined) ?? "A participant";

    const userDocs = await Promise.all(
      otherParticipants.map((uid) => db.collection("users").doc(uid).get())
    );
    const tokens: string[] = userDocs
      .map((doc) => doc.data()?.fcmToken as string | undefined)
      .filter((t): t is string => !!t);

    if (tokens.length === 0) return;

    const messaging = getMessaging();
    await Promise.all(
      tokens.map((token) =>
        messaging.send({
          token,
          data: {
            type: "emergency",
            eventId,
            triggeredById,
            targetUserId: data.targetUserId as string,
            triggererName,
          },
          android: { priority: "high" },
        })
      )
    );
  }
);

/**
 * Fires when an emergency_request document is updated (votes added).
 * Resolves the emergency when:
 * - Accept votes reach absolute majority → APPROVED_SHUTDOWN (event ends immediately)
 * - All participants voted with no majority → REJECTED
 * Client-side handles TIMED_OUT when countdown expires.
 */
export const resolveEmergency = onDocumentUpdated(
  {
    document: "events/{eventId}/emergency_requests/{requestId}",
    region: "europe-southwest1",
  },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;
    if (after.status !== "PENDING") return;

    const { eventId, requestId } = event.params;
    const triggeredById = after.triggeredById as string;
    const targetUserId = after.targetUserId as string;
    const votesAccept: string[] = after.votesAccept ?? [];
    const votesDecline: string[] = after.votesDecline ?? [];

    const eventDoc = await db.collection("events").doc(eventId).get();
    const eventData = eventDoc.data();
    if (!eventData) return;

    const totalParticipants: number = (eventData.participants as string[]).length;
    const majority = Math.ceil(totalParticipants / 2);

    const requestRef = db
      .collection("events")
      .doc(eventId)
      .collection("emergency_requests")
      .doc(requestId);
    const eventRef = db.collection("events").doc(eventId);

    if (votesAccept.length >= majority) {
      // Approved — end event immediately with +1000 pts entry
      const batch = db.batch();
      const logRef = db
        .collection("events")
        .doc(eventId)
        .collection("points_log")
        .doc();
      batch.set(logRef, {
        id: logRef.id,
        authorId: "system",
        targetUserId,
        type: "TEXT",
        textContent: "Emergency Clause Approved",
        tierLabel: "Emergency",
        pointsAwarded: 1000,
        timestamp: Date.now(),
        reactions: {},
      });
      batch.update(requestRef, { status: "APPROVED_SHUTDOWN" });
      batch.update(eventRef, {
        mvpId: targetUserId,
        status: "FINISHED",
        activeEmergencyId: null,
        usedEmergencyClause: FieldValue.arrayUnion(triggeredById),
      });
      await batch.commit();
    } else if (votesAccept.length + votesDecline.length >= totalParticipants) {
      // All voted, no majority → REJECTED
      const batch = db.batch();
      batch.update(requestRef, { status: "REJECTED" });
      batch.update(eventRef, {
        activeEmergencyId: null,
        usedEmergencyClause: FieldValue.arrayUnion(triggeredById),
      });
      await batch.commit();
    }
  }
);