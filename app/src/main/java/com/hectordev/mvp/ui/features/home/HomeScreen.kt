package com.hectordev.mvp.ui.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.ui.core.theme.MVPTheme
import com.hectordev.mvp.ui.features.profile.UserViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    showEventCreatedMessage: Boolean = false,
    onEventCreatedMessageShown: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel()
) {
    val user by viewModel.userState.collectAsState()
    HomeScreenContent(
        userName = user?.name,
        userPhotoUrl = user?.photoUrl,
        showEventCreatedMessage = showEventCreatedMessage,
        onEventCreatedMessageShown = onEventCreatedMessageShown,
        onLogout = onLogout,
        onCreateEvent = onCreateEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    userName: String?,
    userPhotoUrl: String?,
    onLogout: () -> Unit,
    onCreateEvent: () -> Unit,
    showEventCreatedMessage: Boolean = false,
    onEventCreatedMessageShown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val eventCreatedMessage = stringResource(R.string.home_event_created_success)

    LaunchedEffect(showEventCreatedMessage) {
        if (showEventCreatedMessage) {
            snackbarHostState.showSnackbar(eventCreatedMessage)
            onEventCreatedMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${stringResource(R.string.home_welcome_greeting)} ${userName?.substringBefore(" ") ?: ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    Box {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = stringResource(R.string.home_profile_photo_accessibility),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { menuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_menu_sign_out)) },
                                onClick = {
                                    menuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateEvent) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_dashboard_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- SCREEN PREVIEWS ---

@Preview(showBackground = true, name = "Default")
@Composable
private fun HomeScreenPreview() {
    MVPTheme {
        Surface {
            HomeScreenContent(
                userName = "Jack Shepard",
                userPhotoUrl = null,
                onLogout = {},
                onCreateEvent = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Event created snackbar")
@Composable
private fun HomeScreenEventCreatedPreview() {
    MVPTheme {
        Surface {
            HomeScreenContent(
                userName = "Jack Shepard",
                userPhotoUrl = null,
                showEventCreatedMessage = true,
                onEventCreatedMessageShown = {},
                onLogout = {},
                onCreateEvent = {}
            )
        }
    }
}