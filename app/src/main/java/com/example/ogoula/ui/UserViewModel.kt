package com.example.ogoula.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.StorageRepository
import com.example.ogoula.data.UserRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val alias: String = "",
    @SerialName("profile_image_url") val profileImageUri: String? = null,
    @SerialName("banner_image_url") val bannerImageUri: String? = null
)

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val storageRepository = StorageRepository()
    private val prefs = application.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    var userProfile by mutableStateOf(UserProfile())
        private set

    var isUploading by mutableStateOf(false)
        private set

    var uploadError by mutableStateOf<String?>(null)
        private set

    // true quand loadProfile() a terminé (succès ou erreur)
    var isProfileLoaded by mutableStateOf(false)
        private set

    // true si l'utilisateur a déjà un profil (firstName non vide)
    var hasProfile by mutableStateOf(false)
        private set

    fun clearUploadError() { uploadError = null }

    fun resetProfile() {
        userProfile = UserProfile()
        isProfileLoaded = false
        hasProfile = false
    }

    private fun saveImagesLocally(profileUrl: String?, bannerUrl: String?) {
        prefs.edit()
            .putString("profile_image_url", profileUrl)
            .putString("banner_image_url", bannerUrl)
            .apply()
    }

    private fun loadImagesLocally(): Pair<String?, String?> {
        return Pair(
            prefs.getString("profile_image_url", null),
            prefs.getString("banner_image_url", null)
        )
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = userRepository.getProfile(userId)
                if (profile != null && profile.firstName.isNotEmpty()) {
                    // Si Supabase n'a pas les URLs d'images, utiliser le cache local
                    val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                    userProfile = profile.copy(
                        profileImageUri = profile.profileImageUri?.takeIf { it.isNotEmpty() } ?: localProfileUrl,
                        bannerImageUri = profile.bannerImageUri?.takeIf { it.isNotEmpty() } ?: localBannerUrl
                    )
                    hasProfile = true
                } else {
                    val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                    userProfile = UserProfile(
                        userId = userId,
                        profileImageUri = localProfileUrl,
                        bannerImageUri = localBannerUrl
                    )
                    hasProfile = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                userProfile = UserProfile(
                    userId = userId,
                    profileImageUri = localProfileUrl,
                    bannerImageUri = localBannerUrl
                )
                hasProfile = false
            } finally {
                isProfileLoaded = true
            }
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        alias: String,
        profileUri: Uri?,
        bannerUri: Uri?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val userId = userProfile.userId
            if (userId.isEmpty()) {
                uploadError = "Utilisateur non connecté."
                onDone()
                return@launch
            }

            isUploading = true
            uploadError = null

            try {
                val context = getApplication<Application>()
                var profileUrl = userProfile.profileImageUri
                var bannerUrl = userProfile.bannerImageUri

                profileUri?.let { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            profileUrl = storageRepository.uploadProfileImage(userId, bytes)
                        } else {
                            uploadError = "Impossible de lire l'image sélectionnée."
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UserViewModel", "Profile upload error: ${e.message}", e)
                        uploadError = "Photo : ${e.message}"
                    }
                }

                bannerUri?.let { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            bannerUrl = storageRepository.uploadBannerImage(userId, bytes)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UserViewModel", "Banner upload error: ${e.message}", e)
                        if (uploadError == null) uploadError = "Bannière : ${e.message}"
                    }
                }

                val newProfile = UserProfile(
                    userId = userId,
                    firstName = firstName,
                    lastName = lastName,
                    alias = alias,
                    profileImageUri = profileUrl,
                    bannerImageUri = bannerUrl
                )
                userProfile = newProfile
                // Sauvegarde locale des URLs d'images (survit aux redémarrages même si Supabase est lent)
                saveImagesLocally(profileUrl, bannerUrl)
                userRepository.saveProfile(userId, newProfile)
            } catch (e: Exception) {
                e.printStackTrace()
                uploadError = "Erreur inattendue : ${e.localizedMessage}"
            } finally {
                isUploading = false
                onDone()
            }
        }
    }
}
