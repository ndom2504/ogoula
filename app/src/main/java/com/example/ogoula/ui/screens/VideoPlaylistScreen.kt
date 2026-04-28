package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.components.CommentSheetContent
import com.example.ogoula.ui.components.Post
import com.example.ogoula.ui.components.formatRelativeTimeFr
import com.example.ogoula.ui.components.buildExoPlayerForStoredVideo
import com.example.ogoula.ui.components.releaseExoPlayer
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaSurfaceTint
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XDarkGray

private fun videoUrlFromPost(p: Post): String? =
    p.videoUrl ?: p.imageUrls.find { it.startsWith("video:") }?.removePrefix("video:")

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlaylistScreen(
    /** Padding du Scaffold (TopAppBar + barre système) pour ne pas masquer la vidéo. */
    scaffoldPadding: PaddingValues = PaddingValues(),
    initialPostId: String,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    onOpenUserProfile: (String) -> Unit,
) {
    val posts by postViewModel.posts.collectAsState()
    val profile = userViewModel.userProfile
    val ctx = LocalContext.current
    var commentSheetPostId by remember { mutableStateOf<String?>(null) }

    val videoPages = remember(posts) {
        posts.mapNotNull { p -> videoUrlFromPost(p)?.let { url -> p to url } }
    }

    val initialPage = remember(videoPages, initialPostId) {
        videoPages.indexOfFirst { it.first.id == initialPostId }.takeIf { it >= 0 } ?: 0
    }

    if (videoPages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(Color(0xFF0D0D12)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Aucune vidéo pour l’instant.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, videoPages.lastIndex),
        pageCount = { videoPages.size },
    )

    val settledPage by remember { derivedStateOf { pagerState.settledPage } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D12), Color(0xFF14161B), XDarkGray),
                ),
            ),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            pageSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) { page ->
            val (post, videoUrl) = videoPages[page]
            val isActive = settledPage == page
            PlaylistVideoPage(
                post = post,
                videoUrl = videoUrl,
                isActive = isActive,
                onValidate = { postViewModel.toggleValidate(post.id) },
                onLove = { postViewModel.toggleLove(post.id) },
                onFavorite = { postViewModel.toggleFavorite(post.id) },
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, post.content)
                    }
                    ctx.startActivity(Intent.createChooser(sendIntent, null))
                },
                onOpenProfile = {
                    if (post.handle.isNotBlank()) onOpenUserProfile(post.handle)
                },
                onCommentClick = { commentSheetPostId = post.id },
                onDelete = { 
                    // TODO: Implémenter la suppression de la vidéo
                    android.util.Log.d("Playlist", "Suppression de la vidéo: ${post.id}")
                },
            )
        }
    }

    commentSheetPostId?.let { pid ->
        val postForComments = posts.find { it.id == pid }
        if (postForComments != null) {
            ModalBottomSheet(
                onDismissRequest = { commentSheetPostId = null },
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                CommentSheetContent(
                    comments = postForComments.comments,
                    currentUserHandle = profile.alias,
                    onCommentValidate = { commentId ->
                        postViewModel.toggleCommentValidate(pid, commentId, profile.alias)
                    },
                    onCommentLove = { commentId ->
                        postViewModel.toggleCommentLove(pid, commentId, profile.alias)
                    },
                    onAddComment = { text ->
                        postViewModel.addComment(
                            pid,
                            "${profile.firstName} ${profile.lastName}".trim().ifEmpty { profile.alias },
                            text,
                            profile.profileImageUri,
                            authorHandle = profile.alias,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistVideoPage(
    post: Post,
    videoUrl: String,
    isActive: Boolean,
    onValidate: () -> Unit,
    onLove: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onOpenProfile: () -> Unit,
    onCommentClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var muted by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    val player = remember(videoUrl) { buildExoPlayerForStoredVideo(context, videoUrl) }

    LaunchedEffect(isActive) {
        if (isActive) {
            player.playWhenReady = true
            player.play()
        } else {
            player.pause()
            muted = true
        }
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }

    DisposableEffect(player) {
        onDispose { releaseExoPlayer(player) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                onRelease = { view -> view.player = null },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp, end = 72.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaylistSideAction(
                icon = if (post.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                count = post.validates,
                tint = if (post.isValidatedByMe) XBlue else Color.White,
                onClick = onValidate,
                label = "Je valide",
            )
            PlaylistSideAction(
                icon = if (post.isLovedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = post.loves,
                tint = if (post.isLovedByMe) Color(0xFFE91E63) else Color.White,
                onClick = onLove,
                label = "J’adore",
            )
            PlaylistSideAction(
                icon = Icons.Default.Star,
                count = post.favorites,
                tint = if (post.isFavoritedByMe) Color(0xFFFFD700) else Color.White,
                onClick = onFavorite,
                label = "Favoris",
            )
            PlaylistSideAction(
                icon = Icons.Default.ModeComment,
                count = post.comments.size,
                tint = Color.White,
                onClick = onCommentClick,
                label = "Commentaires",
            )
            PlaylistSideAction(
                icon = Icons.Default.Share,
                count = 0,
                tint = Color.White,
                onClick = onShare,
                label = "Partager",
            )
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(XBlue, XDarkGray)),
                        CircleShape,
                    ),
            ) {
                if (post.authorImageUri != null) {
                    AsyncImage(
                        model = post.authorImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Profil", tint = Color.White)
                }
            }
            PlaylistPublicationMetaUnderAvatar(post = post)
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { muted = !muted },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Supprimer la vidéo") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(end = 56.dp),
        ) {
            Text(
                text = post.author,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = post.handle,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.content,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Infos de la publication (type, date, compteurs) sous la bulle de profil de la playlist. */
@Composable
private fun PlaylistPublicationMetaUnderAvatar(post: Post) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp),
    ) {
        if (post.isCommunityPost) {
            Surface(
                color = XBlue.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    "COMMUNAUTÉ",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = formatRelativeTimeFr(post.time),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
    }
}

@Composable
private fun PlaylistSideAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color,
    onClick: () -> Unit,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.42f), CircleShape),
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        if (count > 0) {
            Text(
                text = "$count",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
