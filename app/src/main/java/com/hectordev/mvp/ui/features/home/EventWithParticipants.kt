package com.hectordev.mvp.ui.features.home

import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.User

data class EventWithParticipants(
    val event: Event,
    val participants: List<User>
)