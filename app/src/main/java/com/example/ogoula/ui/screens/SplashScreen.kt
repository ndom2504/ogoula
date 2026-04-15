package com.example.ogoula.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(onVideoFinished: () -> Unit) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            // Utilisation d'un identifiant dynamique pour contourner l'erreur de compilation R.raw
            val resId = context.resources.getIdentifier("intro", "raw", context.packageName)
            if (resId != 0) {
                val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Inclure onVideoFinished : sinon la session Auth peut être prête mais le callback reste celui du 1er frame (→ login à tort).
    DisposableEffect(player, onVideoFinished) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoFinished()
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                // Fichier vidéo absent ou invalide (ex: intro.txt) → on passe directement
                onVideoFinished()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(onVideoFinished) {
        // Sécurité : on passe à l'écran suivant après 6 secondes même si la vidéo bug
        delay(6000)
        onVideoFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

    }
}
