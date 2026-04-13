package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.StoryViewModel
import com.example.ogoula.ui.Story
import com.example.ogoula.ui.UserViewModel

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    onAwasClick: () -> Unit,
    onAddStoryClick: () -> Unit,
) {
    val posts by postViewModel.posts.collectAsState()
    val stories = storyViewModel.stories
    val profile = userViewModel.userProfile
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        storyViewModel.refresh()
    }

    val feedMaxCap = 900.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val feedMaxWidth = maxWidth.coerceAtMost(feedMaxCap)
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = feedMaxWidth)
                .fillMaxWidth(),
        ) {
            item {
                StoryBar(stories, onAddStoryClick)
            }
            item {
                AwasInputBar(profileImageUri = profile.profileImageUri, onClick = onAwasClick)
            }
            items(posts) { post ->
                val displayPost = if (post.handle == profile.alias) {
                    val fullName = "${profile.firstName} ${profile.lastName}".trim()
                    post.copy(
                        author = fullName.ifEmpty { post.author },
                        authorImageUri = profile.profileImageUri ?: post.authorImageUri
                    )
                } else post

                PostItem(
                    post = displayPost,
                    isFollowed = postViewModel.followedUsers.contains(post.handle),
                    showFollowButton = post.handle != profile.alias,
                    currentUserHandle = profile.alias,
                    onValidate = { postViewModel.toggleValidate(post.id) },
                    onLove = { postViewModel.toggleLove(post.id) },
                    onCommentAdded = { text ->
                        postViewModel.addComment(post.id, "${profile.firstName} ${profile.lastName}", text, profile.profileImageUri)
                    },
                    onToggleFollow = { postViewModel.toggleFollow(post.handle) },
                    onShare = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, post.content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    onDelete = { postViewModel.deletePost(post.id) },
                    onEdit = { newContent -> postViewModel.editPost(post.id, newContent) }
                )
            }
        }
    }
}

@Composable
fun StoryBar(stories: List<Story>, onAddStoryClick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Au Quartier",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AddStoryCard(onClick = onAddStoryClick)
            }
            items(stories, key = { it.id }) { story ->
                StoryCard(story)
            }
        }
    }
}

@Composable
fun AddStoryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GreenGabo)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    "Ajouter",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StoryCard(story: Story) {
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(GreenGabo, GreenGabo.copy(alpha = 0.75f), OgoulaSurfaceTint),
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (story.contentImageUrl != null) {
                AsyncImage(
                    model = story.contentImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(story.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.contentText ?: "",
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = story.author,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AwasInputBar(profileImageUri: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GreenGabo),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Quoi de neuf au pays ?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
