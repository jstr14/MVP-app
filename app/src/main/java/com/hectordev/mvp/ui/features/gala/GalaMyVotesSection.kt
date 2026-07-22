package com.hectordev.mvp.ui.features.gala

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.User

@Composable
internal fun MyVotesSection(
    myPrediction: Prediction?,
    myFinalVoteId: String?,
    participants: List<User>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.gala_my_votes_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Pre-event predictions
        if (myPrediction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PredictionCard(
                    emoji = "🔮",
                    label = stringResource(R.string.event_details_prediction_mvp_label),
                    user = participants.find { it.id == myPrediction.projectedMvpId },
                    modifier = Modifier.weight(1f)
                )
                PredictionCard(
                    emoji = "🏀",
                    label = stringResource(R.string.event_details_prediction_triple_label),
                    user = participants.find { it.id == myPrediction.tripleParticipantId },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.gala_no_predictions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Final ballot vote
        myFinalVoteId?.let { votedId ->
            val votedUser = participants.find { it.id == votedId }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🗳", fontSize = 20.sp)
                    Text(
                        text = stringResource(R.string.gala_my_final_vote_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (votedUser != null) {
                        AsyncImage(
                            model = votedUser.photoUrl,
                            contentDescription = votedUser.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(
                            text = votedUser.name.substringBefore(" "),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(
    emoji: String,
    label: String,
    user: User?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AsyncImage(
                model = user?.photoUrl,
                contentDescription = user?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = user?.name?.substringBefore(" ") ?: "—",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}