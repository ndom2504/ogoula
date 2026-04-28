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
import com.example.ogoula.data.SupabaseIdentity
import com.example.ogoula.data.UserRepository
import com.example.ogoula.ui.onboarding.PRO_CONTRIBUTION_CHARTER_VERSION
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
    @SerialName("cultural_reference_country") val culturalReferenceCountry: String? = null,
    /** Identifiants d’intention (catalogue app), séparés par des virgules, max 2 à l’inscription. */
    @SerialName("cultural_intentions") val culturalIntentions: String? = null,
    @SerialName("self_role") val selfRole: String? = null,
    @SerialName("contribution_sentence") val contributionSentence: String? = null,
    /**
     * Qui peut voir le bloc engagement sur le profil public : everyone | followers_only | hidden.
     * [followers_only] : visible seulement si le visiteur suit ce profil (liste Suivre de l’app).
     */
    @SerialName("cultural_profile_visibility") val culturalProfileVisibility: String? = "everyone",
    /** Version du cadre pro-contribution acceptée à l’inscription (ex. phases test). */
    @SerialName("pro_contribution_charter_version") val proContributionCharterVersion: String? = null,
    @SerialName("pro_contribution_acknowledged_at") val proContributionAcknowledgedAt: String? = null,
)

/**
 * Ligne `profiles` avec identité minimale : l’utilisateur a terminé l’inscription côté app.
 * C’est ce critère qui évite de renvoyer vers le formulaire après chaque connexion.
 * (La charte peut être complétée ou relue plus tard si la base n’a pas encore les colonnes.)
 */
private fun hasRegisteredIdentity(p: UserProfile): Boolean {
    if (p.firstName.isNotBlank() || p.lastName.isNotBlank() || p.alias.isNotBlank()) return true
    // Ligne présente mais identité texte vide (migration, ancien flux) : engagement déjà renseigné.
    val culturalDone =
        !p.culturalReferenceCountry.isNullOrBlank() &&
            (
                !p.culturalIntentions.isNullOrBlank() ||
                    !p.selfRole.isNullOrBlank() ||
                    !p.contributionSentence.isNullOrBlank()
                )
    android.util.Log.d(
        "UserViewModel",
        "hasRegisteredIdentity: firstName='${p.firstName}', lastName='${p.lastName}', alias='${p.alias}', culturalDone=$culturalDone",
    )
    return culturalDone
}

/** Cadre pro-contribution enregistré (traçabilité / admin). */
private fun hasAcceptedCharterFramework(p: UserProfile): Boolean =
    p.proContributionCharterVersion == PRO_CONTRIBUTION_CHARTER_VERSION ||
        !p.proContributionAcknowledgedAt.isNullOrBlank()

/** Aligné sur handlesEqual (PostComponents) : comparaison @alias insensible à la casse. */
private fun sameProfileAlias(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    fun norm(s: String) = s.trim().lowercase(Locale.ROOT).removePrefix("@").let { "@$it" }
    return norm(a) == norm(b)
}

/** Aligné sur handlesEqual : stockage stable en base et recherche par alias. */
private fun normalizedAliasForStorage(alias: String): String {
    val t = alias.trim()
    if (t.isEmpty()) return ""
    val core = t.removePrefix("@").lowercase(Locale.ROOT)
    return "@$core"
}

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val storageRepository = StorageRepository()
    private val prefs = application.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    var userProfile by mutableStateOf(UserProfile())
        private set

    /** Profil chargé pour affichage public (autre utilisateur). */
    var publicProfile by mutableStateOf<UserProfile?>(null)
        private set
    var publicProfileLoading by mutableStateOf(false)
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

    fun clearPublicProfile() {
        publicProfile = null
        publicProfileLoading = false
    }

    fun loadPublicProfileByAlias(alias: String) {
        val normalized = alias.trim().replace("\u200B", "").trim()
        if (normalized.isBlank()) return
        // Même compte que la session : pas besoin du SELECT distant (souvent bloqué par RLS pour « sa » ligne via alias, ou alias identique au fil).
        if (userProfile.alias.isNotBlank() && sameProfileAlias(normalized, userProfile.alias)) {
            publicProfileLoading = true
            publicProfile = null
            viewModelScope.launch {
                publicProfile = userProfile
                publicProfileLoading = false
            }
            return
        }
        publicProfileLoading = true
        publicProfile = null
        viewModelScope.launch {
            try {
                var row = userRepository.getProfileByAlias(normalized)
                if (row == null) {
                    val t = normalized
                    val uuidRegex =
                        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                    if (uuidRegex.matches(t)) {
                        row = userRepository.getProfile(t)
                    }
                }
                publicProfile = row
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "loadPublicProfileByAlias", e)
                publicProfile = null
            } finally {
                publicProfileLoading = false
            }
        }
    }

    fun saveCulturalProfileVisibility(visibility: String) {
        val uid = userProfile.userId
        if (uid.isEmpty()) return
        viewModelScope.launch {
            val p = userProfile.copy(culturalProfileVisibility = visibility)
            userProfile = p
            userRepository.saveProfile(uid, p)
        }
    }

    fun resetProfile() {
        userProfile = UserProfile()
        isProfileLoaded = false
        hasProfile = false
        // Ne pas effacer les prefs : au même compte qui se reconnecte, réhydration si SELECT échoue encore.
        // Changer de compte est géré par clearLocalProfileCacheForDifferentUser() dans loadProfile().
    }

    private fun saveImagesLocally(profileUrl: String?, bannerUrl: String?) {
        prefs.edit()
            .putString("profile_image_url", profileUrl)
            .putString("banner_image_url", bannerUrl)
            .apply()
    }

    private fun saveProfileLocally(profile: UserProfile) {
        prefs.edit().apply {
            putString("profile_first_name", profile.firstName)
            putString("profile_last_name", profile.lastName)
            putString("profile_alias", profile.alias)
            putString("profile_cultural_country", profile.culturalReferenceCountry)
            putString("profile_cultural_intentions", profile.culturalIntentions)
            putString("profile_self_role", profile.selfRole)
            putString("profile_contribution_sentence", profile.contributionSentence)
        }.apply()
    }

    private fun loadProfileLocally(): UserProfile {
        return UserProfile().copy(
            firstName = prefs.getString("profile_first_name", "") ?: "",
            lastName = prefs.getString("profile_last_name", "") ?: "",
            alias = prefs.getString("profile_alias", "") ?: "",
            culturalReferenceCountry = prefs.getString("profile_cultural_country", "") ?: "",
            culturalIntentions = prefs.getString("profile_cultural_intentions", "") ?: "",
            selfRole = prefs.getString("profile_self_role", "") ?: "",
            contributionSentence = prefs.getString("profile_contribution_sentence", "") ?: ""
        )
    }

    /** Vérification rapide si l'utilisateur a déjà rempli le formulaire (contournement) */
    fun hasCompletedOnboarding(): Boolean {
        val firstName = prefs.getString("profile_first_name", "") ?: ""
        val lastName = prefs.getString("profile_last_name", "") ?: ""
        val alias = prefs.getString("profile_alias", "") ?: ""
        val country = prefs.getString("profile_cultural_country", "") ?: ""
        val intentions = prefs.getString("profile_cultural_intentions", "") ?: ""
        val role = prefs.getString("profile_self_role", "") ?: ""
        val sentence = prefs.getString("profile_contribution_sentence", "") ?: ""
        if (firstName.isNotBlank() || lastName.isNotBlank() || alias.isNotBlank()) return true
        val culturalDone =
            country.isNotBlank() &&
                (intentions.isNotBlank() || role.isNotBlank() || sentence.isNotBlank())
        android.util.Log.d(
            "UserViewModel",
            "hasCompletedOnboarding: identity=${firstName.isNotBlank() || lastName.isNotBlank() || alias.isNotBlank()} culturalDone=$culturalDone",
        )
        return culturalDone
    }

    /** Forcer hasProfile à true (contournement temporaire) */
    fun forceSetHasProfile(value: Boolean) {
        android.util.Log.d("UserViewModel", "🔧 forceSetHasProfile: forcing hasProfile to $value")
        hasProfile = value
    }

    /** Vérification radicale : est-ce que cet utilisateur existe déjà dans Supabase ? */
    suspend fun checkUserExistsInSupabase(userId: String): Boolean {
        return try {
            val profile = userRepository.getProfile(userId)
            val exists = profile != null
            android.util.Log.d("UserViewModel", "🔍 checkUserExistsInSupabase: userId=$userId, exists=$exists")
            if (exists && profile != null) {
                android.util.Log.d("UserViewModel", "📊 Profile data: firstName='${profile.firstName}', lastName='${profile.lastName}', alias='${profile.alias}'")
            }
            exists
        } catch (e: Exception) {
            android.util.Log.e("UserViewModel", "❌ checkUserExistsInSupabase error: ${e.message}", e)
            false
        }
    }

    /** Met à jour le cache seulement pour les URLs non vides (ne supprime pas une image déjà en cache). */
    private fun persistImageUrlsIfNonBlank(profileUrl: String?, bannerUrl: String?) {
        prefs.edit().apply {
            profileUrl?.takeIf { it.isNotBlank() }?.let { putString("profile_image_url", it) }
            bannerUrl?.takeIf { it.isNotBlank() }?.let { putString("banner_image_url", it) }
        }.apply()
    }

    private fun loadImagesLocally(): Pair<String?, String?> {
        return Pair(
            prefs.getString("profile_image_url", null),
            prefs.getString("banner_image_url", null)
        )
    }

    /** Efface le cache SharedPreferences du profil (texte + images) quand l’utilisateur connecté change. */
    private fun clearLocalProfileCacheForDifferentUser() {
        prefs.edit()
            .remove("profile_image_url")
            .remove("banner_image_url")
            .remove("profile_first_name")
            .remove("profile_last_name")
            .remove("profile_alias")
            .remove("profile_cultural_country")
            .remove("profile_cultural_intentions")
            .remove("profile_self_role")
            .remove("profile_contribution_sentence")
            .apply()
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            isProfileLoaded = false
            val cachedPrefsUid = prefs.getString("profile_prefs_user_id", null)
            if (cachedPrefsUid != null && cachedPrefsUid != userId) {
                clearLocalProfileCacheForDifferentUser()
            }
            try {
                val profile = userRepository.getProfile(userId)
                android.util.Log.d("UserViewModel", "📊 Profile from Supabase: ${profile != null}")
                if (profile != null) {
                    val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                    val merged = profile.copy(
                        profileImageUri = profile.profileImageUri?.takeIf { it.isNotEmpty() } ?: localProfileUrl,
                        bannerImageUri = profile.bannerImageUri?.takeIf { it.isNotEmpty() } ?: localBannerUrl
                    )
                    userProfile = merged
                    // Conserver en local les URLs renvoyées par Supabase après chaque auth (sans effacer si une URL manque)
                    persistImageUrlsIfNonBlank(merged.profileImageUri, merged.bannerImageUri)
                    // Aligner prénom / nom / alias / engagement sur le disque : évite « Ma bulle » vide après redémarrage si SELECT a réussi une fois.
                    saveProfileLocally(merged)
                    prefs.edit().putString("profile_prefs_user_id", userId).apply()
                    val registered = hasRegisteredIdentity(merged)
                    hasProfile = registered
                    android.util.Log.d(
                        "UserViewModel",
                        "✅ Profile loaded from Supabase, hasProfile=$registered",
                    )
                } else {
                    val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                    val localProfileData = loadProfileLocally()
                    userProfile = UserProfile(
                        userId = userId,
                        profileImageUri = localProfileUrl,
                        bannerImageUri = localBannerUrl,
                        firstName = localProfileData.firstName,
                        lastName = localProfileData.lastName,
                        alias = localProfileData.alias,
                        culturalReferenceCountry = localProfileData.culturalReferenceCountry,
                        culturalIntentions = localProfileData.culturalIntentions,
                        selfRole = localProfileData.selfRole,
                        contributionSentence = localProfileData.contributionSentence
                    )
                    android.util.Log.d("UserViewModel", "📱 Loading local profile data: firstName='${localProfileData.firstName}', lastName='${localProfileData.lastName}', alias='${localProfileData.alias}'")
                    prefs.edit().putString("profile_prefs_user_id", userId).apply()
                    // Vérifier si le profil a des données locales (formulaire déjà rempli)
                    hasProfile = hasRegisteredIdentity(userProfile)
                    android.util.Log.d("UserViewModel", "🔍 Local profile loaded, hasProfile=$hasProfile")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val (localProfileUrl, localBannerUrl) = loadImagesLocally()
                val localProfileData = loadProfileLocally()
                userProfile = UserProfile(
                    userId = userId,
                    profileImageUri = localProfileUrl,
                    bannerImageUri = localBannerUrl,
                    firstName = localProfileData.firstName,
                    lastName = localProfileData.lastName,
                    alias = localProfileData.alias,
                    culturalReferenceCountry = localProfileData.culturalReferenceCountry,
                    culturalIntentions = localProfileData.culturalIntentions,
                    selfRole = localProfileData.selfRole,
                    contributionSentence = localProfileData.contributionSentence
                )
                prefs.edit().putString("profile_prefs_user_id", userId).apply()
                // Vérifier si le profil a des données locales même en cas d'erreur
                hasProfile = hasRegisteredIdentity(userProfile)
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
        culturalReferenceCountry: String? = null,
        culturalIntentionsCsv: String? = null,
        selfRole: String? = null,
        contributionSentence: String? = null,
        culturalVisibilityUpdate: String? = null,
        signProContributionCharter: Boolean = false,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val userId = userProfile.userId.ifBlank {
                SupabaseIdentity.sessionUserIdOrNull().orEmpty()
            }
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

                val aliasStored = normalizedAliasForStorage(alias)
                val newProfile = UserProfile(
                    userId = SupabaseIdentity.canonicalUserId(userId),
                    firstName = firstName,
                    lastName = lastName,
                    alias = aliasStored,
                    profileImageUri = profileUrl,
                    bannerImageUri = bannerUrl,
                    accountStatus = userProfile.accountStatus,
                    suspendedUntil = userProfile.suspendedUntil,
                    moderationNote = userProfile.moderationNote,
                    culturalReferenceCountry = culturalReferenceCountry ?: userProfile.culturalReferenceCountry,
                    culturalIntentions = culturalIntentionsCsv ?: userProfile.culturalIntentions,
                    selfRole = selfRole ?: userProfile.selfRole,
                    contributionSentence = contributionSentence ?: userProfile.contributionSentence,
                    culturalProfileVisibility = culturalVisibilityUpdate
                        ?: userProfile.culturalProfileVisibility
                        ?: "everyone",
                    proContributionCharterVersion = if (signProContributionCharter) {
                        PRO_CONTRIBUTION_CHARTER_VERSION
                    } else {
                        userProfile.proContributionCharterVersion
                    },
                    proContributionAcknowledgedAt = if (signProContributionCharter) {
                        Instant.now().toString()
                    } else {
                        userProfile.proContributionAcknowledgedAt
                    },
                )
                userProfile = newProfile
                saveImagesLocally(profileUrl, bannerUrl)
                // Sauvegarder les informations du profil localement pour la persistance
                saveProfileLocally(newProfile)
                val saved = userRepository.saveProfile(userId, newProfile)
                if (!saved) {
                    uploadError =
                        "Impossible d'enregistrer le profil sur le serveur. Vérifie la connexion et les politiques Supabase (RLS : insert + update + select sur ta ligne)."
                    // ❌ NE PAS remettre hasProfile à false pour éviter la perte de données
                    // Garder le profil localement même si la sauvegarde échoue
                    userProfile = newProfile
                    hasProfile = hasRegisteredIdentity(newProfile)
                } else {
                    val fromServer = userRepository.getProfile(userId)
                    if (fromServer != null && hasRegisteredIdentity(fromServer)) {
                        val merged = fromServer.copy(
                            profileImageUri = fromServer.profileImageUri?.takeIf { it.isNotEmpty() }
                                ?: newProfile.profileImageUri,
                            bannerImageUri = fromServer.bannerImageUri?.takeIf { it.isNotEmpty() }
                                ?: newProfile.bannerImageUri,
                        )
                        userProfile = merged
                        hasProfile = hasRegisteredIdentity(merged)
                        persistImageUrlsIfNonBlank(merged.profileImageUri, merged.bannerImageUri)
                        saveImagesLocally(merged.profileImageUri, merged.bannerImageUri)
                        if (!hasAcceptedCharterFramework(merged)) {
                            android.util.Log.w(
                                "UserViewModel",
                                "Profil lu sans horodatage charte — vérifie les colonnes pro_contribution_* en base.",
                            )
                        }
                    } else {
                        userProfile = newProfile
                        hasProfile = hasRegisteredIdentity(newProfile)
                        uploadError =
                            "Profil non relu sur le serveur après enregistrement. Dans Supabase → SQL, exécute docs/supabase_profiles_rls_select_update.sql (politiques SELECT / UPDATE sur profiles)."
                    }
                }
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
