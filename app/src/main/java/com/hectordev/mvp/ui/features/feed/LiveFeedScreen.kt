package com.hectordev.mvp.ui.features.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.tooling.preview.Preview
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.ui.core.theme.MVPTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.hectordev.mvp.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveFeedScreen(
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LiveFeedContent(
        uiState = uiState,
        onBack = onBack,
        onGoToGraph = onGoToGraph,
        onGoToDetails = onGoToDetails,
        onPostNote = viewModel::postNote,
        onDeleteNote = viewModel::deleteNote,
        onErrorShown = viewModel::clearError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveFeedContent(
    uiState: LiveFeedUiState,
    onBack: () -> Unit,
    onGoToGraph: () -> Unit,
    onGoToDetails: () -> Unit,
    onPostNote: (TimelineTier, String, String) -> Unit,
    onDeleteNote: (TimelineNote) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showComposeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    if (showComposeSheet) {
        ComposeNoteBottomSheet(
            participants = uiState.participants.filter { it.id != uiState.currentUserId },
            isPosting = uiState.isPosting,
            onPost = { tier, targetId, text ->
                onPostNote(tier, targetId, text)
                showComposeSheet = false
            },
            onDismiss = { showComposeSheet = false }
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
                    onDelete = { onDeleteNote(note) }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: TimelineNote,
    participants: List<User>,
    isAdmin: Boolean,
    currentUserId: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val author = participants.find { it.id == note.authorId }
    val target = participants.find { it.id == note.targetUserId }

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
                if (isAdmin || currentUserId == note.authorId) {
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

            note.textContent?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun UserChip(user: User?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AsyncImage(
            model = user?.photoUrl,
            contentDescription = user?.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = user?.name?.substringBefore(" ") ?: "?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TierBadge(tier: TimelineTier, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = when (tier) {
        TimelineTier.FACT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TimelineTier.HOT_TAKE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TimelineTier.WITNESSED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TimelineTier.LORE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = "${tier.label.uppercase()}  ${pluralStringResource(R.plurals.live_feed_tier_points, tier.points, tier.points)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeNoteBottomSheet(
    participants: List<User>,
    isPosting: Boolean,
    onPost: (TimelineTier, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTier by remember { mutableStateOf<TimelineTier?>(null) }
    var selectedTargetId by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }

    val canPost = selectedTier != null && selectedTargetId.isNotEmpty() && textInput.isNotBlank() && !isPosting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.live_feed_compose_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tier selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.live_feed_compose_pick_tier),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineTier.entries.take(2).forEach { tier ->
                        TierOption(
                            tier = tier,
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineTier.entries.drop(2).forEach { tier ->
                        TierOption(
                            tier = tier,
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Target selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.live_feed_compose_nominate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(participants) { user ->
                        FilterChip(
                            selected = selectedTargetId == user.id,
                            onClick = { selectedTargetId = user.id },
                            label = { Text(user.name.substringBefore(" ")) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(stringResource(R.string.live_feed_compose_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Button(
                onClick = {
                    val tier = selectedTier ?: return@Button
                    onPost(tier, selectedTargetId, textInput)
                },
                enabled = canPost,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.live_feed_compose_post_btn))
                }
            }
        }
    }
}

@Composable
private fun TierOption(
    tier: TimelineTier,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (tier) {
        TimelineTier.FACT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TimelineTier.HOT_TAKE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TimelineTier.WITNESSED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TimelineTier.LORE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val borderModifier = if (selected) {
        Modifier.border(2.dp, contentColor, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = tier.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = pluralStringResource(R.plurals.live_feed_tier_points, tier.points, tier.points),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun Long.toHourMinute(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))

// --- PREVIEW DATA ---

private val previewUsers = listOf(
    User(id = "1", name = "Jack Shepard", email = "jack@test.com"),
    User(id = "2", name = "Kate Austen", email = "kate@test.com"),
    User(id = "3", name = "Sawyer Ford", email = "sawyer@test.com"),
)

private val previewNotes = listOf(
    TimelineNote(
        id = "1", authorId = "1", targetUserId = "2",
        type = NoteType.TEXT, tier = TimelineTier.HOT_TAKE, pointsAwarded = 2,
        textContent = "She absolutely carried the whole dinner.",
        timestamp = 1_750_000_000_000L
    ),
    TimelineNote(
        id = "2", authorId = "3", targetUserId = "1",
        type = NoteType.TEXT, tier = TimelineTier.LORE, pointsAwarded = 10,
        textContent = "This man ordered dessert before anyone finished their starter.",
        timestamp = 1_750_001_000_000L
    ),
    TimelineNote(
        id = "3", authorId = "2", targetUserId = "3",
        type = NoteType.TEXT, tier = TimelineTier.FACT, pointsAwarded = 1,
        textContent = "Lost at every single card game.",
        timestamp = 1_750_002_000_000L
    ),
    TimelineNote(
        id = "4", authorId = "1", targetUserId = "3",
        type = NoteType.TEXT, tier = TimelineTier.WITNESSED, pointsAwarded = 5,
        textContent = "Broke the vibe machine at the hotel bar. We all saw it.",
        timestamp = 1_750_003_000_000L
    )
)

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Feed — With notes (admin)")
@Composable
private fun LiveFeedWithNotesAdminPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = previewNotes,
                    participants = previewUsers,
                    currentUserId = "1",
                    isAdmin = true,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onPostNote = { _, _, _ -> },
                onDeleteNote = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Feed — With notes (participant)")
@Composable
private fun LiveFeedWithNotesParticipantPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = previewNotes,
                    participants = previewUsers,
                    currentUserId = "2",
                    isAdmin = false,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onPostNote = { _, _, _ -> },
                onDeleteNote = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Feed — Empty state")
@Composable
private fun LiveFeedEmptyPreview() {
    MVPTheme {
        Surface {
            LiveFeedContent(
                uiState = LiveFeedUiState(
                    eventTitle = "Summer Trip 2025",
                    notes = emptyList(),
                    participants = previewUsers,
                    currentUserId = "1",
                    isAdmin = false,
                    isLoading = false
                ),
                onBack = {},
                onGoToGraph = {},
                onGoToDetails = {},
                onPostNote = { _, _, _ -> },
                onDeleteNote = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "NoteCard — Lore (admin)")
@Composable
private fun NoteCardLorePreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = previewNotes[1],
                participants = previewUsers,
                isAdmin = true,
                currentUserId = "1",
                onDelete = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "NoteCard — Fact (participant)")
@Composable
private fun NoteCardFactPreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            NoteCard(
                note = previewNotes[2],
                participants = previewUsers,
                isAdmin = false,
                currentUserId = "2",
                onDelete = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tier badges — All tiers")
@Composable
private fun TierBadgesPreview() {
    MVPTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineTier.entries.forEach { tier -> TierBadge(tier = tier) }
            }
        }
    }
}

@Preview(showBackground = true, name = "Tier options — Selected vs unselected")
@Composable
private fun TierOptionsPreview() {
    MVPTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TierOption(
                        tier = TimelineTier.FACT,
                        selected = false,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    TierOption(
                        tier = TimelineTier.HOT_TAKE,
                        selected = true,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TierOption(
                        tier = TimelineTier.WITNESSED,
                        selected = false,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    TierOption(
                        tier = TimelineTier.LORE,
                        selected = true,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}