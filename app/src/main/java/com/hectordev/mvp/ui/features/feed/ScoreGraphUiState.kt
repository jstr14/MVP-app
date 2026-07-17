package com.hectordev.mvp.ui.features.feed

data class ParticipantScore(
    val userId: String,
    val name: String,
    val photoUrl: String?,
    val colorIndex: Int,
    val cumulativeScores: List<Float>,
    val totalScore: Int
)

data class ScoreGraphUiState(
    val participantScores: List<ParticipantScore> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)