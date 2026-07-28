package com.hectordev.mvp.ui.features.gala

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.ui.core.components.FullScreenPhotoViewer

@Composable
internal fun GalaPhotoSection(photoUrl: String, context: Context) {
    var showFullScreen by remember { mutableStateOf(false) }

    if (showFullScreen) {
        FullScreenPhotoViewer(url = photoUrl, onDismiss = { showFullScreen = false })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.gala_photo_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showFullScreen = true }
        )
        OutlinedButton(
            onClick = { downloadPhoto(context, photoUrl) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.gala_download_photo_btn))
        }
    }
}

@Composable
internal fun GalaActions(
    isAdmin: Boolean,
    isWinner: Boolean,
    isUploadingPhoto: Boolean,
    hasPhoto: Boolean,
    isGeneratingCertificate: Boolean = false,
    onTakePhoto: () -> Unit,
    onDownloadCertificate: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isAdmin) {
            Button(
                onClick = onTakePhoto,
                enabled = !isUploadingPhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploadingPhoto) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(if (hasPhoto) R.string.gala_replace_photo_btn else R.string.gala_take_photo_btn))
            }
        }
        if (isWinner) {
            OutlinedButton(
                onClick = onDownloadCertificate,
                enabled = !isGeneratingCertificate,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGeneratingCertificate) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.gala_download_certificate_btn))
            }
        }
    }
}

internal fun downloadPhoto(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(context.getString(R.string.gala_photo_download_title))
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "mvp_gala_photo.jpg")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
}