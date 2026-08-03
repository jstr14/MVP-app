package com.hectordev.mvp.data.model

data class EmergencyRequestDto(
    val id: String = "",
    val triggeredById: String = "",
    val targetUserId: String = "",
    val votesAccept: List<String> = emptyList(),
    val votesDecline: List<String> = emptyList(),
    val status: String = "PENDING",
    val expiresAt: Long = 0L,
    val timestamp: Long = 0L
)