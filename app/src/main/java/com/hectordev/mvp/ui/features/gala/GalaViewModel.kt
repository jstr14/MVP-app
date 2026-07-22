package com.hectordev.mvp.ui.features.gala

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import com.hectordev.mvp.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<AppDestination.Gala>().eventId
    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(GalaUiState(currentUserId = currentUserId))
    val uiState: StateFlow<GalaUiState> = _uiState.asStateFlow()

    init {
        observeData()
        loadMyPrediction()
    }

    private fun loadMyPrediction() {
        viewModelScope.launch {
            runCatching {
                val prediction = eventsRepository.getPrediction(eventId, currentUserId)
                _uiState.update { it.copy(myPrediction = prediction) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            try {
                combine(
                    eventsRepository.observeEvent(eventId),
                    eventsRepository.observeTimeline(eventId),
                    eventsRepository.observeVoteCount(eventId),
                    eventsRepository.observeUserVoteTarget(eventId, currentUserId)
                ) { event, notes, voteCount, myFinalVoteId ->
                    Quad(event, notes, voteCount, myFinalVoteId)
                }.collect { (event, notes, voteCount, myFinalVoteId) ->
                    val participants = if (event == null) emptyList()
                    else try {
                        userRepository.getUsersByIds(event.participants)
                    } catch (e: Exception) {
                        emptyList<User>()
                    }

                    val topStandings = computeTopStandings(participants, notes)
                    val mvpUser = event?.mvpId?.let { id -> participants.find { it.id == id } }

                    _uiState.update {
                        it.copy(
                            eventTitle = event?.title ?: "",
                            participants = participants,
                            topStandings = topStandings,
                            hasVoted = myFinalVoteId != null,
                            myFinalVoteId = myFinalVoteId,
                            voteCount = voteCount,
                            totalParticipants = participants.size,
                            eventStatus = event?.status ?: it.eventStatus,
                            mvpId = event?.mvpId,
                            mvpUser = mvpUser,
                            isAdmin = event?.adminId == currentUserId,
                            galaPhotoUrl = event?.galaPhotoUrl,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun computeTopStandings(
        participants: List<User>,
        notes: List<TimelineNote>
    ): List<StandingEntry> {
        val scores = mutableMapOf<String, Int>()
        notes.forEach { note ->
            scores[note.targetUserId] = (scores[note.targetUserId] ?: 0) + note.pointsAwarded
        }
        return participants
            .map { user ->
                StandingEntry(
                    userId = user.id,
                    name = user.name.substringBefore(" "),
                    photoUrl = user.photoUrl,
                    totalScore = scores[user.id] ?: 0
                )
            }
            .sortedByDescending { it.totalScore }
            .take(3)
    }

    fun selectCandidate(userId: String) {
        _uiState.update { it.copy(selectedCandidateId = userId) }
    }

    fun submitVote() {
        val candidateId = _uiState.value.selectedCandidateId
        if (candidateId.isEmpty() || _uiState.value.hasVoted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingVote = true) }
            try {
                eventsRepository.submitVote(eventId, currentUserId, candidateId)
                _uiState.update { it.copy(isSubmittingVote = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingVote = false, error = e.localizedMessage) }
            }
        }
    }

    fun uploadGalaPhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true) }
            try {
                eventsRepository.uploadGalaPhoto(eventId, imageUri)
                _uiState.update { it.copy(isUploadingPhoto = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingPhoto = false, error = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth