# MVP - The Ultimate Hangout Companion 🏆

**MVP** is a gamified social application designed to track, rank, and immortalize the best moments of trips and gatherings with friends. Inspired by "Mario Party" mechanics, it turns real-life hangouts into a competitive experience with live scoring, secret voting, and digital trophies.

## ✨ Features

*   **Event Lifecycle:** Manage phases from Pre-Event bets to the Final Ceremony.
*   **The "Triple" Bet:** High-stakes "crazy" bets made before the trip.
*   **Mario Party Scoring:** Real-time point system with a live performance graph.
*   **Media Anecdotes:** Log the "lore" with text, photos, and GIFs (Giphy integration).
*   **Emergency Clause:** A "Nuclear Option" vote for automatic MVP in case of medical or legal chaos.
*   **Blind Voting:** Secret ballot system to decide the event's MVP.
*   **Automated Diplomas:** Generate and download PDF certificates for the winner.

## 🛠 Tech Stack

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architecture:** Clean Architecture (KMP-Ready)
*   **Backend:** [Firebase](https://firebase.google.com/) (Auth, Firestore, Storage)
*   **DI:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
*   **Async:** Coroutines & Flow
*   **Image Loading:** Coil

## 📐 Architecture Overview

The project follows **Clean Architecture** principles to ensure scalability and a smooth transition to **Kotlin Multiplatform (KMP)** in the future.

*   **`:domain`**: Pure Kotlin module containing Entities, Repository interfaces, and Use Cases.
*   **`:data`**: Implementation of repositories, Firebase DataSources, and Mappers.
*   **`:app`**: Jetpack Compose UI, ViewModels (MVVM), and Android-specific logic.
