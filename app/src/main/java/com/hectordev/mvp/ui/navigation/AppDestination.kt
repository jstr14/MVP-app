package com.hectordev.mvp.ui.navigation

import kotlinx.serialization.Serializable

// Sealed interface to group all app destinations
sealed interface AppDestination {

    @Serializable
    data object Splash : AppDestination

    @Serializable
    data object Login : AppDestination

    @Serializable
    data object Home : AppDestination

    @Serializable
    data object CreateEvent : AppDestination

    @Serializable
    data class EventDetails(val eventId: String) : AppDestination

    @Serializable
    data class LiveFeed(val eventId: String, val viewerMode: Boolean = false) : AppDestination

    @Serializable
    data class ScoreGraph(val eventId: String) : AppDestination

    @Serializable
    data class Gala(val eventId: String) : AppDestination

    @Serializable
    data object Badges : AppDestination
}