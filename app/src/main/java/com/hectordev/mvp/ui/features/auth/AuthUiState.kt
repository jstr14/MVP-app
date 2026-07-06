package com.hectordev.mvp.ui.features.auth

/**
 * UI State representing the authentication flow status on the LoginScreen.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)