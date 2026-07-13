package com.hectordev.mvp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.ui.features.auth.AuthViewModel
import com.hectordev.mvp.ui.features.auth.LoginScreen
import com.hectordev.mvp.ui.features.auth.SplashScreen
import com.hectordev.mvp.ui.features.event.CreateEventScreen
import com.hectordev.mvp.ui.features.event.EventDetailsScreen
import com.hectordev.mvp.ui.features.feed.LiveFeedScreen
import com.hectordev.mvp.ui.features.home.HomeScreen

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash,
        modifier = modifier
    ) {
        composable<AppDestination.Splash> {
            SplashScreen(onNavigationRequested = {
                val target = if (isUserLoggedIn) AppDestination.Home else AppDestination.Login
                navController.navigate(target) {
                    popUpTo(AppDestination.Splash) { inclusive = true }
                }
            })
        }

        composable<AppDestination.Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(AppDestination.Home) {
                    popUpTo(AppDestination.Login) { inclusive = true }
                }
            })
        }

        composable<AppDestination.Home> { backStackEntry ->
            val eventCreated by backStackEntry.savedStateHandle
                .getStateFlow("event_created", false)
                .collectAsStateWithLifecycle()

            HomeScreen(
                showEventCreatedMessage = eventCreated,
                onEventCreatedMessageShown = {
                    backStackEntry.savedStateHandle["event_created"] = false
                },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(AppDestination.Login) {
                        popUpTo(AppDestination.Home) { inclusive = true }
                    }
                },
                onCreateEvent = {
                    navController.navigate(AppDestination.CreateEvent)
                },
                onEventClick = { eventId, status ->
                    if (status == EventStatus.ON_GOING) {
                        navController.navigate(AppDestination.LiveFeed(eventId))
                    } else {
                        navController.navigate(AppDestination.EventDetails(eventId))
                    }
                }
            )
        }

        composable<AppDestination.EventDetails> { backStackEntry ->
            val dest = backStackEntry.toRoute<AppDestination.EventDetails>()
            EventDetailsScreen(
                onBack = { navController.popBackStack() },
                onGoToLiveFeed = {
                    navController.navigate(AppDestination.LiveFeed(dest.eventId)) {
                        popUpTo(AppDestination.EventDetails(dest.eventId)) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestination.LiveFeed> { backStackEntry ->
            val dest = backStackEntry.toRoute<AppDestination.LiveFeed>()
            LiveFeedScreen(
                onBack = { navController.popBackStack() },
                onGoToGraph = { /* TODO: navigate to graph screen */ },
                onGoToDetails = { navController.navigate(AppDestination.EventDetails(dest.eventId)) }
            )
        }

        composable<AppDestination.CreateEvent> {
            CreateEventScreen(
                onEventCreated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("event_created", true)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}