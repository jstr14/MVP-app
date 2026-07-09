package com.hectordev.mvp.ui.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.components.ParticipantAvatars
import com.hectordev.mvp.ui.core.extensions.toFormattedDate
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
internal fun PastEventCard(
    item: EventWithParticipants,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isWinner) {
                    Text(text = "🏆", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                text = "${item.event.startDate.toFormattedDate()} – ${item.event.endDate.toFormattedDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.event.locationLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.participants.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.home_event_participants, item.event.participants.size, item.event.participants.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ParticipantAvatars(
                        participants = item.participants,
                        totalCount = item.event.participants.size
                    )
                }
            }
        }
    }
}

// --- PREVIEWS ---

private val previewParticipants = listOf(
    User(id = "1", name = "Jack Shepard", email = ""),
    User(id = "2", name = "Kate Austen", email = ""),
    User(id = "3", name = "Sawyer Ford", email = "")
)

@Preview(showBackground = true, name = "MVP winner")
@Composable
private fun PastEventCardWinnerPreview() {
    MVPTheme {
        PastEventCard(
            item = EventWithParticipants(
                event = Event(
                    id = "1", title = "New Year Trip", status = EventStatus.FINISHED,
                    startDate = 1_700_000_000_000L, endDate = 1_700_200_000_000L,
                    locationLabel = "Paris, France",
                    participants = previewParticipants.map { it.id },
                    mvpId = "1", adminId = "1"
                ),
                participants = previewParticipants
            ),
            isWinner = true
        )
    }
}

@Preview(showBackground = true, name = "Not winner")
@Composable
private fun PastEventCardNotWinnerPreview() {
    MVPTheme {
        PastEventCard(
            item = EventWithParticipants(
                event = Event(
                    id = "2", title = "Birthday Weekend", status = EventStatus.FINISHED,
                    startDate = 1_680_000_000_000L, endDate = 1_680_100_000_000L,
                    participants = previewParticipants.take(2).map { it.id },
                    adminId = "1"
                ),
                participants = previewParticipants.take(2)
            ),
            isWinner = false
        )
    }
}