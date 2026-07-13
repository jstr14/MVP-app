package com.hectordev.mvp.data.model

data class TimelineNoteDto(
    val id: String = "",
    val authorId: String = "",
    val targetUserId: String = "",
    val type: String = "TEXT",
    val textContent: String? = null,
    val contentUrl: String? = null,
    val tierLabel: String = "Fact",
    val pointsAwarded: Int = 1,
    val timestamp: Long = 0L
)