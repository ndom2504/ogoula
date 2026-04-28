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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XBorderGray
import com.example.ogoula.ui.theme.XDarkGray
import com.example.ogoula.ui.theme.XTextGray
import com.example.ogoula.ui.theme.XWhite
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.components.handlesEqual
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.StoryViewModel
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.ProfileSyncManager
import com.example.ogoula.data.UserRepository
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
    val myProducts = myPosts.filter { it.content.contains("🔗 [Link]") }
    
    val context = LocalContext.current
    val userRepository = UserRepository()
    val profileSyncManager = remember { ProfileSyncManager(userViewModel, userRepository, context) }
    var showEngagementEdit by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(XBlack)
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                ProfileHeader(
                    userViewModel = userViewModel,
                    postViewModel = postViewModel,
                    onMenuClick = onMenuClick,
                    onEditClick = onEditClick
                )
            }
            item {
                PopulariteDashboard(postViewModel, storyViewModel, profile.alias)
            }
            item {
                StatsSection(postCount = myPosts.size, followingCount = postViewModel.followedUsers.size)
            }
            
            // --- ONGLET DE NAVIGATION INTERNE ---
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = XBlack,
                    contentColor = XBlue,
                    divider = { HorizontalDivider(color = XBorderGray, thickness = 0.5.dp) },
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = XBlue
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.GridView, null, modifier = Modifier.size(20.dp)) },
                        text = { Text("Flux", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(20.dp)) },
                        text = { Text("Bons Plans", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            if (selectedTab == 0) {
                // FLUX CLASSIQUE
                if (myPosts.isEmpty()) {
                    item {
                        EmptyState("Aucune publication pour le moment.")
                    }
                } else {
                    itemsIndexed(myPosts) { _, post ->
                        ProfilePostItem(post, profile, postViewModel, onOpenUserProfile, onOpenVideoPlaylist)
                    }
                }
            } else {
                // BONS PLANS (AFFILIATION)
                if (myProducts.isEmpty()) {
                    item {
                        EmptyState("Aucun produit recommandé pour l'instant.")
                    }
                } else {
                    itemsIndexed(myProducts) { _, post ->
                        ProfilePostItem(post, profile, postViewModel, onOpenUserProfile, onOpenVideoPlaylist)
                    }
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
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = XTextGray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfilePostItem(
    post: com.example.ogoula.ui.components.Post,
    profile: UserProfile,
    postViewModel: PostViewModel,
    onOpenUserProfile: (String) -> Unit,
    onOpenVideoPlaylist: (String) -> Unit
) {
    val hasVideo = (post.videoUrl ?: post.imageUrls.find { it.startsWith("video:") }) != null
    
    PostItem(
        post = post,
        showFollowButton = false,
        currentUserHandle = profile.alias,
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
            val context = profile.userId // Just for avoiding unused warning, not real context
        },
        onVideoTapOpenPlaylist = if (hasVideo) {
            { onOpenVideoPlaylist(post.id) }
        } else {
            null
        },
    )
}

@Composable
fun ProfileHeader(userViewModel: UserViewModel, postViewModel: PostViewModel, onMenuClick: () -> Unit, onEditClick: () -> Unit) {
    val profile = userViewModel.userProfile
    
    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(XBlack, XBlue.copy(alpha = 0.72f), XDarkGray),
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
            colors = ButtonDefaults.buttonColors(containerColor = XBlue.copy(alpha = 0.18f), contentColor = XWhite),
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
                .border(3.dp, XBlack, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(XDarkGray),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = XWhite)
            if (postViewModel.getInfluenceScore(profile.alias) >= 300) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Verified, "Expert", tint = XBlue, modifier = Modifier.size(20.dp))
            }
        }
        Text(text = profile.alias.ifEmpty { "@anonyme" }, style = MaterialTheme.typography.bodyMedium, color = XTextGray)
        Spacer(modifier = Modifier.height(8.dp))
        val tagline = profile.contributionSentence?.trim().orEmpty().ifBlank {
            "Fier Gabonais. Parlons des choses de notre bled."
        }
        Text(
            text = tagline,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = if (profile.contributionSentence?.isNotBlank() == true) {
                XWhite
            } else {
                XTextGray
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
            containerColor = XDarkGray
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, XBorderGray)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Engagement Ogoula",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = XBlue
            )
            profile.culturalReferenceCountry?.takeIf { it.isNotBlank() }?.let { c ->
                Text(
                    text = "Référence : $c",
                    style = MaterialTheme.typography.bodySmall,
                    color = XWhite
                )
            }
            Text(
                text = "Orientation : ${roleLabelForId(profile.selfRole)}",
                style = MaterialTheme.typography.bodySmall,
                color = XWhite
            )
            if (intents.isNotEmpty()) {
                Text(
                    text = "Intentions :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = XTextGray
                )
                intents.forEach { id ->
                    Text(
                        text = "• ${intentionLabelForId(id)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = XWhite
                    )
                }
            }
            profile.contributionSentence?.takeIf { it.isNotBlank() }?.let { s ->
                Text(
                    text = "« $s »",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = XWhite,
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
    val score = postViewModel.getInfluenceScore(userAlias) // Utilisation du nouveau score intelligent
    
    key(storyListVersion) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = XDarkGray),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, XBorderGray)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(XBlue.copy(alpha = 0.16f))
                        .border(1.dp, XBlue.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = XBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Score d'Expertise : $score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = XWhite
                    )
                    Text(
                        text = "Basé sur la pertinence de vos votes et la qualité de vos débats.",
                        style = MaterialTheme.typography.labelSmall,
                        color = XTextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (score.toFloat() / 1000f).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = XBlue,
                        trackColor = XBorderGray
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
        StatItem(label = "Oracles suivis", value = "$followingCount")
        StatItem(label = "Badges", value = "0")
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = XWhite
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = XTextGray)
    }
}
