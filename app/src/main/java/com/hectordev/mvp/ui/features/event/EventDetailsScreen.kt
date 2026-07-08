package com.hectordev.mvp.ui.features.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.components.EventStatusChip
import com.hectordev.mvp.ui.core.extensions.toFormattedDate
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    EventDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onStartEvent = viewModel::startEvent,
        onInvite = viewModel::inviteByEmail,
        onClearInviteState = viewModel::clearInviteState,
        onRemoveParticipant = viewModel::removeParticipant,
        onCancelInvite = viewModel::cancelInvite,
        onCancelEmailInvite = viewModel::cancelEmailInvite,
        onErrorShown = viewModel::clearError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailsContent(
    uiState: EventDetailsUiState,
    onBack: () -> Unit,
    onStartEvent: () -> Unit,
    onInvite: (String) -> Unit,
    onClearInviteState: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onCancelEmailInvite: (String) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartConfirmation by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmailInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    LaunchedEffect(uiState.inviteSuccess) {
        if (uiState.inviteSuccess) {
            showInviteDialog = false
            inviteEmailInput = ""
            onClearInviteState()
        }
    }

    if (showStartConfirmation) {
        AlertDialog(
            onDismissRequest = { showStartConfirmation = false },
            title = { Text(stringResource(R.string.event_details_start_confirm_title)) },
            text = { Text(stringResource(R.string.event_details_start_confirm_message)) },
            confirmButton = {
                Button(onClick = { onStartEvent(); showStartConfirmation = false }) {
                    Text(stringResource(R.string.event_details_start_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartConfirmation = false }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; onClearInviteState() },
            title = { Text(stringResource(R.string.event_details_invite_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inviteEmailInput,
                        onValueChange = { inviteEmailInput = it; onClearInviteState() },
                        label = { Text(stringResource(R.string.event_details_invite_email_label)) },
                        placeholder = { Text(stringResource(R.string.event_details_invite_email_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = uiState.inviteError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.inviteError != null) {
                        Text(
                            text = stringResource(
                                when (uiState.inviteError) {
                                    InviteError.USER_NOT_FOUND -> R.string.event_details_invite_error_not_found
                                    InviteError.ALREADY_MEMBER -> R.string.event_details_invite_error_already_member
                                    InviteError.UNKNOWN -> R.string.event_details_invite_error_unknown
                                }
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onInvite(inviteEmailInput) },
                    enabled = inviteEmailInput.isNotBlank() && !uiState.isInviting
                ) {
                    if (uiState.isInviting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.event_details_invite_btn))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false; onClearInviteState() }) {
                    Text(stringResource(R.string.home_cancel))
                }
            }
        )
    }

    val event = uiState.event
    val isAdmin = event?.adminId == uiState.currentUserId
    val showStartAction = isAdmin && event?.status == EventStatus.PRE_TRIP

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = event?.title ?: "",
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
                    if (showStartAction) {
                        TextButton(onClick = { showStartConfirmation = true }) {
                            Text(stringResource(R.string.event_details_start_event_btn))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val loadedEvent = uiState.event ?: return@Scaffold
        val loadedIsAdmin = loadedEvent.adminId == uiState.currentUserId

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventStatusChip(status = loadedEvent.status)
                    Text(
                        text = "${loadedEvent.startDate.toFormattedDate()} – ${loadedEvent.endDate.toFormattedDate()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    loadedEvent.locationLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.event_details_section_participants),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 12.dp, bottom = 4.dp)
                    )
                    if (loadedIsAdmin && loadedEvent.status == EventStatus.PRE_TRIP) {
                        IconButton(onClick = { showInviteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.event_details_invite_participant_btn)
                            )
                        }
                    }
                }
            }

            items(uiState.participants) { user ->
                val canRemove = loadedIsAdmin && loadedEvent.status == EventStatus.PRE_TRIP && user.id != loadedEvent.adminId
                ParticipantRow(
                    user = user,
                    isAdmin = user.id == loadedEvent.adminId,
                    onRemove = if (canRemove) ({ onRemoveParticipant(user.id) }) else null
                )
            }

            val pendingEmails = loadedEvent.pendingEmails
            val hasPendingSection = uiState.pendingParticipants.isNotEmpty() || pendingEmails.isNotEmpty()

            if (hasPendingSection) {
                item {
                    Text(
                        text = stringResource(R.string.event_details_section_pending),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.pendingParticipants) { user ->
                    ParticipantRow(
                        user = user,
                        isAdmin = false,
                        isPending = true,
                        onRemove = if (loadedIsAdmin && loadedEvent.status == EventStatus.PRE_TRIP)
                            ({ onCancelInvite(user.id) }) else null
                    )
                }
                items(pendingEmails) { email ->
                    PendingEmailRow(
                        email = email,
                        onRemove = if (loadedIsAdmin && loadedEvent.status == EventStatus.PRE_TRIP)
                            ({ onCancelEmailInvite(email) }) else null
                    )
                }
            }

            item { Box(modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun ParticipantRow(
    user: User,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    isPending: Boolean = false,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = user.name.substringBefore(" "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
        )
        when {
            isAdmin -> Text(
                text = stringResource(R.string.event_details_admin_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            isPending -> Text(
                text = stringResource(R.string.event_details_pending_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.event_details_remove_participant),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PendingEmailRow(
    email: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = email.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.event_details_pending_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.event_details_remove_participant),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- PREVIEW DATA ---

private val previewParticipants = listOf(
    User(id = "1", name = "Jack Shepard", email = "jack@test.com"),
    User(id = "2", name = "Kate Austen", email = "kate@test.com"),
    User(id = "3", name = "Sawyer Ford", email = "sawyer@test.com"),
    User(id = "4", name = "John Locke", email = "locke@test.com")
)

private val previewPendingParticipants = listOf(
    User(id = "5", name = "Hugo Reyes", email = "hurley@test.com"),
)

private val previewPendingEmails = listOf(
    "charlie.pace@test.com",
    "shannon.rutherford@test.com"
)

// --- SCREEN PREVIEWS ---

@Preview(showBackground = true, name = "Admin — Planning")
@Composable
private fun EventDetailsAdminPreTripPreview() {
    MVPTheme {
        Surface {
            EventDetailsContent(
                uiState = EventDetailsUiState(
                    currentUserId = "1",
                    isLoading = false,
                    participants = previewParticipants,
                    pendingParticipants = previewPendingParticipants,
                    event = Event(
                        id = "1", title = "Summer Trip 2025",
                        status = EventStatus.PRE_TRIP,
                        startDate = 1_750_000_000_000L, endDate = 1_750_500_000_000L,
                        locationLabel = "Beach house, Ibiza",
                        adminId = "1",
                        participants = previewParticipants.map { it.id },
                        pendingParticipants = previewPendingParticipants.map { it.id },
                        pendingEmails = previewPendingEmails
                    )
                ),
                onBack = {},
                onStartEvent = {},
                onInvite = {},
                onClearInviteState = {},
                onRemoveParticipant = {},
                onCancelInvite = {},
                onCancelEmailInvite = {},
                onErrorShown = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Participant — Planning")
@Composable
private fun EventDetailsParticipantPreTripPreview() {
    MVPTheme {
        Surface {
            EventDetailsContent(
                uiState = EventDetailsUiState(
                    currentUserId = "2",
                    isLoading = false,
                    participants = previewParticipants,
                    pendingParticipants = previewPendingParticipants,
                    event = Event(
                        id = "1", title = "Summer Trip 2025",
                        status = EventStatus.PRE_TRIP,
                        startDate = 1_750_000_000_000L, endDate = 1_750_500_000_000L,
                        locationLabel = "Beach house, Ibiza",
                        adminId = "1",
                        participants = previewParticipants.map { it.id },
                        pendingParticipants = previewPendingParticipants.map { it.id },
                        pendingEmails = previewPendingEmails
                    )
                ),
                onBack = {},
                onStartEvent = {},
                onInvite = {},
                onClearInviteState = {},
                onRemoveParticipant = {},
                onCancelInvite = {},
                onCancelEmailInvite = {},
                onErrorShown = {}
            )
        }
    }
}