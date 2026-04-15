package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ogoula.ui.components.handlesEqual
import com.example.ogoula.ui.onboarding.intentionLabelForId
import com.example.ogoula.ui.onboarding.parseIntentionsCsv
import com.example.ogoula.ui.onboarding.roleLabelForId
import com.example.ogoula.ui.theme.GreenGabo

private fun viewerFollowsSubject(followedHandles: List<String>, subjectAlias: String): Boolean =
    followedHandles.any { handlesEqual(it, subjectAlias) }

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

    DisposableEffect(Unit) {
        onDispose { userViewModel.clearPublicProfile() }
    }

    LaunchedEffect(userHandle) {
        userViewModel.loadPublicProfileByAlias(userHandle)
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
                        "Profil introuvable ou alias incorrect.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val isSelf = handlesEqual(viewer.alias, profile.alias)
                val showCultural = showCulturalBlock(profile, viewer.alias, followedHandles)
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
                                text = profile.firstName.take(1).uppercase() + profile.lastName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Text(
                            "${profile.firstName} ${profile.lastName}".trim().ifEmpty { profile.alias },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            profile.alias,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelf) {
                            Text(
                                "C’est vous — modifiez ces infos dans « Modifier le profil » ou « Confidentialité ».",
                                style = MaterialTheme.typography.labelMedium,
                                color = GreenGabo
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
