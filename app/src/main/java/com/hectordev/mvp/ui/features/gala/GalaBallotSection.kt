package com.hectordev.mvp.ui.features.gala

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.User

@Composable
internal fun BallotSection(
    participants: List<User>,
    selectedCandidateId: String,
    hasVoted: Boolean,
    isSubmitting: Boolean,
    onSelectCandidate: (String) -> Unit,
    onSubmitVote: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val selectedName = participants.find { it.id == selectedCandidateId }?.name?.substringBefore(" ") ?: ""

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.gala_vote_confirm_title)) },
            text = { Text(stringResource(R.string.gala_vote_confirm_message, selectedName)) },
            confirmButton = {
                Button(onClick = { showConfirmDialog = false; onSubmitVote() }) {
                    Text(stringResource(R.string.gala_vote_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.gala_ballot_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (hasVoted) {
            Text(
                text = stringResource(R.string.gala_vote_cast),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(participants) { user ->
                    FilterChip(
                        selected = selectedCandidateId == user.id,
                        onClick = { onSelectCandidate(user.id) },
                        label = { Text(user.name.substringBefore(" ")) },
                        leadingIcon = {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    )
                }
            }
            Button(
                onClick = { showConfirmDialog = true },
                enabled = selectedCandidateId.isNotEmpty() && !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.gala_vote_btn))
                }
            }
        }
    }
}

@Composable
internal fun CalculatingWinnerBanner() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.gala_calculating_winner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun VoteCounter(voteCount: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.gala_votes_cast, voteCount, total),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        if (voteCount < total) {
            Text(
                text = stringResource(R.string.gala_waiting_votes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}