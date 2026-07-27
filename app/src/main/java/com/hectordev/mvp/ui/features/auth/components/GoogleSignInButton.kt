package com.hectordev.mvp.ui.features.auth.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hectordev.mvp.R
import com.hectordev.mvp.ui.core.theme.MVPTheme

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White,
            disabledContentColor = Color(0xFF1F1F1F)
        ),
        border = BorderStroke(1.dp, Color(0xFF747775)),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF1F1F1F),
                    strokeWidth = 2.dp
                )
            } else {
                // A stylized bold text "G" completely avoids Material 3 icon import bugs
                Text(
                    text = "G",
                    color = Color(0xFF4285F4), // Official Google Blue
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.login_google_btn_text),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// --- DESIGN SYSTEM PREVIEWS ---

@Preview(name = "Normal State", showBackground = true)
@Composable
private fun GoogleButtonNormalPreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            GoogleSignInButton(onClick = {})
        }
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun GoogleButtonLoadingPreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            GoogleSignInButton(onClick = {}, isLoading = true)
        }
    }
}

@Preview(name = "Loading State — Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoogleButtonLoadingDarkPreview() {
    MVPTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            GoogleSignInButton(onClick = {}, isLoading = true)
        }
    }
}