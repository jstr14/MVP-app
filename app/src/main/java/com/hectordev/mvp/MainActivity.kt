package com.hectordev.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hectordev.mvp.ui.navigation.AppDestination
import com.hectordev.mvp.ui.screens.HomeScreen
import com.hectordev.mvp.ui.screens.LoginScreen
import com.hectordev.mvp.ui.screens.SplashScreen
import com.hectordev.mvp.ui.theme.MVPTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MVPTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Splash
                    ) {
                        composable<AppDestination.Splash> {
                            SplashScreen(onNavigationRequested = {
                                // For now, we simulate navigating straight to Login
                                navController.navigate(AppDestination.Login) {
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
            }
        }
    }
}