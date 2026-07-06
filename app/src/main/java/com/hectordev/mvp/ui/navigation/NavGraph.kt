package com.hectordev.mvp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hectordev.mvp.ui.features.auth.AuthViewModel
import com.hectordev.mvp.ui.features.auth.LoginScreen
import com.hectordev.mvp.ui.features.auth.SplashScreen
import com.hectordev.mvp.ui.features.home.HomeScreen

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // We safely read the state inside the NavGraph block
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

        composable<AppDestination.Home> {
            HomeScreen()
        }
    }
}