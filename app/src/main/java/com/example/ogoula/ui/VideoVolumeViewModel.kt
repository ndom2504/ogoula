package com.example.ogoula.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Toggle global muet/son partagé par toutes les vidéos du fil.
 * Un clic mute ou démute toutes les vidéos simultanément (comportement Facebook).
 */
class VideoVolumeViewModel : ViewModel() {

    var isMuted by mutableStateOf(true)
        private set

    fun toggleMute() {
        isMuted = !isMuted
    }
}
