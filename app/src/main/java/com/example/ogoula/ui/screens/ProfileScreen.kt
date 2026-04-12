package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.YellowGabo
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.UserViewModel

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues, 
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    onMenuClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val profile = userViewModel.userProfile
    val allPosts by postViewModel.posts.collectAsState()
    val myPosts = allPosts.filter { it.handle == profile.alias }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        item {
            ProfileHeader(
                userViewModel = userViewModel,
                onMenuClick = onMenuClick,
                onEditClick = onEditClick
            )
        }
        item {
            PopulariteDashboard(postViewModel, profile.alias)
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
            items(myPosts) { post ->
                PostItem(
                    post = post,
                    showFollowButton = false,
                    onValidate = { postViewModel.toggleValidate(post.id) },
                    onLove = { postViewModel.toggleLove(post.id) },
                    onCommentAdded = { text -> 
                        postViewModel.addComment(post.id, "${profile.firstName} ${profile.lastName}", text, profile.profileImageUri)
                    },
                    onShare = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, post.content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
            }
        }
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
                    brush = Brush.horizontalGradient(listOf(GreenGabo, YellowGabo, BlueGabo))
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
        Text(
            text = "Fier Gabonais. Parlons des choses de notre bled.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PopulariteDashboard(viewModel: PostViewModel, userAlias: String) {
    val score = viewModel.getPopularityScore(userAlias)
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
                    .background(BlueGabo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Score de Popularité : $score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Basé sur vos interactions réelles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (score.toFloat() / 1000f).coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = BlueGabo,
                    trackColor = Color.White.copy(alpha = 0.5f)
                )
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
