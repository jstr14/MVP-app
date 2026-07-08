package com.hectordev.mvp.ui.features.event

import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.User

data class EventDetailsUiState(
    val event: Event? = null,
    val participants: List<User> = emptyList(),
    val pendingParticipants: List<User> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isInviting: Boolean = false,
    val inviteError: InviteError? = null,
    val inviteSuccess: Boolean = false,
    val myPrediction: Prediction? = null,
    val isSubmittingPrediction: Boolean = false,
    val predictionSaveCount: Int = 0
)

enum class InviteError { USER_NOT_FOUND, ALREADY_MEMBER, UNKNOWN }