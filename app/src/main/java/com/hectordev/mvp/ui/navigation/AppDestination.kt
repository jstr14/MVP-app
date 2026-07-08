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
}