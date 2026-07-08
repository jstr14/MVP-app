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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.ui.core.extensions.toFormattedDate
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
internal fun PendingInvitationCard(
    event: Event,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${event.startDate.toFormattedDate()} – ${event.endDate.toFormattedDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            event.locationLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDecline) {
                    Text(
                        text = stringResource(R.string.home_pending_decline),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onAccept) {
                    Text(
                        text = stringResource(R.string.home_pending_accept),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

// --- PREVIEW ---

@Preview(showBackground = true)
@Composable
private fun PendingInvitationCardPreview() {
    MVPTheme {
        PendingInvitationCard(
            event = Event(
                id = "1", title = "Skiing Weekend", status = EventStatus.PRE_TRIP,
                startDate = 1_760_000_000_000L, endDate = 1_760_200_000_000L,
                locationLabel = "Sierra Nevada", adminId = "2", participants = listOf("2")
            ),
            onAccept = {},
            onDecline = {}
        )
    }
}