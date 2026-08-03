# 🏆 Project Roadmap: "MVP"

"MVP" is a hybrid gamified event and trip manager tailored for groups of friends. It combines real-time logistics, a chronological multimedia feed, a multi-tier live scoring engine inspired by Mario Party dynamics, secret blind voting mechanics, and an automated final awards gala with dynamic charts and exported digital diplomas.

---

## 🛠️ Tech Stack & Architecture
* **Language:** Kotlin 2.x
* **UI Framework:** Jetpack Compose (Material 3)
* **Asynchronous Flow:** Kotlin Coroutines & Asynchronous StateFlows
* **Dependency Injection:** Hilt
* **Navigation:** Navigation Compose + `kotlinx.serialization` for type-safe routing
* **Backend Suite:** Firebase (Auth, Cloud Firestore, Cloud Storage, Cloud Messaging, Cloud Functions)
* **Data Serialization & Time:** `kotlinx.serialization` + `kotlinx-datetime`
* **Data Visualization:** Vico Charts

---

## 🗺️ Execution Milestones (Sprints)

### 🟢 Sprint 1: Architecture, Authentication, and Identity Core ✅
* **Project Setup:** Integrate Hilt, Jetpack Compose, Type-Safe Navigation, and Serialization.
* **Domain Layer:** Define explicit interface blueprints for Repositories (`AuthRepository`, `EventsRepository`, `UserRepository`).
* **Authentication:** Implement modern Android `Credential Manager` API for seamless Google Sign-In bottom-sheet flows.
* **User Profile Sync:** On every Google login, upsert user payload data (`uid`, `name`, `email`, `photoUrl`) into the `/users` Firestore directory via `AuthViewModel`. This ensures the user is always discoverable by email for the invite system.
* **Reactive Router System:** Maintain a global `NavHost` state router branching dynamically: `Splash` ➡️ `Login` ➡️ `Home (Dashboard)` ➡️ `EventDetails`.

### 🟡 Sprint 2: Event Lifecycle Management & Pre-Trip Strategy
* **Event Creation:** ✅ Create events with metadata: name, date range, optional location. Creator is automatically added as first participant and admin.
* **Event Details Screen:** ✅ Dedicated screen showing event status, date range, location, confirmed participants, and pending invitations (visible to all). Admin-only actions (invite, remove, start) are gated behind role checks.
* **Participant Invite System:** ✅
    * Admin searches by exact email — querying the `/users` collection directly.
    * **Registered user found:** UID added to `pendingParticipants[]` immediately.
    * **Unregistered email:** Email stored in `pendingEmails[]`. On the user's first login, the system automatically binds their UID — moving them from `pendingEmails` to `pendingParticipants`.
    * Admin can remove confirmed participants and cancel pending invitations (UID or email) during `PRE_TRIP`.
* **Push Notification on Invite:** When a user is added to `pendingParticipants[]`, a Firebase Cloud Function triggers an FCM push notification to the invited user's device, so they are immediately aware of the invitation without needing to open the app.
* **Dashboard Split UI (`HomeScreen`):** Four sections in order:
    * **Active Event:** ✅ Exactly one (1) live event the user can fully interact with. FAB to create a new event is hidden while an active event exists.
    * **Pending Invitations:** ✅ Events where the user is in `pendingParticipants[]`. Each card shows event info and Accept / Decline buttons. Accepting moves the user from `pendingParticipants[]` to `participants[]`. No navigation into the event until accepted.
    * **Upcoming Events:** ✅ Events where the user is in `participants[]` but already has an active event. Read-only cards with a lock indicator — navigation is blocked until the active event ends. Activates automatically once the current event finishes.
    * **Past Events Feed:** ✅ Finished events in read-only Viewer Mode showing MVP winner badge (only shown if the current user won).
* **Open Predictions:** ✅ Admin-only action in the `EventDetailsScreen` TopAppBar (visible during `PRE_TRIP`). Transitions the event from `PRE_TRIP` to `PREDICTION`.
    * Atomically clears `pendingParticipants[]` and `pendingEmails[]` — pending users missed their window.
    * Participant pool is now final and frozen. All confirmed participants receive a push notification to submit their picks (Firebase Cloud Function + FCM).
* **Start Event:** ✅ Admin-only action in the `EventDetailsScreen` TopAppBar (visible during `PREDICTION`). Transitions the event from `PREDICTION` to `ON_GOING`, freezing all submitted predictions.
    * Participants who never submitted a prediction forfeit Oracle/Triple badge eligibility for this event. No late submissions allowed once `ON_GOING`.
* **The "Triple" Prediction Setup:** ✅
    * Available to all confirmed participants during `PREDICTION` state.
    * Inputs required: Projected Event MVP Winner + "The Triple" (A high-stakes wildcard prediction, chosen exclusively from the finalized `participants[]` — self-selection locked).
    * **Prediction visibility rules:**
        * `PREDICTION`: each user sees and can edit only their own prediction. Others' are hidden.
        * `ON_GOING`: your prediction is shown in read-only mode inside `EventDetailsScreen` as a personal reminder card. Others' still hidden.
        * `VOTING_PHASE` / `FINISHED`: full reveal of all predictions alongside the final results in the gala screen.

### 🟠 Sprint 3: The Live Event Feed (Real-Time Lore & Micro-Scoring)

#### Screen Layout
The Live Event Feed is a single screen with the following structure:
* **TopAppBar:** Event title in the center. Actions on the right: `HowToVote` icon (admin only — End Event with confirmation dialog), `Info` icon (navigates to Event Details), `ShowChart` icon (navigates to Score Graph). The `⚡` Emergency Clause button is deferred. No persistent scoreboard banner.
* **Timeline:** Full-height `LazyColumn` of note cards ordered by most recent first (descending timestamp).
* **FAB:** `+ Post` button anchored to the bottom right, opens the Compose Bottom Sheet.

#### Navigation Rules
* **`PRE_TRIP` / `PREDICTION`:** Opening an event from Home navigates to `EventDetailsScreen`. LiveFeed is not accessible yet.
* **`ON_GOING`:** Opening an event from Home navigates directly to `LiveFeedScreen`. When the admin taps "Start Event" in EventDetails, the app auto-navigates to LiveFeed and pops EventDetails off the back stack (back → Home).
* **EventDetails during `ON_GOING`:** Accessible from LiveFeed via the `ℹ️` TopAppBar icon for reviewing participants and predictions. Back from EventDetails returns to LiveFeed.

#### Note Card anatomy
Each card displays:
* Author avatar + name → Target avatar + name (both shown)
* Tier badge with label and point value (e.g., `HOT TAKE +2 pts`)
* Note content (text, photo, or GIF) — tapping a photo opens a full-screen viewer (`FullScreenPhotoViewer`, reusable core component)
* Timestamp
* Delete button — visible to the Event Admin (can delete any note) and to the note's author (can delete their own notes only). Deleting a note subtracts its tier points from the targeted user's score in real time.
* Emoji reaction row with real-time counts, add-reaction picker, and the custom `ic_add_reaction` icon

#### Compose Bottom Sheet (tier-first flow)
1. **Tier selection** — four options always displaying label and point value together: `Fact +1`, `Hot Take +2`, `Witnessed +5`, `Lore +10`.
2. **Target selection** — horizontal chip row of all participants, self excluded.
3. **Text input** — optional multiline field, always visible.
4. **Media attachment** — `📷 Camera` and `🖼 Gallery` buttons. Mutually exclusive with each other but combinable with text. When a photo is selected a preview thumbnail is shown with an `✕` to remove it. GIF attachment is deferred pending Giphy API key setup.
5. **Post button** — enabled when tier + target are selected AND at least one of (text is not blank OR a media attachment is present).

**Note type resolution:** `PHOTO` if a photo is attached (regardless of whether text is also present), `GIF` if a GIF is attached, `TEXT` if text only. Both `textContent` and `contentUrl` can be populated simultaneously on the same note.

#### Feature implementations
* ✅ **Chronological Multimedia Wall:** Live Firestore snapshots (`addSnapshotListener`) feeding a real-time, highly synchronized chronological timeline across all participant devices.
* ✅ **Multimedia Capture:** Camera + gallery photo picker, compressed via `ImageCompressor` (max 1280px, JPEG 80%, EXIF rotation fix) before upload to Firebase Storage.
* ✅ **Giphy SDK Core:** In-app GIF selection via `GiphyDialogFragment`. Giphy CDN URL stored directly as `contentUrl` — no Firebase Storage upload needed. Animated GIF rendering via Coil with `ImageDecoderDecoder` / `GifDecoder`.
* ✅ **Points Progression Graph Screen:** Dedicated screen accessible via the `Graph` TopAppBar text button. Displays a Vico Charts line chart tracking each participant's cumulative score over time (one line per participant, colour-coded). Standings list below the chart sorted by total score.
* ✅ **Peer-Driven Scoring System:**
    * **Event Log Feed:** Points are accumulated exclusively when a user publishes a timeline note (Text, Photo, or GIF) and explicitly targets/nominates another participant (self-targeting is locked).
    * **Tier Selection:** Before posting, the author picks a tier that defines the weight of the nomination. The label and point value are always shown together. Four tiers available: **Fact** (+1), **Hot Take** (+2), **Witnessed** (+5), **Lore** (+10). Tier labels are subject to change and the system may expand in future iterations. Display names are localised via `displayName()` Composable extension (EN: Fact / Hot Take / Witnessed / Lore; ES: Facto / Hot Take / Presenciado / Leyenda) while Firestore storage keys remain in English.
    * **Note Card Display:** Each published note shows its tier label and point value so all participants can see the weight assigned.
    * **Social Interactions:** ✅ Emoji reactions (😂 🔥 😭 👀 🤡 👆 🚨 🤮 💩 👻) with real-time counts and a custom add-reaction chip.
    * **Note Deletion:** Admin can delete any note; the note's own author can also delete their own notes. Deletion subtracts the tier's assigned points from the targeted user's score in real time.
* ✅ **Full-Screen Photo Viewer** — tapping a photo or GIF in a note card opens a full-screen `FullScreenPhotoViewer` composable (black backdrop, `ContentScale.Fit`, tap or ✕ to dismiss). Reusable core component also used in the Gala screen.
* **Emergency Vote ("Servicios básicos"):** *(deferred — implement after Sprint 4 event-end flow is complete)*

    **Trigger flow:**
    * Any participant can tap the `⚡` TopAppBar icon in `LiveFeedScreen` to initiate the Emergency Clause. The button is disabled if the user has already spent their one-shot or if an emergency is already active.
    * Tapping `⚡` opens a target selection bottom sheet (all participants except self). Selecting a target shows a confirmation dialog warning that this is a one-time action.
    * On confirm, a new document is created in `/events/{eventId}/emergency_requests/{requestId}` with `status = PENDING`, `expiresAt = now + 3 minutes`, and `votesAccept = [triggeredById]` (triggerer's vote is automatically cast as Accept).
    * The event document is updated with `activeEmergencyId = requestId`.

    **Blocking overlay (shown on all devices when `activeEmergencyId` is set):**
    * Full-screen non-dismissible overlay that appears on top of the feed for all participants.
    * All participants must vote — the overlay cannot be closed until the vote is resolved or times out.
    * **Triggerer view:** shows who they nominated, live vote counts (✓ Accept / ✗ Decline / ⏳ Pending), countdown timer, and a "Waiting for the group to vote…" message. No vote buttons — their vote was auto-cast.
    * **All other participants' view:** shows who triggered it and who the target is, live vote counts, countdown timer, and Accept / Decline buttons. Buttons are disabled after voting.
    * **Phone button:** A prominent phone icon in the overlay opens the system dialer pre-filled with `112` (universal European emergency number) via `Intent(ACTION_DIAL, Uri.parse("tel:112"))`. This provides a real safety affordance in line with the "Cláusula Hospitalaria / Policía" name — no auto-call, just opens the dialer.

    **Resolution:**
    * **APPROVED_SHUTDOWN:** If `votesAccept.size` reaches an absolute majority of all participants before the timeout, the event follows the same end-of-event flow as a normal admin close (see Sprint 4). The target participant is set as the winner (`mvpId = targetUserId`) and the event transitions to the post-event screens. The +1000pts payload is recorded as a special entry in `points_log`.
    * **REJECTED:** If all non-triggerer participants have voted and Accept did not reach majority, status → `REJECTED`. The event continues. The triggerer's UID is added to `usedEmergencyClause[]` on the event document.
    * **TIMED_OUT:** If `expiresAt` passes with no majority reached, status → `TIMED_OUT`. The event continues. The triggerer's UID is added to `usedEmergencyClause[]`.
    * In all cases, `activeEmergencyId` is cleared on the event document after resolution.

    **One-Shot Rule:** Once a user's emergency attempt ends in REJECTED or TIMED_OUT, the `⚡` button is permanently disabled for them for the remainder of this event (enforced via `usedEmergencyClause[]` on the event document).

### 🔴 Sprint 4: The Final Awards Gala & Certification Engine

#### ✅ Closing the Live Event (Admin)
An admin-only "End Event" button in the `LiveFeedScreen` TopAppBar (with a confirmation dialog) transitions the event from `ON_GOING` to `VOTING_PHASE`. All participants' Firestore listeners detect the status change in real time and auto-navigate to the Gala screen.

#### ✅ Gala Screen Layout & Flow

**State 1 — Voting in progress (`VOTING_PHASE`, not all votes cast):**
* ✅ **Preliminary Standings** — top 3 participants by cumulative `points_log` total, displayed with medal icons (🥇🥈🥉), avatar, name, and points. No graph.
* ✅ **Blind Ballot** — participant chip row (self excluded, one selectable at a time). Once submitted, the ballot is hidden and replaced with a confirmation line. Self-voting is programmatically locked.
* ✅ **Vote counter** — `"X / Y votes cast"` updates in real time. When all votes are in, shows "Calculating winner…" with a spinner while the Cloud Function runs.

**State 2 — Winner revealed (`FINISHED`, `mvpId` set):**
* ✅ **MVP Winner card** — prominent display with avatar, trophy icon, and name.
* ✅ **Preliminary Standings** — same top 3 strip as above.
* ✅ **Your Votes section** — shown in both VOTING_PHASE and FINISHED: pre-event predictions (🔮 MVP Pick + 🏀 Triple Pick) and the blind ballot final vote (🗳). Predictions use the existing `predictions/{userId}` subcollection; final vote target from `final_votes/{userId}`.
* ✅ **Gala Photo** — admin uploads via camera or gallery. Once uploaded, visible to all participants as a tappable thumbnail (opens `FullScreenPhotoViewer`). All participants can save the photo to their device via `DownloadManager`. Admin can replace the photo after upload.
* ✅ **Download Certificate** — visible to the MVP winner only. Generates a landscape A4 PDF saved directly to the Downloads folder. See Diploma Export section below.
* ✅ **Access from Home** — `FINISHED` and `VOTING_PHASE` events navigate directly to the Gala screen from Home. Past events section is now tappable.

**Navigation:**
* `VOTING_PHASE` → Gala screen (direct from Home)
* `FINISHED` → Gala screen (from past events section, read-only view mode for photo and certificate)
* `ShowChart` icon in Gala TopAppBar → Score Progression Graph screen
* `DynamicFeed` icon in Gala TopAppBar → Live Feed in viewer mode (read-only: no FAB, no delete, no reactions, no End Event button)

**Live Feed viewer mode** (`viewerMode = true`): accessible from Gala screen for completed events. Shows the full chronological note timeline in read-only state — tapping photos/GIFs still opens `FullScreenPhotoViewer`.

#### MVP Resolution — Cloud Function (`resolveGalaWinner`)
Winner resolution runs entirely server-side to avoid client-side race conditions.

* **Trigger:** `onDocumentCreated("events/{eventId}/final_votes/{voterId}")`
* **Logic:**
  1. Count documents in `final_votes`. If count < `participants.length` → exit, not done yet.
  2. Tally `votedForCandidateId` across all votes.
  3. Winner = candidate with most votes.
  4. **Tiebreaker chain** (applied in order until a single winner emerges):
     * **T1** — highest cumulative `pointsAwarded` total from `points_log`
     * **T2** — most unique nominators (distinct `authorId` values in `points_log`)
     * **T3** — most recent note received (latest `timestamp` in `points_log`)
  5. Write `{ mvpId: winnerId, status: "FINISHED" }` to the event document.
* All clients detect `status == FINISHED` via their existing `observeEvent` listener and update the Gala screen automatically.

#### ✅ Badges Screen
Accessible from the Home screen profile avatar dropdown. Displays the current user's achievements with count and earned/locked states. Earned badges show `× N` count in primary color; locked badges show a 🔒 icon at 35% opacity.

**Regular badges (locked state shows actual name + motivational hint):**
* **🏆 MVP** (`lifetimeMvps`) — "Event champion" / "Win an event to earn this badge"
* **🔮 Oracle** (`oraclePredictionsCorrect`) — "Predicted the event winner" / motivational hint
* **🏀 Triple** (`tripleBetsCorrect`) — "Shoot your triple and call the event winner" / hint

**Mystery badges (locked state shows ❓, title `"???"`, mystery description — identity hidden until earned):**
* **⚡ Reactor** (`reactorWins`) — awarded to participant(s) who gave the most reactions in an event (minimum 20). Ties both win. Revealed as "Reactor — La persona más reactiva del evento" on earn.
* **🌟 All-Rounder / Jugador Total** (`totalPlayerWins`) — awarded when a participant is both Reactor AND (MVP OR Triple) in the same event. Mystery until earned.

**Easter egg:** tapping the Reactor row 7 times plays `waluigi-sound.mp3` and shows `waluigi_sticker.png` with a stamp spring animation (scale + rotation bounce). Counter resets immediately on trigger. Auto-dismisses after 2.5s or on tap.

Per-event badge records stored at `/users/{userId}/badges/{eventId}` for potential future detailed view.

#### ✅ Automated Prediction & Badge Resolution — Cloud Function (`resolvePredictions`)
Triggered by `onDocumentUpdated("events/{eventId}")` when `status` transitions to `FINISHED`. Awards all badges atomically via a single Firestore batch:
* **Oracle**: `projectedMvpId == mvpId` → `stats.oraclePredictionsCorrect++`
* **Triple**: `tripleParticipantId == mvpId` → `stats.tripleBetsCorrect++`
* **MVP**: winner → `stats.lifetimeMvps++`
* **Reactor**: participant(s) with the most reactions given across `points_log`, if ≥ 20 reactions. Ties both win. → `stats.reactorWins++`
* **All-Rounder**: participant who is Reactor AND (MVP OR Triple) → `stats.totalPlayerWins++`
* Writes per-event badge record to `/users/{userId}/badges/{eventId}` for all badge earners.
* Guard against duplicate triggers: exits if `before.status === "FINISHED"`
* Deployed in `europe-southwest1` alongside `resolveGalaWinner`.

#### ✅ Diploma Export System
Landscape A4 PDF generated using Android's built-in `PdfDocument` API — no external library. Device language used for all text (EN/ES).

**Layout:** decorative double border (navy outer + gold inner with corner ornaments). Two-line title: "Winner" / "Ganador" (secondary) + event name (bold). Event dates below. Gala photo (if uploaded). Certificate text: "This diploma certifies that [Name] is the MVP of this event". Vote description using a 4-tier system: Unanimous Victory / Overwhelming Victory (≥75%) / Clear Victory (≥50%) / Hard-fought Victory (<50%) — localised in EN and ES. Custom `stamp_winner.png` seal bottom-right.

**Delivery:** saved directly to the device Downloads folder via `MediaStore` (API 29+) or direct file copy (API < 29). System download notification shown after save (tap to open PDF); `POST_NOTIFICATIONS` requested at runtime on API 33+; snackbar fallback if denied.

**Filename:** `mvp_certificate_{EventName}.pdf` — event title sanitised for filesystem safety.

**Access:** "Download Certificate" button in Gala screen, visible to MVP winner only.

### 🔵 Sprint 5: Hall of Fame & Global Standings

* ✅ **Archive Viewer Mode:** Past events accessible in read-only mode via the Live Feed viewer (`viewerMode = true`) launched from the Gala screen. Full timeline, photos and GIFs viewable; `FullScreenPhotoViewer` available on tap.
* ✅ **User Profile:** Badges screen implemented in Sprint 4 — shows all 5 achievement types with earned/locked states and per-event records stored for future use. No further profile data planned at this stage.
* **Leaderboard Dashboard:** *(deferred — future development)* Aggregate historical leaderboard computing lifetime MVPs, win ratios and top performers across all past events.

### 🟣 Sprint 6: App Icon & UI/UX Polish

#### App Icon
* Custom adaptive icon (foreground + background layers) for Android 8+ using the `mipmap-anydpi-v26` format.
* Legacy icon for Android < 8 in all density buckets (`mipmap-mdpi` through `mipmap-xxxhdpi`).
* Notification icon (monochrome, used in status bar and notification drawer).

#### UI/UX Improvements *(scope TBD)*
* **Loading states** — skeleton screens or shimmer placeholders for lists and card sections that currently show a spinner.
* **Empty states** — illustrated or branded empty state views for sections with no data (no events, no past events, no notes in feed, etc.).
* **Transitions & animations** — screen enter/exit transitions, shared element transitions where relevant.
* **Error states** — user-friendly error screens with retry actions instead of raw snackbar messages.
* **Haptic feedback** — light haptic on key interactions (vote cast, reaction added, easter egg, etc.).
* **Accessibility** — content descriptions audit, minimum touch target sizes, color contrast review.
* **Typography & spacing** — visual consistency pass across all screens.

---

## 📐 Data Schemas & Architecture Blueprints

### 🗄️ Cloud Firestore Layout

#### `/users/{userId}`
```json
{
  "uid": "STRING (Primary Key)",
  "name": "STRING",
  "email": "STRING",
  "photoUrl": "STRING",
  "stats": {
    "lifetimeMvps": "NUMBER",
    "oraclePredictionsCorrect": "NUMBER",
    "tripleBetsCorrect": "NUMBER",
    "reactorWins": "NUMBER",
    "totalPlayerWins": "NUMBER"
  },
  "createdAt": "TIMESTAMP"
}
```
#### `/events/{eventId}`
```json
{
  "eventId": "STRING (Primary Key)",
  "title": "STRING",
  "status": "STRING (PRE_TRIP | PREDICTION | ON_GOING | VOTING_PHASE | FINISHED)",
  "adminId": "STRING",
  "startDate": "TIMESTAMP",
  "endDate": "TIMESTAMP",
  "locationLabel": "STRING (Nullable)",
  "latitude": "NUMBER (Nullable)",
  "longitude": "NUMBER (Nullable)",
  "participants": ["STRING (User UIDs — confirmed, active)"],
  "pendingParticipants": ["STRING (User UIDs — invited, awaiting response)"],
  "pendingEmails": ["STRING (Emails of invited users not yet registered)"],
  "mvpId": "STRING (Nullable)",
  "galaPhotoUrl": "STRING (Nullable)",
  "activeEmergencyId": "STRING (Nullable)",
  "usedEmergencyClause": ["STRING (User UIDs who have spent their one-shot)"],
  "createdAt": "TIMESTAMP"
}
```
#### `/events/{eventId}/predictions/{userId}`
```json
{
  "projectedMvpId": "STRING (User UID)",
  "tripleParticipantId": "STRING (User UID chosen from participant pool)"
}
```
#### `/events/{eventId}/emergency_requests/{requestId}`
```json
{
  "requestId": "STRING (Primary Key)",
  "triggeredById": "STRING",
  "targetUserId": "STRING",
  "votesAccept": ["STRING (User UIDs)"],
  "votesDecline": ["STRING (User UIDs)"],
  "status": "STRING (PENDING | APPROVED_SHUTDOWN | REJECTED | TIMED_OUT)",
  "expiresAt": "TIMESTAMP (triggeredAt + 3 minutes)",
  "timestamp": "TIMESTAMP"
}
```
#### `/events/{eventId}/points_log/{logId}`
```json
{
  "logId": "STRING",
  "authorId": "STRING",
  "targetUserId": "STRING",
  "type": "STRING (TEXT | PHOTO | GIF)",
  "contentUrl": "STRING (Nullable)",
  "textContent": "STRING (Nullable)",
  "pointsAwarded": "NUMBER (1 | 2 | 5 | 10)",
  "tierLabel": "STRING (Fact | Hot Take | Witnessed | Lore)",
  "timestamp": "TIMESTAMP"
}
```

#### `/events/{eventId}/final_votes/{voterId}`
```json
{
  "votedForCandidateId": "STRING (User UID)"
}
```

### 🗂️ Cloud Storage Directory Hierarchy

```text
storage/
└── events/
    └── {eventId}/
        ├── gala_photo.jpg          # Optional group photo uploaded during the final awards gala for the diploma
        └── final_gala.jpg   # High-resolution group cover photo used for the diploma
```
## 📊 Live Event Scoreboard (Modifies Event Graph In Real-Time)

### Peer Timeline Note Tiers
When publishing a note (Text, Photo, or GIF) targeting another participant, the author selects one of the following tiers. The label and its point value are always displayed together during selection. Labels are subject to change and the tier list may expand in future iterations.

| Tier Label     | Value        | Vibe |
|:---------------|:-------------|:-----|
| **Fact**       | `+1 pt`      | A cold, neutral observation. No judgment, just the record. |
| **Hot Take**   | `+2 pts`     | An opinionated call. You saw it, you're saying it. |
| **Witnessed**  | `+5 pts`     | The group saw it. It's documented. *(label subject to change)* |
| **Lore**       | `+10 pts`    | This moment is now part of the group's history. Reserved for the truly legendary. |

| Action                          | Value         | Engine Tracking Trigger |
|:--------------------------------|:--------------|:------------------------|
| **Peer Timeline Note**          | `+1 to +10 pts` | Published note targeting another participant (Text, Photo, or GIF). Tier chosen by author at post time. |
| **Emergency Clause Approved**   | `+1000.0 pts` | Triggered by a player, validated by majority vote. Grants instant match victory. |

## 📊 Post-Event Profile Achievements (Hall of Fame Records)

| Achievement | Tracking Logic                                               | Profile Impact  |
| :--- |:-------------------------------------------------------------|:----------------|
| **Event MVP Winner** | Highest live score tracker or emergency clause recipent      | +1 Lifetime MVP |
| **Oracle Badge** | Pre-trip prediction matched the final MVP winner             | +1 Oracle Win   |
| **The Triple Badge** | Pre-trip wildcard participant guess matched the final metric | +1 Triple Win   |