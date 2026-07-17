package com.hectordev.mvp.ui.features.feed

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier

@Composable
fun LiveFeedScreen(
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToDetails: () -> Unit,
    onGoToGala: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showComposeSheet by remember { mutableStateOf(false) }
    var showEndEventConfirmation by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier.statusBarsPadding(),
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
                    if (uiState.isAdmin) {
                        TextButton(onClick = { showEndEventConfirmation = true }) {
                            Text(
                                text = stringResource(R.string.live_feed_end_event_btn),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = onGoToDetails) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.live_feed_event_details_accessibility))
                    }
                    TextButton(onClick = onGoToGraph) {
                        Text(stringResource(R.string.live_feed_graph_btn))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showComposeSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.live_feed_post_note_accessibility))
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
                    onToggleReaction = { emoji -> onToggleReaction(note, emoji) }
                )
            }
        }
    }
}