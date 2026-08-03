package com.hectordev.mvp.ui.features.feed

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.EmergencyRequest
import com.hectordev.mvp.domain.EmergencyStatus
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.EmergencyRepository
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import com.hectordev.mvp.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val emergencyRepository: EmergencyRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<AppDestination.LiveFeed>().eventId
    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(LiveFeedUiState(currentUserId = currentUserId))
    val uiState: StateFlow<LiveFeedUiState> = _uiState.asStateFlow()

    private var emergencyObserveJob: Job? = null
    private var countdownJob: Job? = null

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

                    val newActiveEmergencyId = event?.activeEmergencyId
                    val currentEmergencyId = _uiState.value.activeEmergencyId

                    if (newActiveEmergencyId != currentEmergencyId) {
                        if (newActiveEmergencyId != null) {
                            startObservingEmergency(newActiveEmergencyId)
                        } else {
                            stopEmergency()
                        }
                    }

                    _uiState.update {
                        it.copy(
                            eventTitle = event?.title ?: "",
                            participants = participants,
                            isAdmin = event?.adminId == currentUserId,
                            navigateToGala = event?.status == EventStatus.VOTING_PHASE ||
                                    event?.status == EventStatus.FINISHED,
                            hasUsedEmergency = event?.usedEmergencyClause?.contains(currentUserId) == true,
                            activeEmergencyId = newActiveEmergencyId,
                            totalParticipants = event?.participants?.size ?: 0,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun startObservingEmergency(requestId: String) {
        emergencyObserveJob?.cancel()
        emergencyObserveJob = viewModelScope.launch {
            var countdownStarted = false
            emergencyRepository.observeEmergencyRequest(eventId, requestId).collect { request ->
                _uiState.update { it.copy(emergencyRequest = request) }
                if (request != null && request.status == EmergencyStatus.PENDING && !countdownStarted) {
                    countdownStarted = true
                    startCountdown(request.expiresAt)
                }
            }
        }
    }

    private fun startCountdown(expiresAt: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = maxOf(0L, (expiresAt - System.currentTimeMillis()) / 1000)
                _uiState.update { it.copy(emergencyCountdownSeconds = remaining.toInt()) }
                if (remaining <= 0L) {
                    resolveEmergencyTimeout()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopEmergency() {
        emergencyObserveJob?.cancel()
        countdownJob?.cancel()
        _uiState.update { it.copy(emergencyRequest = null, emergencyCountdownSeconds = 0) }
    }

    private fun resolveEmergencyTimeout() {
        val requestId = _uiState.value.activeEmergencyId ?: return
        val request = _uiState.value.emergencyRequest ?: return
        if (request.status != EmergencyStatus.PENDING) return
        viewModelScope.launch {
            runCatching {
                emergencyRepository.resolveTimeout(eventId, requestId, request.triggeredById)
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

    fun endEvent() {
        viewModelScope.launch {
            try {
                eventsRepository.updateStatus(eventId, EventStatus.VOTING_PHASE)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearNavigateToGala() {
        _uiState.update { it.copy(navigateToGala = false) }
    }

    fun clearPostSuccess() {
        _uiState.update { it.copy(postSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun triggerEmergency(targetUserId: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val request = EmergencyRequest(
                    triggeredById = currentUserId,
                    targetUserId = targetUserId,
                    votesAccept = listOf(currentUserId),
                    expiresAt = now + 3 * 60 * 1000L,
                    timestamp = now
                )
                emergencyRepository.triggerEmergency(eventId, request)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun castEmergencyVote(accept: Boolean) {
        val requestId = _uiState.value.activeEmergencyId ?: return
        viewModelScope.launch {
            runCatching {
                emergencyRepository.castVote(eventId, requestId, currentUserId, accept)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}