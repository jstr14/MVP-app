package com.hectordev.mvp.ui.features.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.components.FullScreenPhotoViewer
import com.hectordev.mvp.ui.core.components.TierBadge
import com.hectordev.mvp.ui.core.components.UserChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val REACTION_EMOJIS = listOf("😂", "🔥", "😭", "👀", "🤡", "👆", "🚨", "🤮", "💩", "👻")

@Composable
internal fun NoteCard(
    note: TimelineNote,
    participants: List<User>,
    isAdmin: Boolean,
    currentUserId: String,
    onDelete: () -> Unit,
    onToggleReaction: (String) -> Unit,
    viewerMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val author = participants.find { it.id == note.authorId }
    val target = participants.find { it.id == note.targetUserId }
    var fullScreenUrl by remember { mutableStateOf<String?>(null) }

    fullScreenUrl?.let { url ->
        FullScreenPhotoViewer(url = url, onDismiss = { fullScreenUrl = null })
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    UserChip(user = author)
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    UserChip(user = target)
                }
                if (!viewerMode && (isAdmin || currentUserId == note.authorId)) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.live_feed_delete_note_accessibility),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            TierBadge(tier = note.tier)

            if (note.type == NoteType.PHOTO && note.contentUrl != null) {
                AsyncImage(
                    model = note.contentUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { fullScreenUrl = note.contentUrl }
                )
            } else if (note.type == NoteType.GIF && note.contentUrl != null) {
                AsyncImage(
                    model = note.contentUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { fullScreenUrl = note.contentUrl }
                )
            }

            note.textContent?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            if (!viewerMode) {
                ReactionRow(
                    reactions = note.reactions,
                    currentUserId = currentUserId,
                    onToggleReaction = onToggleReaction
                )
            }

            Text(
                text = note.timestamp.toHourMinute(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionRow(
    reactions: Map<String, List<String>>,
    currentUserId: String,
    onToggleReaction: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            reactions.entries
                .filter { it.value.isNotEmpty() }
                .sortedByDescending { it.value.size }
                .forEach { (emoji, users) ->
                    ReactionChip(
                        emoji = emoji,
                        count = users.size,
                        isReacted = currentUserId in users,
                        onClick = { onToggleReaction(emoji) }
                    )
                }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Surface(
                    onClick = { showPicker = !showPicker },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_reaction),
                        contentDescription = stringResource(R.string.live_feed_add_reaction_accessibility),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showPicker) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    REACTION_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .clickable {
                                    onToggleReaction(emoji)
                                    showPicker = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionChip(
    emoji: String,
    count: Int,
    isReacted: Boolean,
    onClick: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isReacted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isReacted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Long.toHourMinute(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))