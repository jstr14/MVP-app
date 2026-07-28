package com.hectordev.mvp.ui.features.profile

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.hectordev.mvp.ui.core.theme.MVPTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BadgesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BadgesContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgesContent(
    uiState: BadgesUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var reactorTapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    val mediaPlayer: MediaPlayer? = remember { MediaPlayer.create(context, R.raw.waluigi_sound) }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    LaunchedEffect(showEasterEgg) {
        if (showEasterEgg) {
            scale.snapTo(0f)
            rotation.snapTo(-20f)
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            coroutineScope {
                launch {
                    scale.animateTo(
                        1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
                launch {
                    rotation.animateTo(
                        5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
            delay(2500L)
            mediaPlayer?.pause()
            showEasterEgg = false
        }
    }

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.badges_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // User avatar + name
            AsyncImage(
                model = uiState.userPhotoUrl,
                contentDescription = uiState.userName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.badges_achievements_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            BadgeRow(
                emoji = "🏆",
                title = stringResource(R.string.badges_mvp_title),
                description = stringResource(R.string.badges_mvp_description),
                lockedMessage = stringResource(R.string.badges_mvp_locked),
                count = uiState.lifetimeMvps
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            BadgeRow(
                emoji = "🔮",
                title = stringResource(R.string.badges_oracle_title),
                description = stringResource(R.string.badges_oracle_description),
                lockedMessage = stringResource(R.string.badges_oracle_locked),
                count = uiState.oracleWins
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            BadgeRow(
                emoji = "🏀",
                title = stringResource(R.string.badges_triple_title),
                description = stringResource(R.string.badges_triple_description),
                lockedMessage = stringResource(R.string.badges_triple_locked),
                count = uiState.tripleWins
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            MysteryBadgeRow(
                earnedEmoji = "⚡",
                earnedTitle = stringResource(R.string.badges_reactor_title),
                earnedDescription = stringResource(R.string.badges_reactor_description),
                count = uiState.reactorWins,
                onClick = {
                    reactorTapCount++
                    if (reactorTapCount >= 7) {
                        showEasterEgg = true
                        reactorTapCount = 0
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            MysteryBadgeRow(
                earnedEmoji = "🌟",
                earnedTitle = stringResource(R.string.badges_total_player_title),
                earnedDescription = stringResource(R.string.badges_total_player_description),
                count = uiState.totalPlayerWins
            )
        }

        // Easter egg overlay
        if (showEasterEgg) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        mediaPlayer?.pause()
                        showEasterEgg = false
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.waluigi_sticker),
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            rotationZ = rotation.value
                        }
                )
            }
        }
        } // close outer Box
    }
}

@Composable
private fun BadgeRow(
    emoji: String,
    title: String,
    description: String,
    lockedMessage: String,
    count: Int
) {
    val earned = count > 0
    val alpha = if (earned) 1f else 0.35f

    Row(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = emoji, fontSize = 36.sp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (earned) {
                    Text(
                        text = "× $count",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (earned) description else lockedMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MysteryBadgeRow(
    earnedEmoji: String,
    earnedTitle: String,
    earnedDescription: String,
    count: Int,
    onClick: (() -> Unit)? = null
) {
    val earned = count > 0
    val alpha = if (earned) 1f else 0.35f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (earned) earnedEmoji else "❓",
            fontSize = 36.sp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (earned) earnedTitle else stringResource(R.string.badges_mystery_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (earned) {
                    Text(
                        text = "× $count",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (earned) earnedDescription else stringResource(R.string.badges_mystery_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Badges — all earned")
@Composable
private fun BadgesAllEarnedPreview() {
    MVPTheme {
        Surface {
            BadgesContent(
                uiState = BadgesUiState(
                    userName = "Jack Shepard",
                    lifetimeMvps = 3,
                    oracleWins = 2,
                    tripleWins = 1,
                    reactorWins = 2,
                    totalPlayerWins = 1,
                    isLoading = false
                ),
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Badges — some earned")
@Composable
private fun BadgesSomeEarnedPreview() {
    MVPTheme {
        Surface {
            BadgesContent(
                uiState = BadgesUiState(
                    userName = "Kate Austen",
                    lifetimeMvps = 1,
                    oracleWins = 0,
                    tripleWins = 0,
                    isLoading = false
                ),
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Badges — none earned")
@Composable
private fun BadgesNoneEarnedPreview() {
    MVPTheme {
        Surface {
            BadgesContent(
                uiState = BadgesUiState(
                    userName = "Sawyer Ford",
                    lifetimeMvps = 0,
                    oracleWins = 0,
                    tripleWins = 0,
                    isLoading = false
                ),
                onBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Badges — dark mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BadgesDarkPreview() {
    MVPTheme {
        Surface {
            BadgesContent(
                uiState = BadgesUiState(
                    userName = "John Locke",
                    lifetimeMvps = 2,
                    oracleWins = 1,
                    tripleWins = 0,
                    isLoading = false
                ),
                onBack = {}
            )
        }
    }
}