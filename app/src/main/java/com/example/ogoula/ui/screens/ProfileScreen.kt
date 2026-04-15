package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.components.handlesEqual
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.StoryViewModel
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.onboarding.intentionLabelForId
import com.example.ogoula.ui.onboarding.parseIntentionsCsv
import com.example.ogoula.ui.onboarding.roleLabelForId
import kotlin.math.abs

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    onOpenVideoPlaylist: (String) -> Unit = {},
) {
    val profile = userViewModel.userProfile
    val allPosts by postViewModel.posts.collectAsState()
    val myPosts = allPosts.filter { handlesEqual(it.handle, profile.alias) }
    val context = LocalContext.current
    var showEngagementEdit by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val centerFocusedLazyIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf -1
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }?.index ?: -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            ProfileHeader(
                userViewModel = userViewModel,
                onMenuClick = onMenuClick,
                onEditClick = onEditClick
            )
        }
        item {
            OutlinedButton(
                onClick = { showEngagementEdit = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Modifier mon engagement",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "(pays, orientation, intentions, phrase)",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            PopulariteDashboard(postViewModel, storyViewModel, profile.alias)
        }
        item {
            StatsSection(postCount = myPosts.size, followingCount = postViewModel.followedUsers.size)
        }
        item {
            Text(
                text = "Mon Empreinte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (myPosts.isEmpty()) {
            item {
                Text(
                    "Aucune publication pour le moment.",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        } else {
            // Indices 0–4 : en-tête, engagement, stats, titre ; les posts commencent à 5.
            itemsIndexed(myPosts) { postIndex, post ->
                val lazyItemIndex = postIndex + 5
                val videoUrlFinal = post.videoUrl
                    ?: post.imageUrls.find { it.startsWith("video:") }?.removePrefix("video:")
                val hasVideo = !videoUrlFinal.isNullOrBlank()
                val feedVideoActive = hasVideo && centerFocusedLazyIndex == lazyItemIndex

                PostItem(
                    post = post,
                    showFollowButton = false,
                    currentUserHandle = profile.alias,
                    useFeedVideoAutoplay = hasVideo,
                    feedVideoActive = feedVideoActive,
                    onValidate = { postViewModel.toggleValidate(post.id) },
                    onLove = { postViewModel.toggleLove(post.id) },
                    onCommentAdded = { text ->
                        postViewModel.addComment(
                            post.id,
                            "${profile.firstName} ${profile.lastName}".trim().ifEmpty { profile.alias },
                            text,
                            profile.profileImageUri,
                            authorHandle = profile.alias
                        )
                    },
                    onCommentValidate = { commentId ->
                        postViewModel.toggleCommentValidate(post.id, commentId, profile.alias)
                    },
                    onCommentLove = { commentId ->
                        postViewModel.toggleCommentLove(post.id, commentId, profile.alias)
                    },
                    onOpenProfile = {
                        if (post.handle.isNotBlank()) onOpenUserProfile(post.handle)
                    },
                    onShare = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, post.content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    onVideoTapOpenPlaylist = if (hasVideo) {
                        { onOpenVideoPlaylist(post.id) }
                    } else {
                        null
                    },
                )
            }
        }
        }
        EngagementEditBottomSheet(
            visible = showEngagementEdit,
            profile = profile,
            userViewModel = userViewModel,
            onDismiss = { showEngagementEdit = false }
        )
    }
}

@Composable
fun ProfileHeader(userViewModel: UserViewModel, onMenuClick: () -> Unit, onEditClick: () -> Unit) {
    val profile = userViewModel.userProfile
    
    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(GreenGabo, GreenGabo.copy(alpha = 0.82f), OgoulaSurfaceTint),
                    )
                )
        ) {
            if (!profile.bannerImageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = profile.bannerImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { /* URL invalide ou règles Storage bloquantes */ }
                )
            }
        }
        
        // Menu Button
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
        }

        // Edit Button
        Button(
            onClick = onEditClick,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Modifier", fontSize = 12.sp)
        }
        
        // Profile Picture
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomCenter)
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (!profile.profileImageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = profile.profileImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { /* URL invalide ou règles Storage bloquantes */ }
                )
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val displayName = if (profile.firstName.isNotEmpty()) "${profile.firstName} ${profile.lastName}" else "Utilisateur Ogoula"
        Text(text = displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = profile.alias.ifEmpty { "@anonyme" }, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        val tagline = profile.contributionSentence?.trim().orEmpty().ifBlank {
            "Fier Gabonais. Parlons des choses de notre bled."
        }
        Text(
            text = tagline,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = if (profile.contributionSentence?.isNotBlank() == true) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Color.Gray
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ProfileEngagementSummary(profile = profile)
    }
}

@Composable
private fun ProfileEngagementSummary(profile: UserProfile) {
    val intents = parseIntentionsCsv(profile.culturalIntentions)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Engagement Ogoula",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GreenGabo
            )
            profile.culturalReferenceCountry?.takeIf { it.isNotBlank() }?.let { c ->
                Text(
                    text = "Référence : $c",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Orientation : ${roleLabelForId(profile.selfRole)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (intents.isNotEmpty()) {
                Text(
                    text = "Intentions :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                intents.forEach { id ->
                    Text(
                        text = "• ${intentionLabelForId(id)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            profile.contributionSentence?.takeIf { it.isNotBlank() }?.let { s ->
                Text(
                    text = "« $s »",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PopulariteDashboard(
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userAlias: String,
) {
    LaunchedEffect(Unit) {
        storyViewModel.refresh()
    }
    val storyListVersion = storyViewModel.stories.size
    val storyPoints = storyViewModel.getStoryPopularityContribution(userAlias)
    val postPoints = postViewModel.getPopularityScore(userAlias)
    val score = postPoints + storyPoints
    key(storyListVersion) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(GreenGabo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Score de popularité : $score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Publications, commentaires, stories (24h) : vues ×1, validations ×10",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    if (storyPoints > 0) {
                        Text(
                            text = "Stories : +$storyPoints pts (implication)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (score.toFloat() / 1000f).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = GreenGabo,
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSection(postCount: Int, followingCount: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Publications", value = "$postCount")
        StatItem(label = "Abonnés", value = "0")
        StatItem(label = "Abonnements", value = "$followingCount")
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
