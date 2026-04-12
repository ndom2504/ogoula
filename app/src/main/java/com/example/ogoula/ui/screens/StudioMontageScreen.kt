package com.example.ogoula.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.GreenGabo
import java.io.ByteArrayOutputStream

enum class VideoFilter(val label: String) {
    NORMAL("Normal"),
    VIBRANT("Vibrant"),
    RETRO("Rétro"),
    CINEMA("Cinéma")
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioMontageScreen(onBack: () -> Unit, onPost: (String?, ByteArray?, Uri?) -> Unit) {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFilter by remember { mutableStateOf(VideoFilter.NORMAL) }
    var customTitle by remember { mutableStateOf("") }
    var showTitleInput by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    // Capture automatique du premier frame de la vidéo sélectionnée
    val thumbnailBitmap = remember(selectedVideoUri) {
        selectedVideoUri?.let { uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bmp = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                bmp
            } catch (e: Exception) {
                null
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedVideoUri = uri }
    )

    val player = remember(selectedVideoUri) {
        ExoPlayer.Builder(context).build().apply {
            selectedVideoUri?.let {
                setMediaItem(MediaItem.fromUri(it))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studio Montage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (selectedVideoUri != null) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = GreenGabo
                            )
                        } else {
                            TextButton(onClick = {
                                val uri = selectedVideoUri ?: return@TextButton
                                isPublishing = true
                                // Compresser le thumbnail (rapide) puis passer l'URI vidéo
                                // directement — l'upload se fait en streaming dans PostViewModel
                                val thumbBytes = thumbnailBitmap?.let { bmp ->
                                    val out = ByteArrayOutputStream()
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    out.toByteArray()
                                }
                                onPost(customTitle.ifBlank { null }, thumbBytes, uri)
                            }) {
                                Text("Publier", color = GreenGabo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedVideoUri != null) {
                    AndroidView(
                        factory = {
                            PlayerView(it).apply {
                                this.player = player
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Simulating filters with overlays
                    val overlayColor = when(selectedFilter) {
                        VideoFilter.RETRO -> Color(0x44795548) // Sepia tint
                        VideoFilter.VIBRANT -> Color(0x22FFEB3B).copy(alpha = 0.1f) // Slight warm glow
                        VideoFilter.CINEMA -> Color(0x33000000) // Dimmer
                        VideoFilter.NORMAL -> Color.Transparent
                    }
                    
                    Box(modifier = Modifier.fillMaxSize().background(overlayColor))

                    if (customTitle.isNotEmpty()) {
                        Text(
                            text = customTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 80.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Toucher pour choisir une vidéo", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Exprime-toi, c'est le Bled !", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (selectedVideoUri != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Filtres Gaboma", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VideoFilter.entries) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter.label) },
                                    leadingIcon = if (selectedFilter == filter) {
                                        { Icon(Icons.Default.Movie, modifier = Modifier.size(16.dp), contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TextFields, contentDescription = null, tint = BlueGabo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajouter un titre fun", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(checked = showTitleInput, onCheckedChange = { showTitleInput = it })
                        }
                        
                        if (showTitleInput) {
                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { if (it.length <= 30) customTitle = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                placeholder = { Text("Ex: C'est le ndole !") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                suffix = { Text("${customTitle.length}/30", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}
