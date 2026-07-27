package com.hectordev.mvp.ui.features.gala

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.ui.features.feed.createTempImageUri

@Composable
fun GalaScreen(
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onViewFeed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GalaContent(
        uiState = uiState,
        onBack = onBack,
        onGoToGraph = onGoToGraph,
        onViewFeed = onViewFeed,
        onSelectCandidate = viewModel::selectCandidate,
        onSubmitVote = viewModel::submitVote,
        onUploadGalaPhoto = viewModel::uploadGalaPhoto,
        onErrorShown = viewModel::clearError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalaContent(
    uiState: GalaUiState,
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onViewFeed: () -> Unit,
    onSelectCandidate: (String) -> Unit,
    onSubmitVote: () -> Unit,
    onUploadGalaPhoto: (Uri) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { onUploadGalaPhoto(it) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onUploadGalaPhoto(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); onErrorShown() }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text(stringResource(R.string.gala_photo_source_title)) },
            confirmButton = {
                Button(onClick = {
                    showPhotoSourceDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = createTempImageUri(context); cameraUri = uri; cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text(stringResource(R.string.gala_photo_camera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.gala_photo_gallery)) }
            }
        )
    }

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.gala_title),
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
                    IconButton(onClick = onGoToGraph) {
                        Icon(Icons.Default.ShowChart, contentDescription = stringResource(R.string.score_graph_title))
                    }
                    IconButton(onClick = onViewFeed) {
                        Icon(Icons.Default.DynamicFeed, contentDescription = stringResource(R.string.gala_view_feed_btn))
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (uiState.eventStatus == EventStatus.FINISHED && uiState.mvpUser != null) {
                item { MvpWinnerCard(user = uiState.mvpUser) }
            }

            item {
                Text(
                    text = stringResource(R.string.gala_preliminary_standings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                TopStandingsRow(standings = uiState.topStandings)
            }

            item { HorizontalDivider() }

            // Predictions + final vote — visible in both VOTING_PHASE and FINISHED
            item {
                MyVotesSection(
                    myPrediction = uiState.myPrediction,
                    myFinalVoteId = uiState.myFinalVoteId,
                    participants = uiState.participants
                )
            }

            item { HorizontalDivider() }

            if (uiState.eventStatus == EventStatus.VOTING_PHASE) {
                val allVotesCast = uiState.totalParticipants > 0 &&
                        uiState.voteCount >= uiState.totalParticipants
                item {
                    if (allVotesCast) CalculatingWinnerBanner()
                    else BallotSection(
                        participants = uiState.participants.filter { it.id != uiState.currentUserId },
                        selectedCandidateId = uiState.selectedCandidateId,
                        hasVoted = uiState.hasVoted,
                        isSubmitting = uiState.isSubmittingVote,
                        onSelectCandidate = onSelectCandidate,
                        onSubmitVote = onSubmitVote
                    )
                }
                item { HorizontalDivider() }
                item { VoteCounter(voteCount = uiState.voteCount, total = uiState.totalParticipants) }
            }

            if (uiState.eventStatus == EventStatus.FINISHED) {
                uiState.galaPhotoUrl?.let { url ->
                    item { GalaPhotoSection(photoUrl = url, context = context) }
                }
                item {
                    GalaActions(
                        isAdmin = uiState.isAdmin,
                        isWinner = uiState.currentUserId == uiState.mvpId,
                        isUploadingPhoto = uiState.isUploadingPhoto,
                        hasPhoto = uiState.galaPhotoUrl != null,
                        onTakePhoto = { showPhotoSourceDialog = true }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}