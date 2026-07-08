package com.hectordev.mvp.ui.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hectordev.mvp.BuildConfig
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    onEventClick: (eventId: String) -> Unit = {},
    showEventCreatedMessage: Boolean = false,
    onEventCreatedMessageShown: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        showEventCreatedMessage = showEventCreatedMessage,
        onEventCreatedMessageShown = onEventCreatedMessageShown,
        onLogout = onLogout,
        onCreateEvent = onCreateEvent,
        onEventClick = onEventClick,
        onDeleteActiveEvent = viewModel::deleteActiveEvent,
        onAcceptInvitation = viewModel::acceptInvitation,
        onDeclineInvitation = viewModel::declineInvitation,
        onDeleteErrorShown = viewModel::clearDeleteError,
        onErrorShown = viewModel::clearError,
        onDebugMessageShown = viewModel::clearDebugMessage,
        onSeedPreTrip = viewModel::seedPreTripEvent,
        onSeedOnGoing = viewModel::seedOnGoingEvent,
        onSeedPastWon = viewModel::seedPastEventWon,
        onSeedPastLost = viewModel::seedPastEventLost,
        onClearSeedData = viewModel::clearSeedData
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    showEventCreatedMessage: Boolean = false,
    onEventCreatedMessageShown: () -> Unit = {},
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    onEventClick: (eventId: String) -> Unit = {},
    onDeleteActiveEvent: () -> Unit,
    onAcceptInvitation: (String) -> Unit = {},
    onDeclineInvitation: (String) -> Unit = {},
    onDeleteErrorShown: () -> Unit = {},
    onErrorShown: () -> Unit = {},
    onDebugMessageShown: () -> Unit = {},
    onSeedPreTrip: () -> Unit = {},
    onSeedOnGoing: () -> Unit = {},
    onSeedPastWon: () -> Unit = {},
    onSeedPastLost: () -> Unit = {},
    onClearSeedData: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var devMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val eventCreatedMessage = stringResource(R.string.home_event_created_success)

    LaunchedEffect(showEventCreatedMessage) {
        if (showEventCreatedMessage) {
            snackbarHostState.showSnackbar(eventCreatedMessage)
            onEventCreatedMessageShown()
        }
    }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            onDeleteErrorShown()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    LaunchedEffect(uiState.debugMessage) {
        uiState.debugMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDebugMessageShown()
        }
    }

    // Dev menu dialog — debug builds only
    if (devMenuExpanded && BuildConfig.DEBUG) {
        AlertDialog(
            onDismissRequest = { devMenuExpanded = false },
            title = { Text("🧪 Debug Menu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { onSeedPreTrip(); devMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add active event (Planning)") }
                    TextButton(
                        onClick = { onSeedOnGoing(); devMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add active event (Live)") }
                    TextButton(
                        onClick = { onSeedPastWon(); devMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add past event (you won 🏆)") }
                    TextButton(
                        onClick = { onSeedPastLost(); devMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add past event (you lost)") }
                    HorizontalDivider()
                    TextButton(
                        onClick = { onClearSeedData(); devMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear all seed data", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { devMenuExpanded = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${stringResource(R.string.home_welcome_greeting)} ${uiState.userName?.substringBefore(" ") ?: ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    Box {
                        AsyncImage(
                            model = uiState.userPhotoUrl,
                            contentDescription = stringResource(R.string.home_profile_photo_accessibility),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(
                                    onClick = { menuExpanded = true },
                                    onLongClick = { if (BuildConfig.DEBUG) devMenuExpanded = true }
                                )
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_menu_sign_out)) },
                                onClick = { menuExpanded = false; onLogout() }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canCreateEvent) {
                FloatingActionButton(onClick = onCreateEvent) {
                    Text(text = "+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active event
            item {
                Text(
                    text = stringResource(R.string.home_section_active_event),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            item {
                if (uiState.activeEvent != null) {
                    ActiveEventCard(
                        item = uiState.activeEvent,
                        isAdmin = uiState.activeEvent.event.adminId == uiState.currentUserId,
                        onDelete = onDeleteActiveEvent,
                        onClick = { onEventClick(uiState.activeEvent.event.id) }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.home_no_active_event),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Pending invitations
            if (uiState.pendingInvitations.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.home_section_pending_invitations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.pendingInvitations) { event ->
                    PendingInvitationCard(
                        event = event,
                        onAccept = { onAcceptInvitation(event.id) },
                        onDecline = { onDeclineInvitation(event.id) }
                    )
                }
            }

            // Upcoming events
            if (uiState.upcomingEvents.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.home_section_upcoming_events),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.upcomingEvents) { event ->
                    UpcomingEventCard(event = event)
                }
            }

            // Past events
            if (uiState.pastEvents.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.home_section_past_events),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.pastEvents) { item ->
                    PastEventCard(
                        item = item,
                        isWinner = item.event.mvpId == uiState.currentUserId
                    )
                }
            }

            item { Box(modifier = Modifier.padding(bottom = 80.dp)) }
        }
    }
}

// --- PREVIEW DATA ---

private val previewUsers = listOf(
    User(id = "1", name = "Jack Shepard", email = ""),
    User(id = "2", name = "Kate Austen", email = ""),
    User(id = "3", name = "Sawyer Ford", email = ""),
    User(id = "4", name = "John Locke", email = ""),
    User(id = "5", name = "Hurley Reyes", email = "")
)

private val previewUiState = HomeUiState(
    userName = "Jack Shepard",
    currentUserId = "1",
    canCreateEvent = false,
    activeEvent = EventWithParticipants(
        event = Event(
            id = "1", title = "Summer Trip 2025", status = EventStatus.PRE_TRIP,
            startDate = 1_750_000_000_000L, endDate = 1_750_500_000_000L,
            locationLabel = "Beach house, Ibiza",
            participants = previewUsers.map { it.id }, adminId = "1"
        ),
        participants = previewUsers
    ),
    pendingInvitations = listOf(
        Event(
            id = "4", title = "Skiing Weekend", status = EventStatus.PRE_TRIP,
            startDate = 1_760_000_000_000L, endDate = 1_760_200_000_000L,
            locationLabel = "Sierra Nevada", adminId = "2", participants = listOf("2")
        )
    ),
    upcomingEvents = listOf(
        Event(
            id = "5", title = "Road Trip South", status = EventStatus.PRE_TRIP,
            startDate = 1_770_000_000_000L, endDate = 1_770_500_000_000L,
            locationLabel = "Andalucía", adminId = "3", participants = listOf("1", "3")
        )
    ),
    pastEvents = listOf(
        EventWithParticipants(
            event = Event(
                id = "2", title = "New Year Trip", status = EventStatus.FINISHED,
                startDate = 1_700_000_000_000L, endDate = 1_700_200_000_000L,
                locationLabel = "Paris, France",
                participants = previewUsers.take(3).map { it.id }, mvpId = "1", adminId = "1"
            ),
            participants = previewUsers.take(3)
        )
    )
)

// --- SCREEN PREVIEWS ---

@Preview(showBackground = true, name = "All sections")
@Composable
private fun HomeScreenAllSectionsPreview() {
    MVPTheme {
        Surface {
            HomeScreenContent(
                uiState = previewUiState,
                onLogout = {},
                onCreateEvent = {},
                onEventClick = {},
                onDeleteActiveEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "No active event")
@Composable
private fun HomeScreenNoActiveEventPreview() {
    MVPTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeUiState(
                    userName = "Jack Shepard",
                    currentUserId = "1",
                    canCreateEvent = true,
                    pastEvents = previewUiState.pastEvents
                ),
                onLogout = {},
                onCreateEvent = {},
                onEventClick = {},
                onDeleteActiveEvent = {}
            )
        }
    }
}