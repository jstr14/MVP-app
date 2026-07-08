package com.hectordev.mvp.ui.features.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.Prediction
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
class EventDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<AppDestination.EventDetails>().eventId
    val currentUserId: String = firebaseAuth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(EventDetailsUiState(currentUserId = currentUserId))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    private var predictionLoaded = false

    init {
        observeEvent()
    }

    private fun observeEvent() {
        viewModelScope.launch {
            try {
                eventsRepository.observeEvent(eventId).collect { event ->
                    val participants = try {
                        if (event == null) emptyList()
                        else userRepository.getUsersByIds(event.participants)
                    } catch (e: Exception) {
                        emptyList<User>()
                    }
                    val pendingParticipants = try {
                        if (event == null) emptyList()
                        else userRepository.getUsersByIds(event.pendingParticipants)
                    } catch (e: Exception) {
                        emptyList<User>()
                    }
                    _uiState.update {
                        it.copy(
                            event = event,
                            participants = participants,
                            pendingParticipants = pendingParticipants,
                            isLoading = false
                        )
                    }
                    if (event != null &&
                        (event.status == EventStatus.PREDICTION || event.status == EventStatus.ON_GOING) &&
                        !predictionLoaded
                    ) {
                        predictionLoaded = true
                        loadMyPrediction()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun loadMyPrediction() {
        viewModelScope.launch {
            runCatching {
                val prediction = eventsRepository.getPrediction(eventId, currentUserId)
                _uiState.update { it.copy(myPrediction = prediction) }
            }
        }
    }

    fun submitPrediction(projectedMvpId: String, tripleParticipantId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPrediction = true) }
            try {
                val prediction = Prediction(projectedMvpId, tripleParticipantId)
                eventsRepository.submitPrediction(eventId, currentUserId, prediction)
                _uiState.update { it.copy(isSubmittingPrediction = false, myPrediction = prediction, predictionSaveCount = it.predictionSaveCount + 1) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingPrediction = false, error = e.localizedMessage) }
            }
        }
    }

    fun openPredictions() {
        viewModelScope.launch {
            try {
                eventsRepository.openPredictions(eventId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun startEvent() {
        viewModelScope.launch {
            try {
                eventsRepository.updateStatus(eventId, EventStatus.ON_GOING)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun inviteByEmail(email: String) {
        val trimmedEmail = email.trim().lowercase()
        viewModelScope.launch {
            _uiState.update { it.copy(isInviting = true, inviteError = null, inviteSuccess = false) }
            try {
                val event = _uiState.value.event ?: run {
                    _uiState.update { it.copy(isInviting = false) }
                    return@launch
                }
                val user = userRepository.getUserByEmail(trimmedEmail)
                if (user != null) {
                    if (event.participants.contains(user.id) || event.pendingParticipants.contains(user.id)) {
                        _uiState.update { it.copy(isInviting = false, inviteError = InviteError.ALREADY_MEMBER) }
                        return@launch
                    }
                    eventsRepository.inviteParticipant(eventId, user.id)
                } else {
                    if (event.pendingEmails.contains(trimmedEmail)) {
                        _uiState.update { it.copy(isInviting = false, inviteError = InviteError.ALREADY_MEMBER) }
                        return@launch
                    }
                    eventsRepository.invitePendingEmail(eventId, trimmedEmail)
                }
                _uiState.update { it.copy(isInviting = false, inviteSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isInviting = false, inviteError = InviteError.UNKNOWN) }
            }
        }
    }

    fun removeParticipant(userId: String) {
        viewModelScope.launch {
            try {
                eventsRepository.removeParticipant(eventId, userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancelEmailInvite(email: String) {
        viewModelScope.launch {
            try {
                eventsRepository.cancelEmailInvite(eventId, email)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun cancelInvite(userId: String) {
        viewModelScope.launch {
            try {
                eventsRepository.cancelInvite(eventId, userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun clearInviteState() {
        _uiState.update { it.copy(inviteError = null, inviteSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}