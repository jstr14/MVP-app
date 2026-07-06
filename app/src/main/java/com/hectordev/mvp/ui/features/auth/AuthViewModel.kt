package com.hectordev.mvp.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hectordev.mvp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Converts our cold Flow from Firebase into a hot StateFlow for Compose UI to listen to safely
    val isUserLoggedIn: StateFlow<Boolean> = authRepository.isUserLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keeps flow active for 5s after UI disconnects (rotation safety)
            initialValue = false
        )

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}