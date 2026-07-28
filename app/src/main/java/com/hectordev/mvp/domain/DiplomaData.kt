package com.hectordev.mvp.domain

data class DiplomaData(
    val eventId: String,
    val eventTitle: String,
    val startDate: Long,
    val endDate: Long,
    val winnerName: String,
    val winnerId: String,
    val standings: List<Pair<String, Int>>, // name, score
    val galaPhotoUrl: String?,
    val totalParticipants: Int
)