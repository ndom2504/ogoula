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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val alias: String = "",
    @SerialName("profile_image_url") val profileImageUri: String? = null,
    @SerialName("banner_image_url") val bannerImageUri: String? = null,
    @SerialName("account_status") val accountStatus: String? = "active",
    @SerialName("suspended_until") val suspendedUntil: String? = null,
    @SerialName("moderation_note") val moderationNote: String? = null,
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

    /** Affiché sur l’écran de connexion après déconnexion forcée (sanction). */
    var accountDenialMessage by mutableStateOf<String?>(null)
        private set

    /** Ne pas nommer `setAccountDenialMessage` : conflit JVM avec le setter de la propriété. */
    fun putAccountDenialMessage(msg: String) {
        accountDenialMessage = msg
    }

    fun clearAccountDenialMessage() {
        accountDenialMessage = null
    }

    /** null si le compte peut utiliser l’app ; sinon message pour l’utilisateur. */
    fun peekAccountModerationMessage(): String? {
        val p = userProfile
        val st = p.accountStatus?.lowercase(Locale.ROOT) ?: "active"
        if (st == "banned") {
            val n = p.moderationNote?.trim().orEmpty()
            return if (n.isNotEmpty()) {
                "Ce compte a été retiré de la communauté Ogoula pour non-respect de nos valeurs. Motif : $n"
            } else {
                "Ce compte n'est plus autorisé sur Ogoula (charte communautaire)."
            }
        }
        if (st == "suspended") {
            val until = p.suspendedUntil ?: return "Ton compte est temporairement suspendu. Réessaie plus tard ou contacte le support."
            return try {
                val end = Instant.parse(until)
                if (end.isAfter(Instant.now())) {
                    val human = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH)
                        .withZone(ZoneId.systemDefault())
                        .format(end)
                    val note = p.moderationNote?.trim().orEmpty()
                    buildString {
                        append("Compte suspendu jusqu'au $human.")
                        if (note.isNotEmpty()) append(" Motif : $note")
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                "Compte suspendu. Contacte le support si besoin."
            }
        }
        return null
    }

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
                    bannerImageUri = bannerUrl,
                    accountStatus = userProfile.accountStatus,
                    suspendedUntil = userProfile.suspendedUntil,
                    moderationNote = userProfile.moderationNote,
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
