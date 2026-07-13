package com.hectordev.mvp.ui.features.feed

import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.User

data class LiveFeedUiState(
    val eventTitle: String = "",
    val notes: List<TimelineNote> = emptyList(),
    val participants: List<User> = emptyList(),
    val currentUserId: String = "",
    val isAdmin: Boolean = false,
    val isPosting: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)