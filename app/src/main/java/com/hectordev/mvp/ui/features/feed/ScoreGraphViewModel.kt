package com.hectordev.mvp.ui.features.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.User
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
class ScoreGraphViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val eventId: String = savedStateHandle.toRoute<AppDestination.ScoreGraph>().eventId

    private val _uiState = MutableStateFlow(ScoreGraphUiState())
    val uiState: StateFlow<ScoreGraphUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            try {
                combine(
                    eventsRepository.observeEvent(eventId),
                    eventsRepository.observeTimeline(eventId)
                ) { event, notes -> Pair(event, notes) }
                    .collect { (event, notes) ->
                        val participants = if (event == null) emptyList()
                        else try {
                            userRepository.getUsersByIds(event.participants)
                        } catch (e: Exception) {
                            emptyList<User>()
                        }

                        val sortedNotes = notes.sortedBy { it.timestamp }
                        val scores = computeCumulativeScores(event?.participants ?: emptyList(), sortedNotes)

                        val participantScores = participants.mapIndexed { index, user ->
                            val scoreData = scores[user.id] ?: emptyList()
                            ParticipantScore(
                                userId = user.id,
                                name = user.name.substringBefore(" "),
                                photoUrl = user.photoUrl,
                                colorIndex = index,
                                cumulativeScores = scoreData,
                                totalScore = scoreData.lastOrNull()?.toInt() ?: 0
                            )
                        }.sortedByDescending { it.totalScore }

                        _uiState.update {
                            it.copy(participantScores = participantScores, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun computeCumulativeScores(
        participantIds: List<String>,
        sortedNotes: List<TimelineNote>
    ): Map<String, List<Float>> {
        val current = participantIds.associateWith { 0f }.toMutableMap()
        val result = participantIds.associateWith { mutableListOf<Float>() }.toMutableMap()

        if (sortedNotes.isNotEmpty()) {
            // Prepend a zero starting point so Vico always has ≥ 2 data points to draw a line
            participantIds.forEach { uid -> result[uid]?.add(0f) }
        }

        sortedNotes.forEach { note ->
            current[note.targetUserId] = (current[note.targetUserId] ?: 0f) + note.pointsAwarded
            participantIds.forEach { uid ->
                result[uid]?.add(current[uid] ?: 0f)
            }
        }
        return result
    }
}