package com.hectordev.mvp.ui.features.home

import com.hectordev.mvp.domain.Event

data class HomeUiState(
    val userName: String? = null,
    val userPhotoUrl: String? = null,
    val currentUserId: String = "",
    val canCreateEvent: Boolean = true,
    val activeEvent: EventWithParticipants? = null,
    val pendingInvitations: List<Event> = emptyList(),
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<EventWithParticipants> = emptyList(),
    val deleteError: String? = null,
    val error: String? = null,
    val debugMessage: String? = null
)