package com.hectordev.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hectordev.mvp.ui.core.theme.MVPTheme
import com.hectordev.mvp.ui.features.auth.AuthViewModel
import com.hectordev.mvp.ui.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MVPTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // One clean line handles all routing
                    AppNavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}