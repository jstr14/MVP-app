package com.hectordev.mvp.ui.features.gala

import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.User

data class StandingEntry(
    val userId: String,
    val name: String,
    val photoUrl: String?,
    val totalScore: Int
)

data class GalaUiState(
    val eventTitle: String = "",
    val participants: List<User> = emptyList(),
    val currentUserId: String = "",
    val topStandings: List<StandingEntry> = emptyList(),
    val hasVoted: Boolean = false,
    val myFinalVoteId: String? = null,
    val myPrediction: Prediction? = null,
    val selectedCandidateId: String = "",
    val voteCount: Int = 0,
    val totalParticipants: Int = 0,
    val eventStatus: EventStatus = EventStatus.VOTING_PHASE,
    val mvpId: String? = null,
    val mvpUser: User? = null,
    val isAdmin: Boolean = false,
    val galaPhotoUrl: String? = null,
    val eventStartDate: Long = 0L,
    val eventEndDate: Long = 0L,
    val isSubmittingVote: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isGeneratingCertificate: Boolean = false,
    val certificateSavedMessage: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)