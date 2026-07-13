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
* **TopAppBar:** Event title on the left. Two action icons on the right: `📊` (navigates to the Points Progression Graph screen) and `⚡` (triggers the Emergency Clause). No persistent scoreboard banner — the feed cards communicate score changes in real time.
* **Timeline:** Full-height `LazyColumn` of chronological note cards.
* **FAB:** `+ Post` button anchored to the bottom right, opens the Compose Bottom Sheet.

#### Note Card anatomy
Each card displays:
* Author avatar + name → Target avatar + name (both shown)
* Tier badge with label and point value (e.g., `HOT TAKE +2 pts`)
* Note content (text, photo, or GIF)
* Emoji reaction counts + timestamp

#### Compose Bottom Sheet (tier-first flow)
1. **Tier selection** — four options always displaying label and point value together: `Fact +1`, `Hot Take +2`, `Witnessed +5`, `Lore +10`.
2. **Target selection** — horizontal chip row of all participants, self excluded.
3. **Content type** — `📷 Photo`, `GIF`, `✏️ Text`.
4. **Content input** — text field or media picker depending on type selected.
5. **Post button.**

#### Feature implementations
* **Chronological Multimedia Wall:** Live Firestore snapshots (`addSnapshotListener`) feeding a real-time, highly synchronized chronological timeline across all participant devices.
* **Multimedia Capture:** Direct Firebase Storage integration for quick-snapping photos embedded within live notes.
* **Giphy SDK Core:** In-app GIF selection interface allowing users to react visually to real-time group highlights.
* **Points Progression Graph Screen:** Dedicated screen accessible via the `📊` TopAppBar icon. Displays a line chart (Vico Charts) tracking each participant's score over time throughout the event.
* **Peer-Driven Scoring System:**
    * **Event Log Feed:** Points are accumulated exclusively when a user publishes a timeline note (Text, Photo, or GIF) and explicitly targets/nominates another participant (self-targeting is locked).
    * **Tier Selection:** Before posting, the author picks a tier that defines the weight of the nomination. The label and point value are always shown together. Four tiers available: **Fact** (+1), **Hot Take** (+2), **Witnessed** (+5), **Lore** (+10). Tier labels are subject to change and the system may expand in future iterations.
    * **Note Card Display:** Each published note shows its tier label and point value so all participants can see the weight assigned.
    * **Social Interactions:** Other players can react with emojis on notes to build engagement, but reactions do not award additional points.
    * **Admin Moderation Power:** The Event Admin has the authority to delete any post from the timeline. Deleting a post automatically subtracts the tier's assigned points from the targeted user's score in real time.
* **Emergency Vote / Basic Services ("Cláusula Hospitalaria / Policía"):**
    * **Instant Crowd-Panic:** Any user can trigger this override via the `⚡` TopAppBar icon, targeting one participant (not themselves).
    * **Democratic Cross-App Pop-up:** Fires a blocking overlay to all connected devices. Users must choose to Accept or Decline.
    * **Timeout:** If the vote is not resolved (accepted or declined by all participants) within **3 minutes**, the request is automatically discarded and no points are awarded.
    * **Instant Match Point Victory:** If the absolute majority votes "Accept" before the timeout, the voting freezes, the event hits emergency shutdown, and the targeted user gets a massive point payload (`+1000.0 pts`) to win the preliminary board instantly.
    * **One-Shot Rule:** If the majority votes "Decline" or the request times out, the user who triggered it permanently loses the ability to fire the Emergency Clause again for the remainder of this event.

### 🔴 Sprint 4: The Final Awards Gala & Certification Engine
* **The Blind Ballot & Scoring Context:**
  * **The Admin officially closes the live event. The app transitions to an interactive screen displaying the preliminary standings scoreboard (e.g., "Top 3 are John, Nancy, and Jack" or a full metrics table) alongside a line chart tracking the timeline performance.**
  * **Every user must cast a secret blind vote for the final winner (self-voting programmatically locked).**
* **The Ultimate MVP Resolution: The official event MVP title is awarded strictly to the user who receives the most votes in this final ballot.**
* **The Final Gala Upload: Once the vote is resolved, the group has the option to upload a final gala photo to Cloud Storage (gala_photo.jpg).**
* **Automated Prediction Resolution: The system's background engine automatically audits pre-trip predictions against final database standings—completely bypassing any manual Admin validation screens. Correct guesses are pushed to the users' profiles as badges.**
* **Diploma Export System: Dynamic composable layout generating localized custom awards containing profile data, performance stats, and the gala photo uploaded at the end (if available, otherwise it renders clean without it). The winner can download it locally as a PDF/PNG anytime.**

### 🔵 Sprint 5: Hall of Fame & Global Standings
* **Leaderboard Dashboard:** Aggregate historical leaderboard computing performance points, total lifetime MVPs, and win ratios across all past events.
* **User Profile File:** Personalized profiles providing rapid review and re-download capability for all unlocked digital diplomas.
* **Archive Viewer Mode:** Past events can be opened by any participant in a read-only viewer mode, maintaining full access to the live timeline texts, photos, and historical graphs.

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
    "tripleBetsCorrect": "NUMBER"
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