package com.hectordev.mvp.ui.features.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.components.TierBadge
import com.hectordev.mvp.ui.core.theme.MVPTheme

// --- PREVIEW DATA ---

internal val previewUsers = listOf(
    User(id = "1", name = "Jack Shepard", email = "jack@test.com"),
    User(id = "2", name = "Kate Austen", email = "kate@test.com"),
    User(id = "3", name = "Sawyer Ford", email = "sawyer@test.com"),
)

internal val previewNotes = listOf(
    TimelineNote(
        id = "1", authorId = "1", targetUserId = "2",
        type = NoteType.TEXT, tier = TimelineTier.HOT_TAKE, pointsAwarded = 2,
        textContent = "She absolutely carried the whole dinner.",
        timestamp = 1_750_000_000_000L,
        reactions = mapOf("😂" to listOf("2", "3"), "🔥" to listOf("3"))
    ),
    TimelineNote(
        id = "2", authorId = "3", targetUserId = "1",
        type = NoteType.TEXT, tier = TimelineTier.LORE, pointsAwarded = 10,
        textContent = "This man ordered dessert before anyone finished their starter.",
        timestamp = 1_750_001_000_000L,
        reactions = mapOf("😭" to listOf("1", "2", "3"), "🚨" to listOf("2"), "🤮" to listOf("1"))
    ),
    TimelineNote(
        id = "3", authorId = "2", targetUserId = "3",
        type = NoteType.TEXT, tier = TimelineTier.FACT, pointsAwarded = 1,
        textContent = "Lost at every single card game.",
        timestamp = 1_750_002_000_000L,
        reactions = mapOf("🤡" to listOf("1", "3"))
    ),
    TimelineNote(
        id = "4", authorId = "1", targetUserId = "3",
        type = NoteType.TEXT, tier = TimelineTier.WITNESSED, pointsAwarded = 5,
        textContent = "Broke the vibe machine at the hotel bar. We all saw it.",
        timestamp = 1_750_003_000_000L,
        reactions = mapOf("👀" to listOf("2", "3"), "🤮" to listOf("1"), "🫡" to listOf("2"))
    )
)

// --- SCREEN PREVIEWS ---

@Preview(showBackground = true, name = "Feed — With notes (admin)")
@Composable
private fun LiveFeedWithNotesAdminPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = previewNotes,
                    participants = previewUsers,
                    currentUserId = "1",
                    isAdmin = true,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onGoToGala = {},
                onEndEvent = {},
                onClearNavigateToGala = {},
                onPostNote = { _, _, _, _, _ -> },
                onPostSuccessConsumed = {},
                onDeleteNote = {},
                onToggleReaction = { _, _ -> },
                onErrorShown = {},
                onTriggerEmergency = {},
                onCastEmergencyVote = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Feed — With notes (participant)")
@Composable
private fun LiveFeedWithNotesParticipantPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = previewNotes,
                    participants = previewUsers,
                    currentUserId = "2",
                    isAdmin = false,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onGoToGala = {},
                onEndEvent = {},
                onClearNavigateToGala = {},
                onPostNote = { _, _, _, _, _ -> },
                onPostSuccessConsumed = {},
                onDeleteNote = {},
                onToggleReaction = { _, _ -> },
                onErrorShown = {},
                onTriggerEmergency = {},
                onCastEmergencyVote = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Feed — Empty state")
@Composable
private fun LiveFeedEmptyPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = emptyList(),
                    participants = previewUsers,
                    currentUserId = "1",
                    isAdmin = false,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onGoToGala = {},
                onEndEvent = {},
                onClearNavigateToGala = {},
                onPostNote = { _, _, _, _, _ -> },
                onPostSuccessConsumed = {},
                onDeleteNote = {},
                onToggleReaction = { _, _ -> },
                onErrorShown = {},
                onTriggerEmergency = {},
                onCastEmergencyVote = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "NoteCard — Lore (admin)")
@Composable
private fun NoteCardLorePreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = previewNotes[1],
                participants = previewUsers,
                isAdmin = true,
                currentUserId = "1",
                onDelete = {},
                onToggleReaction = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "NoteCard — Fact (author, can delete)")
@Composable
private fun NoteCardFactPreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = previewNotes[2],
                participants = previewUsers,
                isAdmin = false,
                currentUserId = "2",
                onDelete = {},
                onToggleReaction = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tier badges — All tiers")
@Composable
private fun TierBadgesPreview() {
    MVPTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineTier.entries.forEach { tier -> TierBadge(tier = tier) }
            }
        }
    }
}