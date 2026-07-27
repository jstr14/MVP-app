package com.hectordev.mvp.ui.features.feed

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.views.GiphyDialogFragment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.TimelineTier
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.ui.core.components.displayName
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeNoteBottomSheet(
    participants: List<User>,
    isPosting: Boolean,
    postSuccess: Boolean,
    onPost: (TimelineTier, String, String, Uri?, String?) -> Unit,
    onDismiss: () -> Unit,
    onPostSuccessConsumed: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTier by remember { mutableStateOf<TimelineTier?>(null) }
    var selectedTargetId by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGifUrl by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            sheetState.hide()
            onDismiss()
            onPostSuccessConsumed()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) selectedImageUri = cameraUri }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { selectedImageUri = it } }

    val canPost = selectedTier != null &&
            selectedTargetId.isNotEmpty() &&
            (textInput.isNotBlank() || selectedImageUri != null || selectedGifUrl != null) &&
            !isPosting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.live_feed_compose_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tier selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.live_feed_compose_pick_tier),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineTier.entries.take(2).forEach { tier ->
                        TierOption(
                            tier = tier,
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimelineTier.entries.drop(2).forEach { tier ->
                        TierOption(
                            tier = tier,
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Target selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.live_feed_compose_nominate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(participants) { user ->
                        FilterChip(
                            selected = selectedTargetId == user.id,
                            onClick = { selectedTargetId = user.id },
                            label = { Text(user.name.substringBefore(" ")) }
                        )
                    }
                }
            }

            // Text input
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(stringResource(R.string.live_feed_compose_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Media attachment buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val uri = createTempImageUri(context)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        selectedGifUrl = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.live_feed_compose_camera_btn))
                }
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        selectedGifUrl = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.live_feed_compose_gallery_btn))
                }
                OutlinedButton(
                    onClick = {
                        val activity = context as FragmentActivity
                        val settings = GPHSettings().apply {
                            mediaTypeConfig = arrayOf(GPHContentType.gif)
                        }
                        val dialog = GiphyDialogFragment.newInstance(settings)
                        dialog.gifSelectionListener = object : GiphyDialogFragment.GifSelectionListener {
                            override fun onGifSelected(
                                media: Media,
                                searchTerm: String?,
                                selectedContentType: GPHContentType
                            ) {
                                selectedGifUrl = media.images.fixedWidth?.gifUrl
                                    ?: media.images.original?.gifUrl
                                selectedImageUri = null
                            }
                            override fun onDismissed(selectedContentType: GPHContentType) {}
                            override fun didSearchTerm(term: String) {}
                        }
                        dialog.show(activity.supportFragmentManager, "giphy_dialog")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.live_feed_compose_gif_btn))
                }
            }

            // Media preview (photo or GIF)
            val hasMedia = selectedImageUri != null || selectedGifUrl != null
            if (hasMedia) {
                Box {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else if (selectedGifUrl != null) {
                        AsyncImage(
                            model = selectedGifUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    IconButton(
                        onClick = {
                            selectedImageUri = null
                            selectedGifUrl = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.live_feed_compose_remove_photo_accessibility),
                            tint = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(4.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val tier = selectedTier ?: return@Button
                    onPost(tier, selectedTargetId, textInput, selectedImageUri, selectedGifUrl)
                },
                enabled = canPost,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.live_feed_compose_post_btn))
                }
            }
        }
    }
}

@Composable
private fun TierOption(
    tier: TimelineTier,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (tier) {
        TimelineTier.FACT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TimelineTier.HOT_TAKE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TimelineTier.WITNESSED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TimelineTier.LORE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val borderModifier = if (selected) Modifier.border(2.dp, contentColor, RoundedCornerShape(12.dp)) else Modifier

    Surface(
        modifier = modifier.then(borderModifier).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = tier.displayName(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = pluralStringResource(R.plurals.live_feed_tier_points, tier.points, tier.points),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

internal fun createTempImageUri(context: Context): Uri {
    val tmpFile = File.createTempFile(
        "note_photo_${System.currentTimeMillis()}",
        ".jpg",
        File(context.cacheDir, "images").also { it.mkdirs() }
    )
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
}