package com.hectordev.mvp.ui.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.hectordev.mvp.BuildConfig
import com.hectordev.mvp.domain.User
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
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Reactive State tracking if the user is currently authenticated in Firebase
    private val _isUserLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Manage UI status reactively using StateFlow
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Triggers the Google Sign-In sheet using the Credential Manager API,
     * authenticates with Firebase, and upserts the user profile to Firestore.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val serverClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID

                if (serverClientId.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Server Client ID is missing in local.properties")
                    }
                    return@launch
                }

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

                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        // Navigate immediately — don't block on Firestore write
                        _isUserLoggedIn.value = true
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }

                        // Fire-and-forget: upsert profile in background, merge preserves stats
                        launch {
                            userRepository.saveUser(
                                User(
                                    id = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "",
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString()
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Firebase authenticated but returned an empty profile.")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Security error: Unexpected credential type received.")
                    }
                }

            } catch (e: GetCredentialException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Google Sign-In canceled or failed.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "An unexpected authentication error occurred.")
                }
            }
        }
    }

    /**
     * Signs out from Firebase and clears the session state.
     */
    fun signOut() {
        firebaseAuth.signOut()
        _isUserLoggedIn.value = false
    }

    /**
     * Resets the authentication UI state to prevent continuous routing loops.
     */
    fun resetState() {
        _uiState.update { AuthUiState() }
    }
}
