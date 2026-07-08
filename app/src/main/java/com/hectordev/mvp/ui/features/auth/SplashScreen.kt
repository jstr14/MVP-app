package com.hectordev.mvp.ui.features.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hectordev.mvp.R

@Composable
fun SplashScreen(onNavigationRequested: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.splash_checking_auth))
    }

    LaunchedEffect(Unit) {
        onNavigationRequested()
    }
}