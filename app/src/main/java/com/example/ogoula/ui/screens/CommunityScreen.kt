package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.Community
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XBorderGray
import com.example.ogoula.ui.theme.XDarkGray
import com.example.ogoula.ui.theme.XTextGray
import com.example.ogoula.ui.theme.XWhite
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.components.handlesEqual
import androidx.compose.ui.graphics.Brush

private const val OGUALA_INVITE_URL = "https://www.ogoula.com/invite"
private const val OGUALA_WEB_URL = "https://www.ogoula.com/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(innerPadding: PaddingValues, viewModel: PostViewModel, onCreateCommunityClick: () -> Unit) {
    val communities = viewModel.communities
    var selectedCommunity by remember { mutableStateOf<Community?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🌍 Bleds", "📈 Tendances", "🔥 Top Votes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(XBlack)
    ) {
        // En-tête avec Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = XBlack,
            contentColor = XBlue,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = XBlue
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = XBlue,
                    unselectedContentColor = XTextGray
                )
            }
        }

        when (selectedTab) {
            0 -> MyCommunitiesTab(communities, onCreateCommunityClick) { selectedCommunity = it }
            1 -> TrendsTab(viewModel)
            2 -> TopVotesTab(viewModel)
        }
    }

    // Bottom sheet paramètres de la communauté sélectionnée
    selectedCommunity?.let { community ->
        CommunityDetailSheet(
            community = community,
            viewModel = viewModel,
            onDismiss = { selectedCommunity = null }
        )
    }
}

@Composable
fun MyCommunitiesTab(
    communities: List<Community>,
    onCreateCommunityClick: () -> Unit,
    onCommunityClick: (Community) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Button(
                onClick = onCreateCommunityClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = XBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Créer mon Bled", fontWeight = FontWeight.Bold)
            }
        }

        if (communities.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun Bled rejoint pour l'instant.", color = XTextGray)
                }
            }
        }

        items(communities) { community ->
            CommunityCard(community = community, onClick = { onCommunityClick(community) })
        }
    }
}

@Composable
fun TrendsTab(viewModel: PostViewModel) {
    val posts by viewModel.posts.collectAsState()
    val trendPosts = remember(posts) {
        posts.filter { it.postType in listOf("duel", "sondage", "vote") }
            .sortedByDescending { it.validates + it.loves + (it.pollVoteCounts.sum()) }
            .take(10)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Ce qui fait du bruit 📈",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = XWhite
            )
        }
        items(trendPosts) { post ->
            PostItem(
                post = post,
                onValidate = { viewModel.toggleValidate(post.id) },
                onLove = { viewModel.toggleLove(post.id) },
                onCommentAdded = { /* ... */ },
                onShare = { /* ... */ },
                onPollVote = { idx -> viewModel.voteOnPoll(post.id, idx) }
            )
        }
    }
}

@Composable
fun TopVotesTab(viewModel: PostViewModel) {
    val posts by viewModel.posts.collectAsState()
    
    // Extraction des experts (score >= 300)
    val experts = remember(posts) {
        posts.map { it.handle }
            .distinct()
            .map { handle -> 
                val author = posts.find { it.handle == handle }?.author ?: handle
                val img = posts.find { it.handle == handle }?.authorImageUri
                val score = viewModel.getInfluenceScore(handle)
                ExpertInfo(handle, author, img, score)
            }
            .filter { it.score >= 300 }
            .sortedByDescending { it.score }
            .take(10)
    }

    val topPosts = remember(posts) {
        posts.filter { it.postType in listOf("duel", "sondage", "vote") }
            .sortedByDescending { it.pollVoteCounts.sum() }
            .take(20)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // --- SECTION EXPERTS ---
        if (experts.isNotEmpty()) {
            item {
                Text(
                    "Top Experts (Oracles) 🧠",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                    color = XWhite
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(experts) { expert ->
                        ExpertCard(expert)
                    }
                }
                HorizontalDivider(color = XBorderGray, thickness = 0.5.dp)
            }
        }

        item {
            Text(
                "La voix du peuple 🔥",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = XWhite
            )
        }
        items(topPosts) { post ->
            PostItem(
                post = post,
                onValidate = { viewModel.toggleValidate(post.id) },
                onLove = { viewModel.toggleLove(post.id) },
                onCommentAdded = { /* ... */ },
                onShare = { /* ... */ },
                onPollVote = { idx -> viewModel.voteOnPoll(post.id, idx) }
            )
        }
    }
}

data class ExpertInfo(val handle: String, val author: String, val image: String?, val score: Int)

@Composable
fun ExpertCard(expert: ExpertInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(XDarkGray)
                .border(2.dp, XBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (expert.image != null) {
                AsyncImage(model = expert.image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(expert.author.take(1), color = XWhite, fontWeight = FontWeight.Bold)
            }
            Icon(
                Icons.Default.Verified, 
                null, 
                tint = XBlue, 
                modifier = Modifier.size(18.dp).align(Alignment.BottomEnd).background(XBlack, CircleShape)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(expert.author, style = MaterialTheme.typography.labelSmall, color = XWhite, maxLines = 1, fontWeight = FontWeight.Bold)
        Text("${expert.score} pts", style = MaterialTheme.typography.labelSmall, color = XBlue, fontWeight = FontWeight.Black)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailSheet(
    community: Community,
    viewModel: PostViewModel,
    onDismiss: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val communityPosts = remember(posts, community.name) {
        posts.filter { it.isCommunityPost && it.content.contains(community.name, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = XBlack,
        dragHandle = { BottomSheetDefaults.DragHandle(color = XBorderGray) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (community.coverImageUri != null) {
                    AsyncImage(
                        model = community.coverImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(XDarkGray))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, XBlack))))
                
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(community.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = XWhite)
                    Text("${community.memberCount} membres actifs", style = MaterialTheme.typography.bodySmall, color = XBlue)
                }
            }

            // Onglets internes au Bled
            var subTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = subTab,
                containerColor = XBlack,
                contentColor = XBlue,
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[subTab]), color = XBlue) }
            ) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Flux") })
                Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Duels") })
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("A propos") })
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (subTab == 0) {
                    items(communityPosts) { post ->
                        PostItem(
                            post = post,
                            onValidate = { viewModel.toggleValidate(post.id) },
                            onLove = { viewModel.toggleLove(post.id) },
                            onCommentAdded = { /* ... */ },
                            onShare = { /* ... */ }
                        )
                    }
                } else if (subTab == 1) {
                    val duels = communityPosts.filter { it.postType == "duel" }
                    items(duels) { post ->
                        PostItem(
                            post = post,
                            onValidate = { viewModel.toggleValidate(post.id) },
                            onLove = { viewModel.toggleLove(post.id) },
                            onCommentAdded = { /* ... */ },
                            onShare = { /* ... */ },
                            onPollVote = { idx -> viewModel.voteOnPoll(post.id, idx) }
                        )
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Description", fontWeight = FontWeight.Bold, color = XWhite)
                            Text(community.description, color = XTextGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityCard(community: Community, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = XDarkGray),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, XBorderGray)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(XBlack),
                contentAlignment = Alignment.Center
            ) {
                if (community.coverImageUri != null) {
                    AsyncImage(model = community.coverImageUri, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Groups, null, tint = XBlue)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(community.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = XWhite)
                Text(community.description, style = MaterialTheme.typography.bodySmall, color = XTextGray, maxLines = 1)
                Text("${community.memberCount} membres", style = MaterialTheme.typography.labelSmall, color = XBlue)
            }
            Icon(Icons.Default.ChevronRight, null, tint = XTextGray)
        }
    }
}
