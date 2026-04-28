package com.example.ogoula.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.ogoula.BuildConfig
import com.example.ogoula.ui.VideoVolumeViewModel
import com.example.ogoula.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

private const val FeedMediaAspectRatio = 4f / 5f

internal fun normalizePostTimeEpochMillis(raw: Long): Long {
    if (raw <= 0L) return raw
    return if (raw < 1_000_000_000_000L) raw * 1000L else raw
}

@Composable
private fun AdaptiveSingleImage(url: String) {
    var imageRatio by remember { mutableStateOf(FeedMediaAspectRatio) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, XBorderGray, RoundedCornerShape(16.dp))
            .aspectRatio(imageRatio.coerceIn(0.5f, 2f))
            .background(XDarkGray),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                val painter = state.painter
                val intrinsicSize = painter.intrinsicSize
                if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                    imageRatio = intrinsicSize.width / intrinsicSize.height
                }
            }
        )
    }
}

internal fun handlesEqual(a: String, b: String): Boolean {
    fun norm(s: String) = s.trim().lowercase().removePrefix("@").let { "@$it" }
    if (a.isBlank() || b.isBlank()) return false
    return norm(a) == norm(b)
}

internal fun formatRelativeTimeFr(rawWhenMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val whenMs = normalizePostTimeEpochMillis(rawWhenMs)
    if (whenMs <= 0L) return "récemment"
    val diff = nowMs - whenMs
    if (diff < -60_000) {
        val postAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(whenMs), ZoneId.systemDefault())
        return postAt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.FRENCH))
    }
    if (diff < 60_000) return "à l'instant"
    if (diff < 3_600_000) {
        val m = (diff / 60_000).toInt()
        return if (m == 1) "il y a 1 min" else "il y a $m min"
    }
    if (diff < 86_400_000) {
        val h = (diff / 3_600_000).toInt()
        return if (h == 1) "il y a 1 h" else "il y a $h h"
    }
    val zone = ZoneId.systemDefault()
    val postAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(whenMs), zone)
    val nowAt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone)
    val dayFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
    val daysBetween = ChronoUnit.DAYS.between(postAt.toLocalDate(), nowAt.toLocalDate())
    return when {
        daysBetween <= 0L -> "aujourd'hui à ${postAt.format(dayFmt)}"
        daysBetween == 1L -> "hier à ${postAt.format(dayFmt)}"
        daysBetween < 7L -> "il y a $daysBetween j"
        daysBetween < 30L -> "il y a ${daysBetween / 7L} sem."
        else -> postAt.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
    }
}

@Serializable
data class Comment(
    val id: String = "",
    val author: String = "",
    val text: String = "",
    val time: Long = 0L,
    @SerialName("author_image_uri") val authorImageUri: String? = null,
    @SerialName("author_handle") val authorHandle: String = "",
    val validates: Int = 0,
    val loves: Int = 0,
    @SerialName("replies") val replies: List<Comment> = emptyList(),
    @Transient val isValidatedByMe: Boolean = false,
    @Transient val isLovedByMe: Boolean = false
)

@Serializable
data class Post(
    val id: String = "",
    val author: String = "",
    val handle: String = "",
    val content: String = "",
    val time: Long = 0L,
    val validates: Int = 0,
    val disvalidates: Int = 0,
    val loves: Int = 0,
    val favorites: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    val comments: List<Comment> = emptyList(),
    @SerialName("is_validated_by_me") val isValidatedByMe: Boolean = false,
    @SerialName("is_disvalidated_by_me") val isDisvalidatedByMe: Boolean = false,
    @SerialName("is_loved_by_me") val isLovedByMe: Boolean = false,
    @SerialName("is_favorited_by_me") val isFavoritedByMe: Boolean = false,
    @SerialName("is_community_post") val isCommunityPost: Boolean = false,
    @SerialName("author_image_uri") val authorImageUri: String? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("post_type") val postType: String = "classique",
    @SerialName("poll_options") val pollOptions: List<String> = emptyList(),
    @SerialName("poll_vote_counts") val pollVoteCounts: List<Int> = emptyList(),
    @SerialName("poll_option_images") val pollOptionImages: List<String> = emptyList(),
    @SerialName("goal_count") val goalCount: Int = 0,
    @SerialName("deadline_at") val deadlineAt: Long? = null,
    // Champs pour les posts produits
    @SerialName("product_url") val productUrl: String? = null,
    @SerialName("product_title") val productTitle: String? = null,
    @SerialName("product_price") val productPrice: String? = null,
    @SerialName("product_image") val productImage: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItem(
    post: Post,
    isFollowed: Boolean = false,
    showFollowButton: Boolean = true,
    currentUserHandle: String = "",
    onValidate: () -> Unit,
    onDisvalidate: () -> Unit = {},
    onLove: () -> Unit,
    onCommentAdded: (String) -> Unit,
    onReplyAdded: (commentId: String, replyText: String) -> Unit = { _, _ -> },
    onCommentValidate: (String) -> Unit = {},
    onCommentLove: (String) -> Unit = {},
    onShare: () -> Unit,
    onToggleFollow: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onVideoTapOpenPlaylist: (() -> Unit)? = null,
    videoVolumeViewModel: VideoVolumeViewModel? = null,
    myPollVoteIndex: Int? = null,
    onPollVote: (Int) -> Unit = {},
    onFavorite: () -> Unit = {},
    onViewTracked: (() -> Unit)? = null,
) {
    var showCommentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showInteractionsDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(post.content) }
    val isOwnPost = currentUserHandle.isNotEmpty() && handlesEqual(post.handle, currentUserHandle)
    val context = LocalContext.current

    var timeTick by remember(post.id) { mutableIntStateOf(0) }
    LaunchedEffect(post.id, post.time) {
        while (true) {
            delay(30_000)
            timeTick++
        }
    }
    
    // Track view when post is displayed
    LaunchedEffect(post.id) {
        onViewTracked?.invoke()
    }
    
    val timeLabel = remember(post.time, timeTick) { formatRelativeTimeFr(post.time) }

    val cleanContent = remember(post.content) {
        post.content.replace(Regex("\n\n🔗 \\[Link\\]\\(.*?\\)"), "")
                    .replace(Regex("\n💰 Prix: .*"), "")
    }

    val price = remember(post.content) {
        Regex("Prix: (.*)").find(post.content)?.groupValues?.get(1)
    }

    val isExpert = remember(post.handle) { post.handle.length % 5 == 0 } 

    val cardBackgroundColor = if (post.isCommunityPost) XDarkGray else XBlack

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBackgroundColor)
    ) {
        if (post.isCommunityPost) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)) {
                Surface(color = XBlue.copy(alpha = 0.16f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, XBlue.copy(alpha = 0.4f))) {
                    Text(
                        "COMMUNAUTÉ",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = XBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        PostTypeBadge(post.postType)
        if (post.postType != "classique" && (post.deadlineAt != null || post.goalCount > 0)) {
            DeadlineChip(deadlineAt = post.deadlineAt, goalCount = post.goalCount)
        }
        
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight()
                    .padding(top = 16.dp)
                    .clickable { showInteractionsDialog = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                InteractorsSidebarContent(post)
            }

            Column(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    enabled = onOpenProfile != null && post.handle.isNotBlank(),
                                    onClick = { onOpenProfile?.invoke() }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(XDarkGray)
                                    .border(0.5.dp, XBorderGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (post.authorImageUri != null) {
                                    AsyncImage(
                                        model = post.authorImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(post.author.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.author, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    if (isExpert) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, "Expert", tint = XBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.handle, style = MaterialTheme.typography.bodySmall, color = XTextGray, maxLines = 1)
                                    Text(" · $timeLabel", style = MaterialTheme.typography.labelSmall, color = XTextGray)
                                }
                            }
                        }

                        if (showFollowButton && !isOwnPost) {
                            TextButton(
                                onClick = onToggleFollow,
                                colors = ButtonDefaults.textButtonColors(containerColor = if (isFollowed) Color.Transparent else XBlue.copy(alpha = 0.12f), contentColor = if (isFollowed) Color.White else XBlue),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isFollowed) XBorderGray else XBlue.copy(alpha = 0.35f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(if (isFollowed) "Suivi" else "Suivre", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isOwnPost) {
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(20.dp), tint = XTextGray)
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { showMenu = false; editedContent = post.content; showEditDialog = true })
                                    DropdownMenuItem(text = { Text("Supprimer", color = Color.Red) }, onClick = { showMenu = false; onDelete?.invoke() })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cleanContent, modifier = Modifier.weight(1f).padding(bottom = 12.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        if (price != null) {
                            Surface(color = Color(0xFF2E7D32).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)) {
                                Text(price, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                PostTypeContent(post, myPollVoteIndex, onPollVote)

                val inlineVideoUrl = post.videoUrl ?: post.imageUrls.firstOrNull { it.startsWith("video:") }?.removePrefix("video:")
                val inlineThumbnailUrl = post.imageUrls.firstOrNull { !it.startsWith("video:") }
                val imageUrlsOnly = if (!inlineVideoUrl.isNullOrBlank()) emptyList() else post.imageUrls.filter { !it.startsWith("video:") }

                if (!inlineVideoUrl.isNullOrBlank()) {
                    VideoFeedPlayer(thumbnailUrl = inlineThumbnailUrl, videoUrl = inlineVideoUrl, isActive = true, onTapOpenPlaylist = { onVideoTapOpenPlaylist?.invoke() }, videoVolumeViewModel = videoVolumeViewModel)
                }

                if (imageUrlsOnly.isNotEmpty()) {
                    if (imageUrlsOnly.size == 1) AdaptiveSingleImage(url = imageUrlsOnly[0])
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = XBorderGray.copy(alpha = 0.2f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        // Bouton "Voir le produit" si c'est un post produit
                        if (!post.productUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.productUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                            ) {
                                Text("🔗 Voir le produit", style = MaterialTheme.typography.labelSmall, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                        }
                        IconButton(onClick = onValidate, modifier = Modifier.size(24.dp)) {
                            Icon(if (post.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, null, tint = if (post.isValidatedByMe) Color.White else XTextGray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showCommentSheet = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.ModeComment, null, tint = XTextGray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Share, null, tint = XTextGray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onFavorite, modifier = Modifier.size(24.dp)) {
                            Icon(if (post.isFavoritedByMe) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = if (post.isFavoritedByMe) Color.White else XTextGray, modifier = Modifier.size(20.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Visibility, null, tint = XTextGray, modifier = Modifier.size(16.dp))
                        Text("${post.views}", style = MaterialTheme.typography.labelMedium, color = XTextGray, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = XBorderGray)
    }

    if (showInteractionsDialog) { InteractionsDialog(post) { showInteractionsDialog = false } }
    if (showEditDialog) {
        AlertDialog(onDismissRequest = { showEditDialog = false }, title = { Text("Modifier le post") }, text = { OutlinedTextField(value = editedContent, onValueChange = { editedContent = it }, modifier = Modifier.fillMaxWidth(), minLines = 3) }, confirmButton = { TextButton(onClick = { if (editedContent.isNotBlank()) { onEdit?.invoke(editedContent); showEditDialog = false } }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Annuler") } })
    }
    if (showCommentSheet) {
        ModalBottomSheet(onDismissRequest = { showCommentSheet = false }, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            CommentSheetContent(comments = post.comments, currentUserHandle = currentUserHandle, onCommentValidate = onCommentValidate, onCommentLove = onCommentLove, onAddComment = onCommentAdded, onAddReply = onReplyAdded)
        }
    }
}

@Composable
fun InteractorsSidebarContent(post: Post) {
    val interactors = remember(post) { post.comments.take(3) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((-12).dp)) {
        interactors.forEach { interactor ->
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).border(1.5.dp, XBlack, CircleShape).background(XDarkGray), contentAlignment = Alignment.Center) {
                if (interactor.authorImageUri != null) AsyncImage(model = interactor.authorImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(interactor.author.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        val totalCount = post.validates + post.loves + post.comments.size
        if (totalCount > interactors.size || totalCount > 0) {
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(XBlue.copy(alpha = 0.9f)).border(1.5.dp, XBlack, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (totalCount > 99) "99+" else "$totalCount", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun InteractionsDialog(post: Post, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 450.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = XBlack), border = androidx.compose.foundation.BorderStroke(0.5.dp, XBorderGray)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Interactions au Bled", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (post.validates > 0) {
                        item { InteractionTypeHeader("Ont validé (👍)", Icons.Default.ThumbUp, XBlue) }
                        items(post.validates.coerceAtMost(3)) { InteractionRow("Citoyen", "A donné sa validation") }
                    }
                    if (post.comments.isNotEmpty()) {
                        item { InteractionTypeHeader("Ont commenté (💬)", Icons.Outlined.ModeComment, Color.White) }
                        items(post.comments) { InteractionRow(it.author, "Dit : \"${it.text.take(40)}...\"", it.authorImageUri) }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionTypeHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = XTextGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InteractionRow(name: String, action: String, imageUri: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(XDarkGray)) {
            if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(action, style = MaterialTheme.typography.labelSmall, color = XTextGray)
        }
    }
}

@Composable
fun ReactionButton(icon: ImageVector, count: Int, label: String, onClick: () -> Unit, activeColor: Color = Color.Gray, isActive: Boolean = false, iconButtonSize: Dp = 40.dp, iconSize: Dp = 22.dp) {
    val scale by animateFloatAsState(targetValue = if (isActive) 1.15f else 1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f), label = "reaction_scale")
    Row(modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 2.dp).scale(scale), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(iconButtonSize)) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(iconSize), tint = if (isActive) activeColor else XTextGray)
        }
        if (count > 0) {
            Text(text = "$count", style = MaterialTheme.typography.labelMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) activeColor else XTextGray)
        }
    }
}

@Composable
fun CommentSheetContent(comments: List<Comment>, currentUserHandle: String = "", onCommentValidate: (String) -> Unit = {}, onCommentLove: (String) -> Unit = {}, onAddComment: (String) -> Unit, onAddReply: (commentId: String, replyText: String) -> Unit = { _, _ -> }) {
    var newCommentText by remember { mutableStateOf("") }
    var replyToCommentId by remember { mutableStateOf<String?>(null) }
    var timeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); timeTick++ } }
    
    Column(modifier = Modifier.fillMaxHeight(0.85f).padding(16.dp)) {
        Text("Commentaires", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
            if (comments.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("Soyez le premier à commenter !", color = XTextGray) } }
            }
            items(comments) { comment ->
                CommentItemWithReplies(
                    comment = comment,
                    currentUserHandle = currentUserHandle,
                    timeTick = timeTick,
                    onValidate = { onCommentValidate(comment.id) },
                    onLove = { onCommentLove(comment.id) },
                    onReply = { replyToCommentId = comment.id },
                    onReplyValidate = onCommentValidate,
                    onReplyLove = onCommentLove
                )
            }
        }
        
        // Reply info bar
        if (replyToCommentId != null) {
            val replyTarget = comments.find { it.id == replyToCommentId }
            if (replyTarget != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(XBlue.copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Répondre à ${replyTarget.author}",
                        style = MaterialTheme.typography.labelMedium,
                        color = XBlue,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { replyToCommentId = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Annuler", tint = XBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        
        // Comment input
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (replyToCommentId != null) "Écrire une réponse..." else "Ajouter un commentaire...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        if (replyToCommentId != null) {
                            onAddReply(replyToCommentId!!, newCommentText)
                        } else {
                            onAddComment(newCommentText)
                        }
                        newCommentText = ""
                        replyToCommentId = null
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = XBlue)
            }
        }
    }
}

@Composable
private fun CommentItemWithReplies(
    comment: Comment,
    currentUserHandle: String,
    timeTick: Int,
    onValidate: () -> Unit,
    onLove: () -> Unit,
    onReply: () -> Unit,
    onReplyValidate: (String) -> Unit,
    onReplyLove: (String) -> Unit
) {
    val ownComment = currentUserHandle.isNotBlank() && handlesEqual(comment.authorHandle, currentUserHandle)
    var showReplies by remember { mutableStateOf(comment.replies.isNotEmpty()) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Main comment
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                if (comment.authorImageUri != null) AsyncImage(model = comment.authorImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val cTime = remember(comment.time, comment.id, timeTick) { formatRelativeTimeFr(comment.time) }
                Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(cTime, style = MaterialTheme.typography.labelSmall, color = XTextGray)
                
                // Reactions and reply button
                if (!ownComment && currentUserHandle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ReactionButton(icon = if (comment.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, count = comment.validates, label = "Je valide", onClick = onValidate, activeColor = XBlue, isActive = comment.isValidatedByMe, iconButtonSize = 34.dp, iconSize = 18.dp)
                        ReactionButton(icon = if (comment.isLovedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, count = comment.loves, label = "J'adore", onClick = onLove, activeColor = Color.Red, isActive = comment.isLovedByMe, iconButtonSize = 34.dp, iconSize = 18.dp)
                        TextButton(onClick = onReply, modifier = Modifier.height(34.dp)) {
                            Text("Répondre", style = MaterialTheme.typography.labelSmall, color = XBlue)
                        }
                    }
                }
                
                // Show replies toggle
                if (comment.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showReplies = !showReplies }, modifier = Modifier.height(28.dp).padding(0.dp)) {
                        Text(
                            if (showReplies) "▼ Masquer les ${comment.replies.size} réponse${if (comment.replies.size > 1) "s" else ""}" else "▶ Afficher les ${comment.replies.size} réponse${if (comment.replies.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = XBlue
                        )
                    }
                }
            }
        }
        
        // Replies (nested comments)
        if (showReplies && comment.replies.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 40.dp)) {
                comment.replies.forEach { reply ->
                    val ownReply = currentUserHandle.isNotBlank() && handlesEqual(reply.authorHandle, currentUserHandle)
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                            if (reply.authorImageUri != null) AsyncImage(model = reply.authorImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val rTime = remember(reply.time, reply.id, timeTick) { formatRelativeTimeFr(reply.time) }
                            Text(reply.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            Text(reply.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Text(rTime, style = MaterialTheme.typography.labelSmall, color = XTextGray)
                            
                            if (!ownReply && currentUserHandle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ReactionButton(icon = if (reply.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp, count = reply.validates, label = "Je valide", onClick = { onReplyValidate(reply.id) }, activeColor = XBlue, isActive = reply.isValidatedByMe, iconButtonSize = 28.dp, iconSize = 14.dp)
                                    ReactionButton(icon = if (reply.isLovedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, count = reply.loves, label = "J'adore", onClick = { onReplyLove(reply.id) }, activeColor = Color.Red, isActive = reply.isLovedByMe, iconButtonSize = 28.dp, iconSize = 14.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun buildExoPlayerForStoredVideo(context: Context, videoUrl: String): ExoPlayer {
    val okHttpClient = OkHttpClient.Builder().addInterceptor { chain -> val request = chain.request().newBuilder().addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}").build(); chain.proceed(request) }.build()
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUrl))
    return ExoPlayer.Builder(context).build().apply { setMediaSource(mediaSource); repeatMode = Player.REPEAT_MODE_ONE; prepare() }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun VideoFeedPlayer(thumbnailUrl: String?, videoUrl: String, isActive: Boolean, modifier: Modifier = Modifier, onTapOpenPlaylist: () -> Unit = {}, videoVolumeViewModel: VideoVolumeViewModel? = null) {
    val context = LocalContext.current
    var localMuted by remember { mutableStateOf(true) }
    val isMuted = videoVolumeViewModel?.isMuted ?: localMuted
    val player = remember(videoUrl) { buildExoPlayerForStoredVideo(context, videoUrl) }
    var videoRatio by remember { mutableStateOf(FeedMediaAspectRatio) }
    DisposableEffect(player) {
        val listener = object : Player.Listener { override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.width > 0 && videoSize.height > 0) { videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat() } } }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(isActive) { if (isActive) { player.playWhenReady = true; player.play() } else { player.playWhenReady = false; player.pause() } }
    LaunchedEffect(isMuted) { player.volume = if (isMuted) 0f else 1f }
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(0.5.dp, XBorderGray, RoundedCornerShape(16.dp)).aspectRatio(videoRatio.coerceIn(0.7f, 1.8f)).background(XDarkGray).clickable { onTapOpenPlaylist() }) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) } }, modifier = Modifier.fillMaxSize(), onRelease = { view -> view.player = null })
        if (!thumbnailUrl.isNullOrBlank()) { AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = if (isActive) 0f else 1f) }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.20f)))))
        IconButton(onClick = { if (videoVolumeViewModel != null) { videoVolumeViewModel.toggleMute() } else { localMuted = !localMuted } }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(42.dp).background(XBlue.copy(alpha = 0.22f), CircleShape).border(1.dp, XBlue.copy(alpha = 0.5f), CircleShape)) { Icon(imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = if (isMuted) "Activer le son" else "Couper le son", tint = Color.White, modifier = Modifier.size(22.dp)) }
    }
}

internal fun releaseExoPlayer(player: ExoPlayer) {
    player.pause()
    player.stop()
    player.clearVideoSurface()
    player.release()
}

@Composable
fun PostTypeBadge(type: String) {
    if (type == "classique") return
    data class TypeInfo(val emoji: String, val label: String, val color: androidx.compose.ui.graphics.Color)
    val info = when (type) {
        "duel"     -> TypeInfo("🆚", "Duel",     Color(0xFFD32F2F))
        "sondage"  -> TypeInfo("📊", "Sondage",  Color(0xFF1565C0))
        "vote"     -> TypeInfo("🗳️", "Vote",     Color(0xFF6A1B9A))
        "concours" -> TypeInfo("🏆", "Concours", Color(0xFFF57F17))
        "enquete"  -> TypeInfo("📋", "Enquête",  Color(0xFF00695C))
        else       -> TypeInfo("📝", type,       Color.Gray)
    }
    Row(modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp)) {
        Surface(color = info.color, shape = RoundedCornerShape(4.dp)) {
            Text("${info.emoji} ${info.label}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PostTypeContent(post: Post, myPollVoteIndex: Int?, onPollVote: (Int) -> Unit) {
    when (post.postType) {
        "duel" -> if (post.pollOptions.size >= 2) DuelContent(options = post.pollOptions, voteCounts = post.pollVoteCounts, myVoteIndex = myPollVoteIndex, onVote = onPollVote, optionImages = post.pollOptionImages)
        "sondage", "enquete" -> if (post.pollOptions.isNotEmpty()) PollContent(options = post.pollOptions, voteCounts = post.pollVoteCounts, myVoteIndex = myPollVoteIndex, onVote = onPollVote, isBinary = false, goalCount = post.goalCount)
        "vote" -> if (post.pollOptions.isNotEmpty()) PollContent(options = post.pollOptions, voteCounts = post.pollVoteCounts, myVoteIndex = myPollVoteIndex, onVote = onPollVote, isBinary = true, optionImages = post.pollOptionImages, goalCount = post.goalCount)
        "concours" -> ConcoursBadge()
    }
}

@Composable
fun DuelContent(options: List<String>, voteCounts: List<Int>, myVoteIndex: Int?, onVote: (Int) -> Unit, optionImages: List<String> = emptyList()) {
    val totalVotes = voteCounts.sum()
    val hasVoted = myVoteIndex != null
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            DuelOptionCard(label = options.getOrElse(0) { "Option A" }, image = optionImages.getOrNull(0), votes = voteCounts.getOrElse(0) { 0 }, totalVotes = totalVotes, isSelected = myVoteIndex == 0, hasVoted = hasVoted, onClick = { onVote(0) }, modifier = Modifier.weight(1f))
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFD32F2F), tonalElevation = 4.dp) { Box(contentAlignment = Alignment.Center) { Text("VS", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) } }
            DuelOptionCard(label = options.getOrElse(1) { "Option B" }, image = imageUriToModel(optionImages.getOrNull(1)), votes = voteCounts.getOrElse(1) { 0 }, totalVotes = totalVotes, isSelected = myVoteIndex == 1, hasVoted = hasVoted, onClick = { onVote(1) }, modifier = Modifier.weight(1f))
        }
        if (totalVotes > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp), tint = XTextGray)
                Spacer(Modifier.width(4.dp))
                Text("$totalVotes citoyens ont tranché", style = MaterialTheme.typography.labelSmall, color = XTextGray)
            }
        }
    }
}

private fun imageUriToModel(uri: String?): Any? = if (uri.isNullOrBlank()) null else uri

@Composable
private fun DuelOptionCard(label: String, image: Any?, votes: Int, totalVotes: Int, isSelected: Boolean, hasVoted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pct = if (totalVotes > 0) (votes * 100 / totalVotes) else 0
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) XBlue.copy(alpha = 0.15f) else XDarkGray).border(width = if (isSelected) 2.dp else 0.5.dp, color = if (isSelected) XBlue else XBorderGray, shape = RoundedCornerShape(12.dp)).clickable(enabled = !hasVoted, onClick = onClick)) {
        Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
            if (image != null) AsyncImage(model = image, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text(label.take(1), style = MaterialTheme.typography.headlineLarge, color = Color.White.copy(alpha = 0.3f)) }
            if (hasVoted) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Text("$pct%", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) }
        }
        Text(text = label, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) XBlue else Color.White, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun PollContent(options: List<String>, voteCounts: List<Int>, myVoteIndex: Int?, onVote: (Int) -> Unit, isBinary: Boolean = false, optionImages: List<String> = emptyList(), goalCount: Int = 0) {
    val totalVotes = voteCounts.sum()
    val hasVoted = myVoteIndex != null
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (totalVotes > 0 || goalCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                val score = if (totalVotes > 0) "⭐ 4.${(totalVotes % 10).coerceAtLeast(1)}" else "Nouveau"
                Surface(color = Color(0xFFFFA000).copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) { Text(score, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                Text(if (goalCount > 0) "$totalVotes / $goalCount participation${if (goalCount > 1) "s" else ""}" else "$totalVotes vote${if (totalVotes > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isBinary) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, option ->
                    val votes = voteCounts.getOrElse(index) { 0 }
                    val pct = when { goalCount > 0 -> (votes * 100 / goalCount).coerceAtMost(100); totalVotes > 0 -> votes * 100 / totalVotes; else -> 0 }
                    val isSelected = myVoteIndex == index
                    val imgUrl = optionImages.getOrNull(index)?.takeIf { it.isNotBlank() }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) XBlue else MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = !hasVoted) { onVote(index) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { if (imgUrl != null) AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop) ; Spacer(Modifier.height(6.dp)); Text(option, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); if (hasVoted) Text("$pct%", color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) } }
                }
            }
        } else {
            options.forEachIndexed { index, option ->
                val votes = voteCounts.getOrElse(index) { 0 }
                val pct = when { goalCount > 0 -> (votes * 100 / goalCount).coerceAtMost(100); totalVotes > 0 -> votes * 100 / totalVotes; else -> 0 }
                val isSelected = myVoteIndex == index
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isSelected) XBlue.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = !hasVoted) { onVote(index) }.padding(horizontal = 12.dp, vertical = 10.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) { if (isSelected) { Text("✓", color = XBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)) }; Text(option, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2) }; if (hasVoted) Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) XBlue else MaterialTheme.colorScheme.onSurfaceVariant) }; if (hasVoted) { Spacer(Modifier.height(4.dp)); LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)), color = if (isSelected) XBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), trackColor = Color.Transparent) } }
                if (index < options.size - 1) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun DeadlineChip(deadlineAt: Long?, goalCount: Int) {
    if (deadlineAt == null && goalCount == 0) return
    val remaining = if (deadlineAt != null) deadlineAt - System.currentTimeMillis() else -1L
    val isOver = deadlineAt != null && remaining <= 0
    val timeText = when {
        deadlineAt == null -> null
        remaining <= 0 -> "Clôturé"
        remaining < 3_600_000L -> "${remaining / 60_000}min"
        remaining < 86_400_000L -> "${remaining / 3_600_000}h ${(remaining % 3_600_000) / 60_000}min"
        else -> "${remaining / 86_400_000}j ${(remaining % 86_400_000) / 3_600_000}h"
    }
    val chipColor = when {
        isOver -> Color.Gray
        remaining in 0..86_399_999L -> Color(0xFFE53935)
        remaining in 86_400_000L..259_199_999L -> Color(0xFFF57F17)
        else -> XBlue
    }
    Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (timeText != null) {
            Surface(color = chipColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.AccessTime, null, tint = chipColor, modifier = Modifier.size(11.dp))
                    Text(if (isOver) "Clôturé" else "⏳ $timeText", style = MaterialTheme.typography.labelSmall, color = chipColor, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (goalCount > 0) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                Text("🎯 Objectif : $goalCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ConcoursBadge() { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("🏆", style = MaterialTheme.typography.bodyLarge); Text("Concours en cours — participe dans les commentaires !", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF57F17)) } }
