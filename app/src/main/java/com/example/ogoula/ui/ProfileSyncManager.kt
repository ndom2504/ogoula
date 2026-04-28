package com.example.ogoula.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import com.example.ogoula.data.SupabaseIdentity
import com.example.ogoula.data.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.ogoula.ui.PostViewModel

/**
 * Gestionnaire unifié pour la synchronisation du profil entre:
 * - Formulaire d'inscription
 * - Page "Ma Bulle" 
 * - Paramètres de profil
 * - Affichage public depuis les bulles
 */
class ProfileSyncManager(
    private val userViewModel: UserViewModel,
    private val userRepository: UserRepository,
    private val context: Context,
    private val postViewModel: PostViewModel? = null
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("profile_sync", Context.MODE_PRIVATE)
    
    /**
     * Synchronisation complète du profil - garantit la cohérence partout
     */
    fun syncProfileComplete(
        firstName: String = userViewModel.userProfile.firstName,
        lastName: String = userViewModel.userProfile.lastName,
        alias: String = userViewModel.userProfile.alias,
        profileUri: android.net.Uri? = null,
        bannerUri: android.net.Uri? = null,
        culturalReferenceCountry: String? = userViewModel.userProfile.culturalReferenceCountry,
        culturalIntentionsCsv: String? = userViewModel.userProfile.culturalIntentions,
        selfRole: String? = userViewModel.userProfile.selfRole,
        contributionSentence: String? = userViewModel.userProfile.contributionSentence,
        culturalVisibilityUpdate: String? = userViewModel.userProfile.culturalProfileVisibility,
        signProContributionCharter: Boolean = false,
        onSyncComplete: (success: Boolean, error: String?) -> Unit = { _, _ -> }
    ) {
        userViewModel.viewModelScope.launch {
            try {
                // 1. Mettre à jour le profil via UserViewModel (garantit la synchronisation locale + serveur)
                userViewModel.updateProfile(
                    firstName = firstName,
                    lastName = lastName,
                    alias = alias,
                    profileUri = profileUri,
                    bannerUri = bannerUri,
                    culturalReferenceCountry = culturalReferenceCountry,
                    culturalIntentionsCsv = culturalIntentionsCsv,
                    selfRole = selfRole,
                    contributionSentence = contributionSentence,
                    culturalVisibilityUpdate = culturalVisibilityUpdate,
                    signProContributionCharter = signProContributionCharter,
                    onDone = {
                        val success = userViewModel.uploadError == null
                        onSyncComplete(success, userViewModel.uploadError)
                        
                        // 2. Forcer le rechargement du profil pour garantir la synchronisation
                        if (success) {
                            val userId = userViewModel.userProfile.userId
                            if (userId.isNotEmpty()) {
                                userViewModel.loadProfile(userId)
                            }
                        }
                    }
                )
                
                // 3. Marquer la synchronisation comme réussie localement
                prefs.edit()
                    .putLong("last_sync_timestamp", System.currentTimeMillis())
                    .putString("last_sync_alias", alias)
                    .apply()
                    
            } catch (e: Exception) {
                onSyncComplete(false, "Erreur de synchronisation: ${e.message}")
            }
        }
    }
    
    /**
     * Vérifie si le profil a besoin d'être resynchronisé
     */
    fun needsResync(): Boolean {
        val lastSync = prefs.getLong("last_sync_timestamp", 0)
        val lastAlias = prefs.getString("last_sync_alias", "") ?: ""
        val currentAlias = userViewModel.userProfile.alias
        
        // Resync si: jamais synchronisé ou alias changé ou plus de 24h
        val timeDiff = System.currentTimeMillis() - lastSync
        return lastSync == 0L || lastAlias != currentAlias || timeDiff > 24 * 60 * 60 * 1000L
    }
    
    /**
     * Force la resynchronisation complète du profil
     */
    fun forceResync(onComplete: (success: Boolean) -> Unit) {
        val currentProfile = userViewModel.userProfile
        syncProfileComplete(
            firstName = currentProfile.firstName,
            lastName = currentProfile.lastName,
            alias = currentProfile.alias,
            culturalReferenceCountry = currentProfile.culturalReferenceCountry,
            culturalIntentionsCsv = currentProfile.culturalIntentions,
            selfRole = currentProfile.selfRole,
            contributionSentence = currentProfile.contributionSentence,
            culturalVisibilityUpdate = currentProfile.culturalProfileVisibility,
            onSyncComplete = { success, _ -> onComplete(success) }
        )
    }
    
    /**
     * Récupère les statistiques d'engagement du profil
     */
    suspend fun getProfileEngagementStats(userId: String): ProfileEngagementStats? {
        return try {
            // TODO: Implémenter la récupération des stats depuis Supabase
            // Pour l'instant, retourner des stats fictives
            ProfileEngagementStats(
                postsCount = 0,
                followersCount = 0,
                followingCount = 0,
                likesCount = 0,
                commentsCount = 0
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Génère automatiquement un alias basé sur les premières lettres du nom + numérotation
     */
    suspend fun generateAutoAlias(
        firstName: String,
        lastName: String,
        onAliasGenerated: (String) -> Unit
    ) {
        userViewModel.viewModelScope.launch {
            try {
                // Récupérer tous les alias existants depuis Supabase
                val existingAliases = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // TODO: Implémenter la récupération des alias depuis Supabase
                    // Pour l'instant, utilisons une liste vide
                    emptyList<String>()
                }
                
                val newAlias = AliasGenerator.generateAlias(firstName, lastName, existingAliases)
                onAliasGenerated(newAlias)
            } catch (e: Exception) {
                android.util.Log.e("ProfileSyncManager", "Erreur génération alias", e)
                // En cas d'erreur, générer un alias de secours
                val fallbackAlias = "${firstName.take(2)}${lastName.take(2)}${System.currentTimeMillis() % 10000}"
                onAliasGenerated(fallbackAlias.uppercase())
            }
        }
    }
    
    /**
     * Synchronisation automatique complète - met à jour TOUTE l'application
     * Appelé automatiquement quand le profil est modifié dans "Ma Bulle"
     */
    fun syncProfileAutomatic(
        onSyncComplete: (success: Boolean, error: String?) -> Unit = { _, _ -> }
    ) {
        userViewModel.viewModelScope.launch {
            try {
                val currentProfile = userViewModel.userProfile
                
                // 1. Mettre à jour le profil dans la base de données
                userViewModel.updateProfile(
                    firstName = currentProfile.firstName,
                    lastName = currentProfile.lastName,
                    alias = currentProfile.alias,
                    profileUri = null, // Garder les images existantes
                    bannerUri = null,
                    culturalReferenceCountry = currentProfile.culturalReferenceCountry,
                    culturalIntentionsCsv = currentProfile.culturalIntentions,
                    selfRole = currentProfile.selfRole,
                    contributionSentence = currentProfile.contributionSentence,
                    culturalVisibilityUpdate = currentProfile.culturalProfileVisibility
                )
                
                // 2. Mettre à jour toutes les publications de l'utilisateur
                postViewModel?.let { postVM ->
                    withContext(Dispatchers.IO) {
                        // Mettre à jour les posts existants avec les nouvelles infos de profil
                        val currentPosts = postVM.posts.value
                        currentPosts.forEach { post ->
                            if (post.handle == currentProfile.alias) {
                                // Mettre à jour le nom, alias et image de profil dans le post
                                postVM.updatePostProfileInfo(
                                    postId = post.id,
                                    newAuthorName = "${currentProfile.firstName} ${currentProfile.lastName}".trim(),
                                    newAlias = currentProfile.alias,
                                    newProfileImage = currentProfile.profileImageUri
                                )
                            }
                        }
                    }
                }
                
                // 3. Forcer le rafraîchissement des données dans toute l'application
                withContext(Dispatchers.Main) {
                    // Rafraîchir les posts
                    postViewModel?.refreshPosts()
                    
                    // Recharger le profil depuis la base de données
                    userViewModel.loadProfile(userViewModel.userProfile.userId)
                    
                    android.util.Log.d("ProfileSyncManager", "Profil synchronisé et rafraîchi")
                }
                
                android.util.Log.d("ProfileSyncManager", "Synchronisation automatique réussie pour ${currentProfile.alias}")
                onSyncComplete(true, null)
                
            } catch (e: Exception) {
                android.util.Log.e("ProfileSyncManager", "Erreur synchronisation automatique", e)
                onSyncComplete(false, "Erreur lors de la synchronisation: ${e.message}")
            }
        }
    }
    
    /**
     * Vérifie si la synchronisation est nécessaire et la déclenche si besoin
     */
    fun checkAndSyncIfNeeded() {
        val lastSyncTime = prefs.getLong("last_sync_time", 0)
        val currentTime = System.currentTimeMillis()
        val syncInterval = 5 * 60 * 1000 // 5 minutes
        
        if (currentTime - lastSyncTime > syncInterval) {
            syncProfileAutomatic { success, _ ->
                if (success) {
                    prefs.edit().putLong("last_sync_time", currentTime).apply()
                }
            }
        }
    }
}

/**
 * Statistiques d'engagement du profil
 */
data class ProfileEngagementStats(
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val likesCount: Int,
    val commentsCount: Int
)
