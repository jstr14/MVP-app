package com.hectordev.mvp.ui.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.BuildConfig
import com.hectordev.mvp.debug.DebugDataSeeder
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
        observeActiveEvent()
        observePastEvents()
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

    private fun observeActiveEvent() {
        viewModelScope.launch {
            try {
            eventsRepository.observeActiveEvent(currentUserId).collect { event ->
                val participants = try {
                    val ids = event?.participants?.takeIf { it.isNotEmpty() } ?: emptyList()
                    enrichParticipants(userRepository.getUsersByIds(ids))
                } catch (e: Exception) {
                    emptyList()
                }
                val enriched = event?.let { EventWithParticipants(it, participants) }
                _uiState.update { it.copy(activeEvent = enriched, canCreateEvent = enriched == null) }
            }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "observeActiveEvent crashed: ${e.message}")
            }
        }
    }

    private fun observePastEvents() {
        viewModelScope.launch {
            try {
            eventsRepository.observePastEvents(currentUserId).collect { events ->
                if (events.isEmpty()) {
                    _uiState.update { it.copy(pastEvents = emptyList()) }
                    return@collect
                }
                val allIds = events.flatMap { it.participants }.distinct()
                val usersById = try {
                    userRepository.getUsersByIds(allIds).associateBy { it.id }
                } catch (e: Exception) {
                    emptyMap()
                }
                val enriched = events.map { event ->
                    EventWithParticipants(
                        event = event,
                        participants = enrichParticipants(
                            event.participants.mapNotNull { usersById[it] }
                        )
                    )
                }
                _uiState.update { it.copy(pastEvents = enriched) }
            }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "observePastEvents crashed: ${e.message}")
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

    // TODO: implement when invite system is built (Sprint 2 - Event Details)
    fun acceptInvitation(eventId: String) { }
    fun declineInvitation(eventId: String) { }

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