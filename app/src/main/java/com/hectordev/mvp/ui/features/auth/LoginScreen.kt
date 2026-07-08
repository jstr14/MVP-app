package com.hectordev.mvp.ui.features.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hectordev.mvp.R
import com.hectordev.mvp.ui.core.theme.MVPTheme
import com.hectordev.mvp.ui.features.auth.components.GoogleSignInButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Obtain the Android context from the composition local tree
    val context = LocalContext.current

    // Collect the UI state reactively from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Handle authentication side-effects Reactively
    LaunchedEffect(uiState.isSuccess, uiState.errorMessage) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetState()
        }
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clean and iconic welcome header using localized string resources
        Text(
            text = stringResource(id = R.string.login_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.login_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Custom feature-scoped Google Sign-In button connected to the flow
        GoogleSignInButton(
            onClick = {
                // Credential Manager requires an explicit Activity Context to draw its bottom sheet
                (context as? Activity)?.let { activityContext ->
                    viewModel.signInWithGoogle(activityContext)
                }
            },
            isLoading = uiState.isLoading
        )
    }
}

// --- SCREEN PREVIEW ---

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MVPTheme {
        Surface {
            // Static instantiation bypasses Hilt ViewModel requirements inside the preview panel
            LoginScreen(onLoginSuccess = {})
        }
    }
}