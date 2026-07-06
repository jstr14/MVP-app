package com.hectordev.mvp.ui.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.hectordev.mvp.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Firebase Auth instance reference
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Reactive State tracking if the user is currently authenticated in Firebase
    private val _isUserLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Manage UI status reactively using StateFlow
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val credentialManager = CredentialManager.create(context)

    /**
     * Triggers the modern Google Sign-In sheet using the Credential Manager API.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val serverClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID

                if (serverClientId.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Server Client ID is missing") }
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

                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                // TODO: Exchange this Google ID Token for a Firebase Credential (Next Step)
                // For now, we simulate a successful login state updating both variables
                _isUserLoggedIn.value = true
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            } catch (e: GetCredentialException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Authentication failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "An unexpected error occurred")
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