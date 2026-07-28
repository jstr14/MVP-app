package com.hectordev.mvp.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""
    private val firebaseUser = firebaseAuth.currentUser

    private val _uiState = MutableStateFlow(
        BadgesUiState(
            userName = firebaseUser?.displayName ?: "",
            userPhotoUrl = firebaseUser?.photoUrl?.toString()
        )
    )
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            try {
                userRepository.observeUserStats(currentUserId).collect { stats ->
                    _uiState.update {
                        it.copy(
                            lifetimeMvps = stats.lifetimeMvps,
                            oracleWins = stats.oracleWins,
                            tripleWins = stats.tripleWins,
                            reactorWins = stats.reactorWins,
                            totalPlayerWins = stats.totalPlayerWins,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}