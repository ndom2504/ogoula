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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint
import com.example.ogoula.ui.components.PostItem
import com.example.ogoula.ui.components.handlesEqual
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.StoryViewModel
import com.example.ogoula.ui.Story
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    onAwasClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    onOpenVideoPlaylist: (String) -> Unit = {},
) {
    val posts by postViewModel.posts.collectAsState()
    val stories = storyViewModel.stories
    val profile = userViewModel.userProfile
    val context = LocalContext.current
    var openedStoryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        storyViewModel.refresh()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(90_000L)
            storyViewModel.refresh()
        }
    }

    val feedMaxCap = 900.dp
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val feedMaxWidth = maxWidth.coerceAtMost(feedMaxCap)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = feedMaxWidth)
                .fillMaxWidth(),
        ) {
            item {
                StoryBar(
                    stories = stories,
                    onAddStoryClick = onAddStoryClick,
                    onStoryClick = { openedStoryId = it.id },
                )
            }
            item {
                AwasInputBar(profileImageUri = profile.profileImageUri, onClick = onAwasClick)
            }
            itemsIndexed(posts) { postIndex, post ->
                val displayPost = if (handlesEqual(post.handle, profile.alias)) {
                    val fullName = "${profile.firstName} ${profile.lastName}".trim()
                    post.copy(
                        author = fullName.ifEmpty { post.author },
                        authorImageUri = profile.profileImageUri ?: post.authorImageUri
                    )
                } else post

                val lazyItemIndex = postIndex + 2
                val videoUrlFinal = displayPost.videoUrl
                    ?: displayPost.imageUrls.find { it.startsWith("video:") }?.removePrefix("video:")
                val hasVideo = !videoUrlFinal.isNullOrBlank()
                val feedVideoActive = hasVideo && centerFocusedLazyIndex == lazyItemIndex

                PostItem(
                    post = displayPost,
                    isFollowed = postViewModel.followedUsers.contains(post.handle),
                    showFollowButton = !handlesEqual(post.handle, profile.alias),
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
                    onEdit = { newContent -> postViewModel.editPost(post.id, newContent) },
                    onOpenProfile = {
                        if (post.handle.isNotBlank()) onOpenUserProfile(post.handle)
                    },
                    onVideoTapOpenPlaylist = if (hasVideo) {
                        { onOpenVideoPlaylist(displayPost.id) }
                    } else {
                        null
                    },
                )
            }
        }

        openedStoryId?.let { sid ->
            StoryViewerDialog(
                storyId = sid,
                storyViewModel = storyViewModel,
                profile = profile,
                onDismiss = { openedStoryId = null },
            )
        }
    }
}

private fun formatStoryTimeRemaining(expiresAtMs: Long): String {
    val ms = expiresAtMs - System.currentTimeMillis()
    if (ms <= 0L) return "Expirée"
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    return if (h > 0L) "${h}h restantes" else "${m.coerceAtLeast(1L)} min"
}

@Composable
private fun StoryViewerDialog(
    storyId: String,
    storyViewModel: StoryViewModel,
    profile: UserProfile,
    onDismiss: () -> Unit,
) {
    val story = storyViewModel.stories.find { it.id == storyId }
    if (story == null) {
        LaunchedEffect(storyId) {
            onDismiss()
        }
        return
    }
    val isOwn = story.userId == profile.userId ||
        (profile.alias.isNotBlank() && handlesEqual(story.authorHandle, profile.alias))

    LaunchedEffect(storyId, profile.userId) {
        storyViewModel.recordStoryViewIfNeeded(profile.userId, story)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (story.contentImageUrl != null) {
                AsyncImage(
                    model = story.contentImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(story.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.contentText.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(16.dp)
            ) {
                Text(
                    text = story.author,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatStoryTimeRemaining(story.expiresAtMs),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${story.views} vue${if (story.views > 1) "s" else ""}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (!isOwn && profile.alias.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    storyViewModel.toggleStoryValidate(story.id, profile.alias)
                                }
                            ) {
                                Icon(
                                    imageVector = if (story.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Je valide",
                                    tint = if (story.isValidatedByMe) GreenGabo else Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                "${story.validates}",
                                color = if (story.isValidatedByMe) GreenGabo else Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Je valide",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    } else if (isOwn) {
                        Text(
                            text = "${story.validates} validation${if (story.validates > 1) "s" else ""}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryBar(
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit,
) {
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
                StoryCard(story = story, onClick = { onStoryClick(story) })
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
fun StoryCard(story: Story, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 160.dp)
            .clickable { onClick() }
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
            if (story.views > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "${story.views}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp
                    )
                }
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
