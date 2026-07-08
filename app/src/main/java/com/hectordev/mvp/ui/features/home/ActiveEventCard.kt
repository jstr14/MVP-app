package com.hectordev.mvp.ui.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.hectordev.mvp.ui.core.components.EventStatusChip
import com.hectordev.mvp.ui.core.components.ParticipantAvatars
import com.hectordev.mvp.ui.core.extensions.toFormattedDate
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
internal fun ActiveEventCard(
    item: EventWithParticipants,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.home_delete_event_confirm_title)) },
            text = { Text(stringResource(R.string.home_delete_event_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirmation = false }) {
                    Text(
                        text = stringResource(R.string.home_delete_event_btn),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                EventStatusChip(status = item.event.status)
                if (isAdmin) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.home_delete_event),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { showMenu = false; showDeleteConfirmation = true }
                            )
                        }
                    }
                }
            }
            Text(
                text = "${item.event.startDate.toFormattedDate()} – ${item.event.endDate.toFormattedDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            item.event.locationLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(R.plurals.home_event_participants, item.event.participants.size, item.event.participants.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (item.participants.isNotEmpty()) {
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
    User(id = "3", name = "Sawyer Ford", email = ""),
    User(id = "4", name = "John Locke", email = ""),
    User(id = "5", name = "Hurley Reyes", email = "")
)

private val previewEvent = EventWithParticipants(
    event = Event(
        id = "1", title = "Summer Trip 2025", status = EventStatus.PRE_TRIP,
        startDate = 1_750_000_000_000L, endDate = 1_750_500_000_000L,
        locationLabel = "Beach house, Ibiza",
        participants = previewParticipants.map { it.id }, adminId = "1"
    ),
    participants = previewParticipants
)

@Preview(showBackground = true, name = "Admin — Planning")
@Composable
private fun ActiveEventCardAdminPreview() {
    MVPTheme {
        ActiveEventCard(item = previewEvent, isAdmin = true, onDelete = {})
    }
}

@Preview(showBackground = true, name = "Participant — Live")
@Composable
private fun ActiveEventCardParticipantPreview() {
    MVPTheme {
        ActiveEventCard(
            item = previewEvent.copy(
                event = previewEvent.event.copy(status = EventStatus.ON_GOING)
            ),
            isAdmin = false,
            onDelete = {}
        )
    }
}