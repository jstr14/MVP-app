package com.hectordev.mvp.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // Flow that emits the current user state (true if logged in, false if not)
    val isUserLoggedIn: Flow<Boolean>

    // Gets the current logged-in user's unique ID, or null if anonymous
    val currentUserId: String?

    // Signs out the user from Firebase
    suspend fun signOut()
}