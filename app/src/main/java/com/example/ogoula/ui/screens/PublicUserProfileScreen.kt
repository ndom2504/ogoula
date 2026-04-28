package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.ogoula.data.FollowRequestRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.ProfileEngagementStats
import com.example.ogoula.ui.onboarding.intentionLabelForId
import com.example.ogoula.ui.onboarding.parseIntentionsCsv
import com.example.ogoula.ui.onboarding.roleLabelForId
import com.example.ogoula.ui.theme.GreenGabo
import java.util.Locale

/** Même logique que UserViewModel.sameProfileAlias : @ et casse. */
private fun normalizedAlias(s: String): String {
    val t = s.trim().lowercase(Locale.ROOT).removePrefix("@")
    return if (t.isEmpty()) "" else "@$t"
}

private fun handlesEqual(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    return normalizedAlias(a) == normalizedAlias(b)
}

private fun viewerFollowsSubject(followedHandles: List<String>, subjectAlias: String): Boolean {
    val subj = normalizedAlias(subjectAlias)
    if (subj.isEmpty()) return false
    return followedHandles.any { normalizedAlias(it) == subj }
}

private fun showCulturalBlock(
    subject: UserProfile,
    viewerAlias: String,
    followedHandles: List<String>,
): Boolean {
    if (handlesEqual(viewerAlias, subject.alias)) return true
    return when (subject.culturalProfileVisibility ?: "everyone") {
        "hidden" -> false
        "followers_only" -> viewerFollowsSubject(followedHandles, subject.alias)
        else -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    userHandle: String,
    userViewModel: UserViewModel,
    followedHandles: List<String>,
    onBack: () -> Unit,
) {
    val viewer = userViewModel.userProfile
    val loading = userViewModel.publicProfileLoading
    val profile = userViewModel.publicProfile
    val engagementStats = remember { mutableStateOf<ProfileEngagementStats?>(null) }
    val statsLoading = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { userViewModel.clearPublicProfile() }
    }

    LaunchedEffect(userHandle) {
        userViewModel.loadPublicProfileByAlias(userHandle)
    }
    
    LaunchedEffect(profile) {
        profile?.let { profileData: UserProfile -> 
            statsLoading.value = true
            // Charger les statistiques d'engagement
            // TODO: Implémenter avec le ProfileSyncManager en utilisant profileData
            android.util.Log.d("PublicUserProfile", "Chargement stats pour ${profileData.alias}")
            statsLoading.value = false
            engagementStats.value = ProfileEngagementStats(
                postsCount = 0, // TODO: Récupérer depuis Supabase
                followersCount = 0, // TODO: Récupérer depuis Supabase  
                followingCount = 0, // TODO: Récupérer depuis Supabase
                likesCount = 0, // TODO: Récupérer depuis Supabase
                commentsCount = 0 // TODO: Récupérer depuis Supabase
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenGabo)
                }
            }
            profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Profil introuvable ou accès restreint.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val isSelf = handlesEqual(viewer.alias, profile.alias)
                val showCultural = showCulturalBlock(profile, viewer.alias, followedHandles)
                val displayName =
                    "${profile.firstName} ${profile.lastName}".trim().ifBlank {
                        profile.alias.ifBlank {
                            if (profile.userId.isNotBlank()) "Membre Ogoula"
                            else "Profil"
                        }
                    }
                val displayHandle = profile.alias.ifBlank { userHandle }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        profile.bannerImageUri?.takeIf { it.isNotBlank() }?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(GreenGabo.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            profile.profileImageUri?.takeIf { it.isNotBlank() }?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Text(
                                text = buildString {
                                    val a = profile.firstName.take(1).uppercase()
                                    val b = profile.lastName.take(1).uppercase()
                                    if (a.isNotEmpty() || b.isNotEmpty()) append(a).append(b)
                                    else append(displayHandle.removePrefix("@").take(2).uppercase(Locale.FRENCH))
                                }.ifEmpty { "?" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                displayHandle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Bouton de suivi avec options
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Bouton principal de suivi
                                IconButton(
                                    onClick = { 
                                        if (!isSelf) {
                                            // Envoyer une demande de suivi
                                            sendFollowRequest(profile, userViewModel)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Suivre",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Suivre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // Menu d'options pour les propres publications
                                if (viewerOwnsPost(profile.alias)) {
                                    IconButton(
                                        onClick = { /* TODO: Ouvrir menu options de modification */ },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (isSelf) {
                            Text(
                                "C’est vous — modifiez ces infos dans « Modifier le profil » ou « Confidentialité ».",
                                style = MaterialTheme.typography.labelMedium,
                                color = GreenGabo
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Statistiques d'engagement
                        engagementStats.value?.let { stats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.postsCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Posts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.followersCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Abonnés",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.followingCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Abonnements",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${stats.likesCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "J'aime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (!showCultural) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    when (profile.culturalProfileVisibility) {
                                        "hidden" -> "Cette personne a choisi de ne pas afficher son engagement Ogoula."
                                        "followers_only" -> "Suivez ce profil depuis le fil (bouton Suivre) pour voir son intention et son engagement."
                                        else -> "Informations d’engagement non disponibles."
                                    },
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Engagement sur Ogoula",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    profile.culturalReferenceCountry?.takeIf { it.isNotBlank() }?.let { c ->
                                        Text(
                                            "Lien culturel : $c",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    val intents = parseIntentionsCsv(profile.culturalIntentions)
                                    if (intents.isNotEmpty()) {
                                        Text(
                                            "Intentions :",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        intents.forEach { id ->
                                            Text(
                                                "• ${intentionLabelForId(id)}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    profile.selfRole?.takeIf { it.isNotBlank() }?.let { r ->
                                        Text(
                                            "Profil : ${roleLabelForId(r)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    profile.contributionSentence?.takeIf { it.isNotBlank() }?.let { s ->
                                        Text(
                                            "« $s »",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (!profile.proContributionAcknowledgedAt.isNullOrBlank()) {
                                        Text(
                                            text = "Charte pro-contribution acceptée (v. ${profile.proContributionCharterVersion ?: "—"}).",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val hasEngagementDetail =
                                        !profile.culturalReferenceCountry.isNullOrBlank() ||
                                            parseIntentionsCsv(profile.culturalIntentions).isNotEmpty() ||
                                            !profile.selfRole.isNullOrBlank() ||
                                            !profile.contributionSentence.isNullOrBlank() ||
                                            !profile.proContributionAcknowledgedAt.isNullOrBlank()
                                    if (!hasEngagementDetail) {
                                        Text(
                                            "Aucun détail d’engagement renseigné pour l’instant.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fonction pour envoyer une demande de suivi
 */
private fun sendFollowRequest(profile: UserProfile, userViewModel: UserViewModel) {
    // Cette fonction doit être appelée depuis un contexte @Composable
    // Pour l'instant, nous allons la simplifier
    try {
        val currentUser = userViewModel.userProfile
        
        // Empêcher l'envoi à soi-même
        if (profile.userId == currentUser.userId) {
            return
        }
        
        android.util.Log.d("FollowRequest", "Demande envoyée à ${profile.alias}")
        // TODO: Implémenter l'envoi réel avec coroutines
    } catch (e: Exception) {
        android.util.Log.e("FollowRequest", "Exception envoi demande", e)
    }
}

/**
 * Vérifie si l'utilisateur actuel possède la publication (pour afficher les options de modification)
 */
private fun viewerOwnsPost(authorAlias: String): Boolean {
    // TODO: Implémenter la logique pour vérifier si l'utilisateur actuel est l'auteur
    // Pour l'instant, retourner false
    return false
}
