# MVP - The Ultimate Hangout Companion 🏆

**MVP** is a gamified social application designed to track, rank, and immortalise the best moments of trips and gatherings with friends. Inspired by "Mario Party" mechanics, it turns real-life hangouts into a competitive experience with live scoring, secret voting, and digital trophies.

## ✨ Features

- **Event Lifecycle** — Manage phases from pre-event predictions to the final gala ceremony.
- **The "Triple" Bet** — High-stakes wildcard predictions made before the event starts.
- **Tier-Based Scoring** — Real-time point system (Fact / Hot Take / Witnessed / Lore) with a live performance graph.
- **Multimedia Notes** — Log the lore with text, photos, and GIFs (Giphy integration).
- **Emoji Reactions** — React to notes with a predefined set of emojis.
- **Emergency Clause** — A crowd-panic nuclear option vote for an instant MVP winner.
- **Blind Voting** — Secret ballot system to decide the event's MVP.
- **Automated Diplomas** — Generate and download PDF certificates for the winner.

## 🛠 Tech Stack

- **Language:** [Kotlin 2.x](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture:** Clean Architecture — Domain / Data / UI layers in a single-module project
- **Backend:** [Firebase](https://firebase.google.com/) (Auth, Firestore, Storage, Cloud Functions)
- **DI:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Async:** Coroutines & Flow
- **Navigation:** Navigation Compose with type-safe routes (`kotlinx.serialization`)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **GIF:** [Giphy SDK](https://developers.giphy.com/)
- **Charts:** [Vico](https://github.com/patrykandpatrick/vico)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or later
- **JDK 11+**
- **Node.js 20+** and **npm** (for Cloud Functions)
- **Firebase CLI** — install with `npm install -g firebase-tools`
- A Firebase project with Auth, Firestore, Storage, and Cloud Functions enabled

---

### 1. Clone the repository

```bash
git clone <repo-url>
cd MVP-app
```

---

### 2. Add `google-services.json`

Download `google-services.json` from your Firebase project:
> Firebase Console → Project Settings → General → Your Apps → Download

Place it at:
```
app/google-services.json
```

> ⚠️ This file is gitignored and must never be committed.

---

### 3. Configure `local.properties`

The file `local.properties` is gitignored. Create it at the project root with the following values:

```properties
# Android SDK path — usually set automatically by Android Studio
sdk.dir=/Users/<your-username>/Library/Android/sdk

# Firebase Web Client ID
# Firebase Console → Project Settings → General → Web Client → OAuth 2.0 Client ID
FIREBASE_WEB_CLIENT_ID="your-web-client-id-here"

# Giphy API Key
# https://developers.giphy.com → Create an App → SDK key
GIPHY_API_KEY="your-giphy-api-key-here"
```

> ⚠️ Values for `FIREBASE_WEB_CLIENT_ID` and `GIPHY_API_KEY` must be wrapped in double quotes.

---

### 4. Build and run the Android app

Open the project in Android Studio and run it, or via the command line:

```bash
./gradlew assembleDebug
```

---

### 5. Set up Cloud Functions

The `functions/` directory contains the Firebase Cloud Functions (TypeScript).

```bash
cd functions

# Install dependencies (uses the public npm registry via .npmrc)
npm install

# Compile TypeScript → JavaScript
npm run build
```

---

### 6. Deploy Cloud Functions

Authenticate with Firebase (first time only):

```bash
firebase login
```

Deploy:

```bash
# From the project root
firebase deploy --only functions
```

To deploy and see logs:

```bash
firebase deploy --only functions && firebase functions:log
```

---

## 📁 Project Structure

```
MVP-app/
├── app/
│   └── src/main/java/com/hectordev/mvp/
│       ├── domain/          # Entities, Repository interfaces
│       ├── data/            # Repository implementations, DTOs, Mappers, Firebase
│       └── ui/              # Compose screens, ViewModels, Navigation
├── functions/
│   └── src/index.ts         # Firebase Cloud Functions
├── firebase.json            # Firebase CLI config
└── .firebaserc              # Firebase project alias
```

---

## 🗺 Roadmap

See [ROADMAP.md](ROADMAP.md) for the full feature roadmap and sprint breakdown.