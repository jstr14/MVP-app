package com.hectordev.mvp.ui.features.feed

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User

@Composable
fun LiveFeedScreen(
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToDetails: () -> Unit,
    onGoToGala: () -> Unit,
    viewerMode: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: LiveFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveFeedContent(
        uiState = uiState,
        onBack = onBack,
        onGoToGraph = onGoToGraph,
        onGoToDetails = onGoToDetails,
        onGoToGala = onGoToGala,
        onEndEvent = viewModel::endEvent,
        onClearNavigateToGala = viewModel::clearNavigateToGala,
        onPostNote = { tier, targetId, text, uri, gifUrl -> viewModel.postNote(tier, targetId, text, uri, gifUrl) },
        onPostSuccessConsumed = viewModel::clearPostSuccess,
        onDeleteNote = viewModel::deleteNote,
        onToggleReaction = viewModel::toggleReaction,
        onErrorShown = viewModel::clearError,
        onTriggerEmergency = viewModel::triggerEmergency,
        onCastEmergencyVote = viewModel::castEmergencyVote,
        viewerMode = viewerMode,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiveFeedContent(
    uiState: LiveFeedUiState,
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToDetails: () -> Unit,
    onGoToGala: () -> Unit,
    onEndEvent: () -> Unit,
    onClearNavigateToGala: () -> Unit,
    onPostNote: (TimelineTier, String, String, Uri?, String?) -> Unit,
    onPostSuccessConsumed: () -> Unit,
    onDeleteNote: (TimelineNote) -> Unit,
    onToggleReaction: (TimelineNote, String) -> Unit,
    onErrorShown: () -> Unit,
    onTriggerEmergency: (String) -> Unit,
    onCastEmergencyVote: (Boolean) -> Unit,
    viewerMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showComposeSheet by remember { mutableStateOf(false) }
    var showEndEventConfirmation by remember { mutableStateOf(false) }
    var showEmergencySheet by remember { mutableStateOf(false) }
    var emergencyTargetId by remember { mutableStateOf("") }
    var showEmergencyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.navigateToGala) {
        if (uiState.navigateToGala) {
            onGoToGala()
            onClearNavigateToGala()
        }
    }

    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) listState.animateScrollToItem(0)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    if (showEndEventConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndEventConfirmation = false },
            title = { Text(stringResource(R.string.live_feed_end_event_confirm_title)) },
            text = { Text(stringResource(R.string.live_feed_end_event_confirm_message)) },
            confirmButton = {
                Button(onClick = { onEndEvent(); showEndEventConfirmation = false }) {
                    Text(stringResource(R.string.live_feed_end_event_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndEventConfirmation = false }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    if (showComposeSheet) {
        ComposeNoteBottomSheet(
            participants = uiState.participants.filter { it.id != uiState.currentUserId },
            isPosting = uiState.isPosting,
            postSuccess = uiState.postSuccess,
            onPost = { tier, targetId, text, uri, gifUrl -> onPostNote(tier, targetId, text, uri, gifUrl) },
            onDismiss = { showComposeSheet = false },
            onPostSuccessConsumed = onPostSuccessConsumed
        )
    }

    // Emergency target selection sheet
    if (showEmergencySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEmergencySheet = false },
            sheetState = sheetState
        ) {
            EmergencyTargetSheet(
                participants = uiState.participants.filter { it.id != uiState.currentUserId },
                onTargetSelected = { targetId ->
                    emergencyTargetId = targetId
                    showEmergencySheet = false
                    showEmergencyConfirm = true
                },
                onDismiss = { showEmergencySheet = false }
            )
        }
    }

    // Emergency confirmation dialog
    if (showEmergencyConfirm) {
        val targetName = uiState.participants.find { it.id == emergencyTargetId }?.name ?: emergencyTargetId
        AlertDialog(
            onDismissRequest = { showEmergencyConfirm = false },
            title = { Text(stringResource(R.string.emergency_confirm_title)) },
            text = { Text(stringResource(R.string.emergency_confirm_message, targetName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onTriggerEmergency(emergencyTargetId)
                        showEmergencyConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text(stringResource(R.string.emergency_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyConfirm = false }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    Box(modifier = modifier) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.eventTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Emergency clause button — visible to non-viewer participants who haven't used theirs
                    if (!viewerMode && uiState.activeEmergencyId == null && !uiState.hasUsedEmergency) {
                        IconButton(onClick = { showEmergencySheet = true }) {
                            Icon(
                                Icons.Default.NotificationImportant,
                                contentDescription = stringResource(R.string.emergency_btn_accessibility),
                                tint = Color.Red
                            )
                        }
                    }
                    if (uiState.isAdmin && !viewerMode) {
                        IconButton(onClick = { showEndEventConfirmation = true }) {
                            Icon(
                                Icons.Default.HowToVote,
                                contentDescription = stringResource(R.string.live_feed_end_event_btn)
                            )
                        }
                    }
                    IconButton(onClick = onGoToDetails) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.live_feed_event_details_accessibility))
                    }
                    IconButton(onClick = onGoToGraph) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = stringResource(R.string.live_feed_graph_btn))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!viewerMode) {
                FloatingActionButton(onClick = { showComposeSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.live_feed_post_note_accessibility))
                }
            }
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (uiState.notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.live_feed_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(uiState.notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    participants = uiState.participants,
                    isAdmin = uiState.isAdmin,
                    currentUserId = uiState.currentUserId,
                    onDelete = { onDeleteNote(note) },
                    onToggleReaction = { emoji -> onToggleReaction(note, emoji) },
                    viewerMode = viewerMode
                )
            }
        }
    }

    // Emergency blocking overlay
    if (uiState.emergencyRequest != null && uiState.activeEmergencyId != null) {
        EmergencyOverlay(
            emergencyRequest = uiState.emergencyRequest,
            participants = uiState.participants,
            currentUserId = uiState.currentUserId,
            totalParticipants = uiState.totalParticipants,
            countdownSeconds = uiState.emergencyCountdownSeconds,
            onAccept = { onCastEmergencyVote(true) },
            onDecline = { onCastEmergencyVote(false) }
        )
    }
    } // end Box
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyTargetSheet(
    participants: List<User>,
    onTargetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.emergency_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            text = stringResource(R.string.emergency_sheet_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        participants.forEach { participant ->
            OutlinedButton(
                onClick = { onTargetSelected(participant.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(participant.name)
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_cancel))
        }
    }
}