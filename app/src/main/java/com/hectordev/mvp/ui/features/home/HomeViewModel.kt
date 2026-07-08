package com.hectordev.mvp.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.BuildConfig
import com.hectordev.mvp.debug.DebugDataSeeder
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val debugDataSeeder: DebugDataSeeder
) : ViewModel() {

    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(HomeUiState(currentUserId = currentUserId))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        observeParticipantEvents()
        observePendingInvitations()
    }

    private fun loadUserData() {
        val fbUser = firebaseAuth.currentUser ?: return
        _uiState.update {
            it.copy(
                userName = fbUser.displayName ?: "",
                userPhotoUrl = fbUser.photoUrl?.toString()
            )
        }
        viewModelScope.launch {
            userRepository.getCurrentUser(fbUser.uid)?.let { user ->
                _uiState.update { it.copy(userName = user.name, userPhotoUrl = user.photoUrl) }
            }
        }
    }

    private fun observeParticipantEvents() {
        viewModelScope.launch {
            try {
                eventsRepository.observeParticipantEvents(currentUserId).collect { events ->
                    // Active: ON_GOING first, then PREDICTION, then earliest PRE_TRIP
                    val active = events.firstOrNull { it.status == EventStatus.ON_GOING }
                        ?: events.firstOrNull { it.status == EventStatus.PREDICTION }
                        ?: events.filter { it.status == EventStatus.PRE_TRIP }.minByOrNull { it.startDate }

                    // Upcoming: all non-finished events that are not the active one
                    val upcoming = events
                        .filter { it.status != EventStatus.FINISHED && it.id != active?.id }
                        .sortedBy { it.startDate }

                    val past = events.filter { it.status == EventStatus.FINISHED }

                    // Enrich active event participants
                    val activeParticipants = if (active != null) {
                        try {
                            val ids = active.participants.takeIf { it.isNotEmpty() } ?: emptyList()
                            enrichParticipants(userRepository.getUsersByIds(ids))
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else emptyList()

                    // Batch-load all past participants in one call
                    val allPastIds = past.flatMap { it.participants }.distinct()
                    val pastUsersById = try {
                        userRepository.getUsersByIds(allPastIds).associateBy { it.id }
                    } catch (e: Exception) {
                        emptyMap()
                    }
                    val enrichedPast = past.map { event ->
                        EventWithParticipants(
                            event = event,
                            participants = enrichParticipants(
                                event.participants.mapNotNull { pastUsersById[it] }
                            )
                        )
                    }

                    _uiState.update {
                        it.copy(
                            activeEvent = active?.let { EventWithParticipants(it, activeParticipants) },
                            canCreateEvent = active == null,
                            upcomingEvents = upcoming,
                            pastEvents = enrichedPast
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun deleteActiveEvent() {
        val eventId = _uiState.value.activeEvent?.event?.id ?: return
        viewModelScope.launch {
            try {
                eventsRepository.deleteEvent(eventId)
            } catch (e: Exception) {
                _uiState.update { it.copy(deleteError = e.localizedMessage ?: "Failed to delete event") }
            }
        }
    }

    fun clearDeleteError() = _uiState.update { it.copy(deleteError = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearDebugMessage() = _uiState.update { it.copy(debugMessage = null) }

    // Ensures the current user always appears in the participant list with their photo,
    // even if Firestore hasn't synced their document yet.
    private fun enrichParticipants(fetched: List<User>): List<User> {
        val state = _uiState.value
        val withCurrentUser = if (fetched.none { it.id == currentUserId }) {
            fetched + User(
                id = currentUserId,
                name = state.userName ?: "",
                email = "",
                photoUrl = state.userPhotoUrl
            )
        } else {
            fetched.map { user ->
                if (user.id == currentUserId && user.photoUrl == null)
                    user.copy(photoUrl = state.userPhotoUrl)
                else user
            }
        }
        return withCurrentUser.sortedWith { a, _ -> if (a.id == currentUserId) 1 else -1 }
    }

    private fun observePendingInvitations() {
        viewModelScope.launch {
            try {
                eventsRepository.observePendingInvitations(currentUserId).collect { events ->
                    _uiState.update { it.copy(pendingInvitations = events) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun acceptInvitation(eventId: String) {
        viewModelScope.launch {
            try {
                eventsRepository.acceptInvitation(eventId, currentUserId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun declineInvitation(eventId: String) {
        viewModelScope.launch {
            try {
                eventsRepository.declineInvitation(eventId, currentUserId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    // --- DEBUG ONLY ---

    fun seedPreTripEvent() = runDebugSeed("✅ Active event seeded!") { debugDataSeeder.seedPreTripEvent() }
    fun seedOnGoingEvent() = runDebugSeed("✅ Live event seeded!") { debugDataSeeder.seedOnGoingEvent() }
    fun seedPastEventWon() = runDebugSeed("✅ Past event (you won) seeded!") { debugDataSeeder.seedPastEventWon() }
    fun seedPastEventLost() = runDebugSeed("✅ Past event (you lost) seeded!") { debugDataSeeder.seedPastEventLost() }
    fun clearSeedData() = runDebugSeed("🗑 All seed data cleared") { debugDataSeeder.clearSeedData() }

    private fun runDebugSeed(message: String, block: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        block()
        _uiState.update { it.copy(debugMessage = message) }
    }
}