package com.hectordev.mvp.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val firebaseUser = firebaseAuth.currentUser ?: return

        // Populate immediately from FirebaseAuth — always available, no network needed
        _userState.value = User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )

        // Enrich asynchronously from Firestore (stats, mvpCount, etc.)
        // If the document doesn't exist yet, we keep the FirebaseAuth data
        viewModelScope.launch {
            userRepository.getCurrentUser(firebaseUser.uid)?.let {
                _userState.value = it
            }
        }
    }
}