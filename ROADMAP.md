# 🏆 Project Roadmap: "MVP"

"MVP" is a hybrid gamified event and trip manager tailored for groups of friends. It combines real-time logistics, a chronological multimedia feed, a multi-tier live scoring engine inspired by Mario Party dynamics, secret blind voting mechanics, and an automated final awards gala with dynamic charts and exported digital diplomas.

---

## 🛠️ Tech Stack & Architecture
* **Language:** Kotlin 2.x
* **UI Framework:** Jetpack Compose (Material 3)
* **Asynchronous Flow:** Kotlin Coroutines & Asynchronous StateFlows
* **Dependency Injection:** Hilt
* **Navigation:** Navigation Compose + `kotlinx.serialization` for type-safe routing
* **Backend Suite:** Firebase (Auth, Cloud Firestore, Cloud Storage, Cloud Messaging)
* **Data Serialization & Time:** `kotlinx.serialization` + `kotlinx-datetime`
* **Data Visualization:** Vico Charts

---

## 🗺️ Execution Milestones (Sprints)

### 🟢 Sprint 1: Architecture, Authentication, and Identity Core
* **Project Setup:** Integrate Hilt, Jetpack Compose, Type-Safe Navigation, and Serialization.
* **Domain Layer:** Define explicit interface blueprints for Repositories (`AuthRepository`, `EventsRepository`, `StorageRepository`).
* **Authentication:** Implement modern Android `Credential Manager` API for seamless Google Sign-In bottom-sheet flows.
* **User Profile Sync:** On first-time Google logins, upsert user payload data (`uid`, `displayName`, `email`, `photoUrl`) into the `/users` Firestore directory.
* **Reactive Router System:** Maintain a global `NavHost` state router branching dynamically: `Splash` ➡️ `Login` ➡️ `Home (Dashboard)` ➡️ `EventDetails`.

### 🟡 Sprint 2: Event Lifecycle Management & Pre-Trip Strategy
* **Event Creation Log:**
    * Create events containing metadata: name, date range, location, and an initial user roles.
    * Location is captured via a short user-defined label (e.g. "Beach house", "Madrid") plus optional coordinates obtained from a **"Use current location"** button backed by `FusedLocationProviderClient` — no extra SDK required beyond Google Play Services.
    * Storing `latitude` / `longitude` alongside the label avoids a Geocoding API round-trip and makes the map stretch goal trivially simple to implement later.
    * **Stretch goal (time permitting):** Display the saved coordinates as a pinned marker on a small embedded Google Map in the Event Details screen.
    * Friend lookup feature using exact email matching queries directly targeting the `/users` collection. Inviting participants pushes a real-time system notification.
    * Support for "Pending Invitations" to bind users who register with Google post-event creation.
* **Event Editing (Admin only):**
    * The event Admin can edit event metadata (name, date range, location label, and coordinates) at any time while the event status is `PRE_TRIP`.
    * Editing is locked once the event transitions to `ON_GOING` to preserve scoring integrity.
    * The Edit screen reuses the same form as creation, pre-populated with the current values.
* **Dashboard Split UI (`HomeScreen`):**
    * **Active Section:** Displays exactly one (1) primary live event in progress.
    * **Past Events Feed:** Displays finished historical events in a read-only "Viewer Mode" explicitly highlighting the final MVP winner badge (mvpId).
* **The "Triple" Prediction Setup:**
    * Once the Admin closes the participant pool, all active players must submit a pre-trip prophecy.
    * Inputs required: Projected Event MVP Winner + "The Triple" (A high-stakes wildcard prediction, chosen exclusively from the finalized list of event participants).
    * System Admin switches state to `ON_GOING` to freeze predictions and start the live event.

### 🟠 Sprint 3: The Live Event Feed (Real-Time Lore & Micro-Scoring)
* **Chronological Multimedia Wall:** Live Firestore snapshots (`addSnapshotListener`) feeding a real-time, highly synchronized chronological timeline across all participant devices.
* **Multimedia Capture:** Direct Firebase Storage integration for quick-snapping photos embedded within live notes.
* **Giphy SDK Core:** In-app GIF selection interface allowing users to react visually to real-time group highlights.
* **Peer-Driven Scoring System:**
    * **Event Log Feed:** Points are accumulated exclusively when a user publishes a timeline note (Text, Photo, or GIF) and explicitly targets/nominates another participant (self-targeting is locked).
    * **Social Interactions:** Other players can react with emojis on notes to build engagement, but reactions do not award additional points.
    * **Admin Moderation Power:** The Event Admin has the authority to delete any post from the timeline. Deleting a post automatically subtracts the assigned points from the targeted user's score in real time.
* **Emergency Vote / Basic Services ("Cláusula Hospitalaria / Policía"):**
    * **Instant Crowd-Panic:** Any user can trigger this override, targeting one participant (not themselves).
    * **Democratic Cross-App Pop-up:** Fires a blocking notification overlay to all connected devices. Users must choose to Accept or Cancel.
    * **Instant Match Point Victory:** If the absolute majority votes "Yes":
        * The targeted user is awarded `+1000.0 pts` on the scoreboard.
        * The event status jumps **directly to `FINISHED`**, bypassing `VOTING_PHASE` entirely.
        * The targeted user is written as `mvpId` — no blind ballot takes place.
        * Prediction resolution runs automatically using the same logic as a normal ending — both badges check against `mvpId`.

### 🔴 Sprint 4: The Final Awards Gala & Certification Engine
* **The Blind Ballot & Scoring Context:**
  * **The Admin officially closes the live event. The app transitions to an interactive screen displaying the preliminary standings scoreboard (e.g., "Top 3 are John, Nancy, and Jack" or a full metrics table) alongside a line chart tracking the timeline performance.**
  * **Every user must cast a secret blind vote for the final winner (self-voting programmatically locked).**
* **The Ultimate MVP Resolution: The official event MVP title is awarded strictly to the user who receives the most votes in this final ballot.**
* **The Final Gala Upload: Once the vote is resolved, the group has the option to upload a final gala photo to Cloud Storage (gala_photo.jpg).**
* **Automated Prediction Resolution: The system's background engine automatically audits pre-trip predictions against final database standings—completely bypassing any manual Admin validation screens. Correct guesses are pushed to the users' profiles as badges.**
* **Diploma Export System: Dynamic composable layout generating localized custom awards containing profile data, performance stats, and the gala photo uploaded at the end (if available, otherwise it renders clean without it). The winner can download it locally as a PDF/PNG anytime.**

### 🔵 Sprint 5: Hall of Fame & Global Standings
* **Leaderboard Dashboard:** Single screen with a tab selector — **[ MVP ] [ Oracle ] [ Triple ]** — each tab re-sorts the same participant list by the corresponding stat field. Scoped to users from events the current user has participated in (not global strangers).
* **User Profile Screen:** Displays badge icons with counters (🏆 ×3 · 🔮 ×2 · 🎯 ×1). Tapping any badge icon opens a bottom sheet listing each individual earn with the event name and date, sourced from the `/users/{userId}/badges` subcollection.
* **Archive Viewer Mode:** Past events can be opened by any participant in a read-only viewer mode, maintaining full access to the live timeline texts, photos, and historical graphs.
* **Diploma Re-download:** Rapid re-download capability for all previously generated digital diplomas from the profile screen.

### 🟣 Sprint 6: Polish & Branding
* **App Icon:** Final launcher icon (adaptive icon for Android 8+, including foreground, background, and monochrome layers).
* **Color System:** Define the full Material 3 color scheme (primary, secondary, tertiary, error, surface tokens) for both light and dark themes.
* **Typography:** Finalize font family and type scale across all text styles (`displayLarge` → `labelSmall`).
* **Images & Illustrations:** Onboarding visuals, empty state illustrations, and any decorative assets.
* **Sounds & Haptics:** Audio feedback for key moments (emergency clause triggered, MVP awarded, diploma generated) and haptic patterns.
* **Animations & Transitions:** Screen transitions, loading skeletons, and micro-interactions (e.g. score counter animating up, emergency overlay entrance).
* **Dark Mode:** Verify all screens against the dark theme token set and fix any contrast or visibility issues.
* **String & Localization Audit:** Final pass over all string resources to ensure EN and ES are complete and consistent.

---

## 📐 Data Schemas & Architecture Blueprints

### 🗄️ Cloud Firestore Layout

#### `/users/{userId}`
```json
{
  "uid": "STRING (Primary Key)",
  "displayName": "STRING",
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
#### `/users/{userId}/badges/{badgeId}`
```json
{
  "type": "STRING (MVP | ORACLE | TRIPLE)",
  "eventId": "STRING",
  "eventTitle": "STRING (denormalized — avoids joining events collection at display time)",
  "earnedAt": "TIMESTAMP"
}
```
#### `/events/{eventId}`
```json
{
  "eventId": "STRING (Primary Key)",
  "title": "STRING",
  "locationLabel": "STRING (Nullable — user-defined place name)",
  "latitude": "NUMBER (Nullable)",
  "longitude": "NUMBER (Nullable)",
  "status": "STRING (PRE_TRIP | ON_GOING | VOTING_PHASE | FINISHED)",
  // Normal flow:    PRE_TRIP → ON_GOING → VOTING_PHASE → FINISHED
  // Emergency path: PRE_TRIP → ON_GOING → FINISHED (mvpId = emergency target, bypasses vote)
  "adminId": "STRING",
  "participants": ["STRING (User UIDs)"],
  "mvpId": "STRING (Nullable)",
  "galaPhotoUrl": "STRING (Nullable)",
  "activeEmergencyId": "STRING (Nullable)",
  "createdAt": "TIMESTAMP"
}
```
#### `/events/{eventId}/predictions/{userId}`
```json
{
  "projectedMvpId": "STRING (User UID)",
  "tripleBetParticipantId": "STRING (User UID chosen from participant pool)"
}
```
#### `/events/{eventId}/emergency_requests/{requestId}`
```json
{
  "requestId": "STRING (Primary Key)",
  "triggeredById": "STRING",
  "targetUserId": "STRING",
  "votesYes": ["STRING (User UIDs)"],
  "votesNo": ["STRING (User UIDs)"],
  "status": "STRING (PENDING | APPROVED_SHUTDOWN | REJECTED)",
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
  "pointsAwarded": "NUMBER (0.1 | 10.0)",
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

| Action                            | Value         | Engine Tracking Trigger |
|:----------------------------------|:--------------| :--- |
| **Peer Timeline Note**        | `+1,0 pts`    | Published note targeting another participant (Text, Photo, or GIF). |
| **Emergency Clause Approved**   | `+1000.0 pts` | Triggered by a player, validated by majority vote. Grants instant match victory. |

## 📊 Post-Event Profile Achievements (Hall of Fame Records)

| Achievement | Tracking Logic                                               | Profile Impact  |
| :--- |:-------------------------------------------------------------|:----------------|
| **Event MVP Winner** | Highest live score tracker or emergency clause recipent      | +1 Lifetime MVP |
| **Oracle Badge** | `projectedMvpId == mvpId` (normal or emergency path)         | +1 Oracle Win   |
| **The Triple Badge** | `tripleBetParticipantId == mvpId` (normal or emergency path) | +1 Triple Win   |