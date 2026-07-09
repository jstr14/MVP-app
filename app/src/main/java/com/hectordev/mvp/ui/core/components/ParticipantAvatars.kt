package com.hectordev.mvp.ui.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun ParticipantAvatars(
    participants: List<User>,
    totalCount: Int = participants.size,
    modifier: Modifier = Modifier
) {
    val maxVisible = 4
    val visible = participants.take(maxVisible)
    val overflow = totalCount - visible.size

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-8.dp)
    ) {
        visible.forEach { user ->
            AsyncImage(
                model = user.photoUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${overflow}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "4 visible")
@Composable
private fun ParticipantAvatarsFourPreview() {
    MVPTheme {
        Surface {
            ParticipantAvatars(
                participants = listOf(
                    User(id = "1", name = "Jack", email = ""),
                    User(id = "2", name = "Kate", email = ""),
                    User(id = "3", name = "Sawyer", email = ""),
                    User(id = "4", name = "Locke", email = "")
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "With overflow")
@Composable
private fun ParticipantAvatarsOverflowPreview() {
    MVPTheme {
        Surface {
            ParticipantAvatars(
                participants = listOf(
                    User(id = "1", name = "Jack", email = ""),
                    User(id = "2", name = "Kate", email = ""),
                    User(id = "3", name = "Sawyer", email = ""),
                    User(id = "4", name = "Locke", email = ""),
                    User(id = "5", name = "Hurley", email = ""),
                    User(id = "6", name = "Claire", email = "")
                )
            )
        }
    }
}