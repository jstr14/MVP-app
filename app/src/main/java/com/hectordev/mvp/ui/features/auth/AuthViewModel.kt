package com.hectordev.mvp.ui.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.hectordev.mvp.BuildConfig
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    // Firebase Auth instance reference
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Reactive State tracking if the user is currently authenticated in Firebase
    private val _isUserLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Manage UI status reactively using StateFlow
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Triggers the modern Google Sign-In sheet using the Credential Manager API
     * and authenticates the token securely with Firebase.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            // Initialize UI state for loading indicator
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val serverClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID

                if (serverClientId.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Server Client ID is missing in local.properties")
                    }
                    return@launch
                }

                // Configure Google Identity Options
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // CredentialManager must be created with the Activity context so it can
                // properly anchor and render the account picker bottom sheet.
                val credentialManager = CredentialManager.create(activityContext)

                // 1. Await system's native account picker sheet
                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                val credential = result.credential

                // 2. Validate and parse the credential type safely
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // Generate modern Firebase credential wrapper
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                    // 3. Authenticate with Firebase using coroutine suspension (.await())
                    // If the network call fails, it will automatically throw a Firebase exception handled by the catch block below
                    val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val email = firebaseUser.email ?: ""
                        userRepository.saveUser(
                            User(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "",
                                email = email,
                                photoUrl = firebaseUser.photoUrl?.toString()
                            )
                        )
                        if (email.isNotEmpty()) {
                            runCatching { eventsRepository.bindPendingEmailInvite(email, firebaseUser.uid) }
                        }
                        runCatching {
                            val token = FirebaseMessaging.getInstance().token.await()
                            userRepository.saveFcmToken(firebaseUser.uid, token)
                        }
                        // Success path: update session states and trigger UI navigation
                        _isUserLoggedIn.value = true
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Authentication succeeded but returned an empty profile.")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Security error: Unexpected credential type format received.")
                    }
                }

            } catch (e: GetCredentialException) {
                // Catches user cancelations (swiping down the sheet), configuration errors, or API unavailabilities
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Google Sign-In canceled or failed.")
                }
            } catch (e: Exception) {
                // Catches network connection drops, revoked tokens, or Firebase side exceptions gracefully
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "An unexpected authentication error occurred.")
                }
            }
        }
    }

    /**
     * Helper function to execute clean sign-out tasks across Firebase
     */
    fun signOut() {
        firebaseAuth.signOut()
        _isUserLoggedIn.value = false
    }

    /**
     * Resets the authentication UI states to prevent continuous routing loops.
     */
    fun resetState() {
        _uiState.update { AuthUiState() }
    }
}