import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

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

async function finish(
  eventRef: FirebaseFirestore.DocumentReference,
  winnerId: string
): Promise<void> {
  await eventRef.update({
    mvpId: winnerId,
    status: "FINISHED",
  });
}