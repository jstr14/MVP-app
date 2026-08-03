package com.hectordev.mvp.ui.features.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.EmergencyRequest
import com.hectordev.mvp.domain.EmergencyStatus
import com.hectordev.mvp.domain.User

@Composable
fun EmergencyOverlay(
    emergencyRequest: EmergencyRequest,
    participants: List<User>,
    currentUserId: String,
    totalParticipants: Int,
    countdownSeconds: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current
    val isTriggerer = currentUserId == emergencyRequest.triggeredById
    val hasVoted = currentUserId in emergencyRequest.votesAccept ||
            currentUserId in emergencyRequest.votesDecline

    fun userName(userId: String) = participants.find { it.id == userId }?.name ?: userId

    val mm = countdownSeconds / 60
    val ss = countdownSeconds % 60
    val countdown = "%02d:%02d".format(mm, ss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.NotificationImportant,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Red
                )

                Text(
                    text = stringResource(R.string.emergency_overlay_triggered_by, userName(emergencyRequest.triggeredById)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.emergency_overlay_target, userName(emergencyRequest.targetUserId)),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                // Vote counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val pending = totalParticipants - emergencyRequest.votesAccept.size - emergencyRequest.votesDecline.size
                    Text(
                        text = stringResource(R.string.emergency_overlay_votes_accept, emergencyRequest.votesAccept.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = stringResource(R.string.emergency_overlay_votes_decline, emergencyRequest.votesDecline.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (pending > 0) {
                        Text(
                            text = "⏳ $pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Countdown
                Text(
                    text = stringResource(R.string.emergency_overlay_time_remaining, countdown),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (countdownSeconds < 30) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (emergencyRequest.status != EmergencyStatus.PENDING) {
                    Text(
                        text = when (emergencyRequest.status) {
                            EmergencyStatus.APPROVED_SHUTDOWN -> stringResource(R.string.emergency_overlay_approved)
                            EmergencyStatus.REJECTED -> stringResource(R.string.emergency_overlay_rejected)
                            EmergencyStatus.TIMED_OUT -> stringResource(R.string.emergency_overlay_timed_out)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (emergencyRequest.status) {
                            EmergencyStatus.APPROVED_SHUTDOWN -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                } else if (isTriggerer) {
                    Text(
                        text = stringResource(R.string.emergency_overlay_waiting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else if (!hasVoted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.emergency_overlay_decline))
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text(stringResource(R.string.emergency_overlay_accept))
                        }
                    }
                } else {
                    Text(
                        text = if (currentUserId in emergencyRequest.votesAccept)
                            stringResource(R.string.emergency_overlay_voted_accept)
                        else
                            stringResource(R.string.emergency_overlay_voted_decline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Emergency call button
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Text(
                        text = stringResource(R.string.emergency_overlay_call_112),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}