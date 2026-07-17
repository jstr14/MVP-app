package com.hectordev.mvp.ui.features.feed

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import com.hectordev.mvp.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveFeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<AppDestination.LiveFeed>().eventId
    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(LiveFeedUiState(currentUserId = currentUserId))
    val uiState: StateFlow<LiveFeedUiState> = _uiState.asStateFlow()

    init {
        observeEvent()
        observeTimeline()
    }

    private fun observeEvent() {
        viewModelScope.launch {
            try {
                eventsRepository.observeEvent(eventId).collect { event ->
                    val participants = if (event == null) emptyList()
                    else try {
                        userRepository.getUsersByIds(event.participants)
                    } catch (e: Exception) {
                        emptyList<User>()
                    }
                    _uiState.update {
                        it.copy(
                            eventTitle = event?.title ?: "",
                            participants = participants,
                            isAdmin = event?.adminId == currentUserId,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun observeTimeline() {
        viewModelScope.launch {
            try {
                eventsRepository.observeTimeline(eventId).collect { notes ->
                    _uiState.update { it.copy(notes = notes) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun postNote(
        tier: TimelineTier,
        targetUserId: String,
        text: String,
        imageUri: Uri? = null,
        gifUrl: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true) }
            try {
                val contentUrl = when {
                    imageUri != null -> eventsRepository.uploadNotePhoto(eventId, imageUri)
                    else -> gifUrl
                }
                val type = when {
                    gifUrl != null -> NoteType.GIF
                    contentUrl != null -> NoteType.PHOTO
                    else -> NoteType.TEXT
                }
                val note = TimelineNote(
                    authorId = currentUserId,
                    targetUserId = targetUserId,
                    type = type,
                    textContent = text.trim().ifBlank { null },
                    contentUrl = contentUrl,
                    tier = tier,
                    pointsAwarded = tier.points,
                    timestamp = System.currentTimeMillis()
                )
                eventsRepository.postNote(eventId, note)
                _uiState.update { it.copy(isPosting = false, postSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPosting = false, error = e.localizedMessage) }
            }
        }
    }

    fun toggleReaction(note: TimelineNote, emoji: String) {
        viewModelScope.launch {
            try {
                val alreadyReacted = note.reactions[emoji]?.contains(currentUserId) == true
                if (alreadyReacted) {
                    eventsRepository.removeReaction(eventId, note.id, emoji, currentUserId)
                } else {
                    eventsRepository.addReaction(eventId, note.id, emoji, currentUserId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteNote(note: TimelineNote) {
        viewModelScope.launch {
            try {
                eventsRepository.deleteNote(eventId, note.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearPostSuccess() {
        _uiState.update { it.copy(postSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}