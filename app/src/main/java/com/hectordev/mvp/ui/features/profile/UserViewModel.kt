package com.hectordev.mvp.ui.features.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    fun loadUser(userId: String) {
        viewModelScope.launch {
            Log.d("UserViewModel", "Loading user with ID: $userId")
            val user = userRepository.getCurrentUser(userId)
            if (user != null) {
                Log.d("UserViewModel", "User loaded successfully: ${user.name}")
            } else {
                Log.d("UserViewModel", "User not found or Firestore is empty")
            }
            _userState.value = user
        }
    }
}