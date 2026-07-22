package com.hectordev.mvp.ui.features.gala

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.theme.MVPTheme

// --- PREVIEW DATA ---

private val previewUsers = listOf(
    User(id = "1", name = "Jack Shepard", email = "jack@test.com"),
    User(id = "2", name = "Kate Austen", email = "kate@test.com"),
    User(id = "3", name = "Sawyer Ford", email = "sawyer@test.com"),
    User(id = "4", name = "John Locke", email = "locke@test.com"),
)

private val previewStandings = listOf(
    StandingEntry(userId = "1", name = "Jack", photoUrl = null, totalScore = 42),
    StandingEntry(userId = "2", name = "Kate", photoUrl = null, totalScore = 31),
    StandingEntry(userId = "3", name = "Sawyer", photoUrl = null, totalScore = 20),
)

private val previewWinner = User(id = "2", name = "Kate Austen", email = "kate@test.com")

private val previewPrediction = Prediction(
    projectedMvpId = "1", // Jack as MVP
    tripleParticipantId = "3" // Sawyer as Triple
)

private fun votingState(
    hasVoted: Boolean = false,
    voteCount: Int = 0,
    currentUserId: String = "1"
) = GalaUiState(
    eventTitle = "Summer Trip 2025",
    participants = previewUsers,
    currentUserId = currentUserId,
    topStandings = previewStandings,
    hasVoted = hasVoted,
    voteCount = voteCount,
    totalParticipants = 4,
    eventStatus = EventStatus.VOTING_PHASE,
    isLoading = false
)

private fun finishedState(
    galaPhotoUrl: String? = null,
    currentUserId: String = "1"
) = GalaUiState(
    eventTitle = "Summer Trip 2025",
    participants = previewUsers,
    currentUserId = currentUserId,
    topStandings = previewStandings,
    hasVoted = true,
    voteCount = 4,
    totalParticipants = 4,
    eventStatus = EventStatus.FINISHED,
    mvpId = "2",
    mvpUser = previewWinner,
    isAdmin = currentUserId == "1",
    galaPhotoUrl = galaPhotoUrl,
    isLoading = false
)

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Voting — no votes yet")
@Composable
private fun GalaVotingPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = votingState(),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Voting — user already voted")
@Composable
private fun GalaVotedPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = votingState(hasVoted = true, voteCount = 2),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Voting — all votes cast, calculating")
@Composable
private fun GalaCalculatingPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = votingState(hasVoted = true, voteCount = 4),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Finished — winner, no photo (admin)")
@Composable
private fun GalaFinishedNoPhotoAdminPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = finishedState(currentUserId = "1"),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Finished — winner revealed (winner view)")
@Composable
private fun GalaFinishedWinnerPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = finishedState(currentUserId = "2"), // Kate is both winner and viewer
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Finished — with gala photo (participant)")
@Composable
private fun GalaFinishedWithPhotoPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = finishedState(
                    galaPhotoUrl = "https://picsum.photos/800/600",
                    currentUserId = "3"
                ),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Voting — with predictions and final vote filled")
@Composable
private fun GalaVotingWithVotesPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = votingState(hasVoted = true, voteCount = 3).copy(
                    myPrediction = previewPrediction,
                    myFinalVoteId = "2" // voted for Kate
                ),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Finished — winner with all votes revealed")
@Composable
private fun GalaFinishedWithVotesPreview() {
    MVPTheme {
        Surface {
            GalaContent(
                uiState = finishedState(currentUserId = "2").copy( // Kate is winner + viewer
                    myPrediction = previewPrediction,
                    myFinalVoteId = "2" // voted for herself (just for preview)
                ),
                onBack = {},
                onGoToGraph = {},
                onSelectCandidate = {},
                onSubmitVote = {},
                onUploadGalaPhoto = {},
                onErrorShown = {}
            )
        }
    }
}