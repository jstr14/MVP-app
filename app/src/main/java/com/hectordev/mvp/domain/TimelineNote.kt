package com.hectordev.mvp.domain

data class TimelineNote(
    val id: String = "",
    val authorId: String = "",
    val targetUserId: String = "",
    val type: NoteType = NoteType.TEXT,
    val textContent: String? = null,
    val contentUrl: String? = null,
    val tier: TimelineTier = TimelineTier.FACT,
    val pointsAwarded: Int = 1,
    val timestamp: Long = 0L
)