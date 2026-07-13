package com.hectordev.mvp.domain

data class Event(
    val id: String = "",
    val title: String = "",
    val locationLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val status: EventStatus = EventStatus.PRE_TRIP,
    val adminId: String = "",
    val participants: List<String> = emptyList(),
    val pendingParticipants: List<String> = emptyList(),
    val pendingEmails: List<String> = emptyList(),
    val mvpId: String? = null,
    val galaPhotoUrl: String? = null,
    val activeEmergencyId: String? = null,
    val usedEmergencyClause: List<String> = emptyList(),
    val createdAt: Long = 0L
)