package com.example.ogoula.ui.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.ogoula.app.BuildConfig
import okhttp3.OkHttpClient
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.ogoula.ui.theme.GreenGabo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Secondes UNIX → ms si la valeur ressemble à un timestamp en secondes (PostgREST / JSON). */
internal fun normalizePostTimeEpochMillis(raw: Long): Long {
    if (raw <= 0L) return raw
    return if (raw < 1_000_000_000_000L) raw * 1000L else raw
}

/** Compare deux handles (@alias ou alias), insensible à la casse. */
internal fun handlesEqual(a: String, b: String): Boolean {
    fun norm(s: String) = s.trim().lowercase().removePrefix("@").let { "@$it" }
    if (a.isBlank() || b.isBlank()) return false
    return norm(a) == norm(b)
}

/** Libellé court en français (fil d’actualité). */
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
        daysBetween < 7 -> "il y a $daysBetween j"
        daysBetween < 30 -> "il y a ${daysBetween / 7} sem."
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
    /** Handle du commentateur (ex. @jp_ndong) — sert au score et aux réactions « pas sur soi-même ». */
    @SerialName("author_handle") val authorHandle: String = "",
    val validates: Int = 0,
    val loves: Int = 0,
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
    val loves: Int = 0,
    val comments: List<Comment> = emptyList(),
    @SerialName("is_validated_by_me") val isValidatedByMe: Boolean = false,
    @SerialName("is_loved_by_me") val isLovedByMe: Boolean = false,
    @SerialName("is_community_post") val isCommunityPost: Boolean = false,
    @SerialName("author_image_uri") val authorImageUri: String? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null
)

val samplePosts = listOf(
    Post("1", "Jean-Pierre", "@jp_ndong", "Le ndole de midi était incroyable ! #Cuisine241", System.currentTimeMillis(), 24, 12, listOf(Comment(id = "1", author = "Moussa", text = "Vrai ça !", time = System.currentTimeMillis(), authorHandle = "@moussa"))),
    Post("2", "Sarah", "@sarah_gabo", "Magnifique coucher de soleil sur la Pointe-Denis. #GabonEvasion", System.currentTimeMillis(), 156, 89, emptyList()),
    Post("3", "Marc", "@marc_tech", "L'écosystème tech au Gabon est en pleine ébullition. #DigitalGabon", System.currentTimeMillis(), 45, 10, emptyList())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItem(
    post: Post,
    isFollowed: Boolean = false,
    showFollowButton: Boolean = true,
    currentUserHandle: String = "",
    /** Lecture inline type réseau social (boucle, mute par défaut) ; sinon vignette + lecture au tap. */
    useFeedVideoAutoplay: Boolean = false,
    /** Post le plus proche du centre du viewport — seul ce lecteur joue. */
    feedVideoActive: Boolean = false,
    onValidate: () -> Unit,
    onLove: () -> Unit,
    onCommentAdded: (String) -> Unit,
    onCommentValidate: (String) -> Unit = {},
    onCommentLove: (String) -> Unit = {},
    onShare: () -> Unit,
    onToggleFollow: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    /** Ouverture du mode « Playlist » plein écran (toutes les vidéos). */
    onVideoTapOpenPlaylist: (() -> Unit)? = null,
) {
    var showCommentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(post.content) }
    val isOwnPost = currentUserHandle.isNotEmpty() && handlesEqual(post.handle, currentUserHandle)

    var timeTick by remember(post.id) { mutableIntStateOf(0) }
    LaunchedEffect(post.id, post.time) {
        while (true) {
            delay(30_000)
            timeTick++
        }
    }
    val timeLabel = remember(post.time, timeTick) { formatRelativeTimeFr(post.time) }

    // Même palette dégradée que VideoPlaylistScreen (cartes « post ordinaire » hors communauté).
    val cardBackgroundBrush = if (post.isCommunityPost) {
        Brush.horizontalGradient(listOf(GreenGabo.copy(alpha = 0.14f), GreenGabo.copy(alpha = 0.14f)))
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF0D0D12), Color(0xFF1A1520), Color(0xFF0D3D2E)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBackgroundBrush)
    ) {
        if (post.isCommunityPost) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)) {
                Surface(color = GreenGabo, shape = CircleShape) {
                    Text(
                        "COMMUNAUTÉ",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        // ── En-tête : avatar | infos auteur (cliquable → profil) | bouton action ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GreenGabo.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.authorImageUri)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.author.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " · $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Bouton "Suivre" (si applicable)
            if (showFollowButton && !isOwnPost) {
                IconButton(onClick = onToggleFollow, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        contentDescription = "Suivre",
                        tint = if (isFollowed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Menu ⋮ pour les posts de l'utilisateur connecté
            if (isOwnPost) {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                editedContent = post.content
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete?.invoke()
                            }
                        )
                    }
                }
            }
        }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = post.content, modifier = Modifier.padding(bottom = 10.dp))
        } // fin Column intérieure (header + texte avec padding horizontal)

        // ── Médias FULL WIDTH — zéro padding latéral ──────────────────────
        val videoUrlFinal = post.videoUrl
            ?: post.imageUrls.find { it.startsWith("video:") }?.removePrefix("video:")
        val imageUrlsOnly = post.imageUrls.filter { !it.startsWith("video:") }

        if (videoUrlFinal != null) {
            if (useFeedVideoAutoplay) {
                VideoFeedPlayer(
                    thumbnailUrl = imageUrlsOnly.firstOrNull(),
                    videoUrl = videoUrlFinal,
                    isActive = feedVideoActive,
                    onTapOpenPlaylist = { onVideoTapOpenPlaylist?.invoke() },
                )
            } else {
                VideoThumbnailPlayer(
                    thumbnailUrl = imageUrlsOnly.firstOrNull(),
                    videoUrl = videoUrlFinal
                )
            }
        } else if (imageUrlsOnly.isNotEmpty()) {
            if (imageUrlsOnly.size == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .aspectRatio(4f / 5f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = imageUrlsOnly[0],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(imageUrlsOnly) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillParentMaxWidth(if (imageUrlsOnly.size == 2) 0.5f else 0.75f)
                                .heightIn(max = 320.dp)
                                .aspectRatio(3f / 4f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // ── Réactions ─────────────────────────────────────────────────────
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 0.5.dp,
            color = if (post.isCommunityPost) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                Color.White.copy(alpha = 0.18f)
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReactionButton(
                icon = if (post.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                count = post.validates,
                label = "Je valide",
                onClick = onValidate,
                activeColor = GreenGabo,
                isActive = post.isValidatedByMe
            )
            ReactionButton(
                icon = if (post.isLovedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = post.loves,
                label = "J'adore",
                onClick = onLove,
                activeColor = Color.Red,
                isActive = post.isLovedByMe
            )
            ReactionButton(
                icon = Icons.Default.ModeComment,
                count = post.comments.size,
                label = "Commente",
                onClick = { showCommentSheet = true }
            )
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Partager",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // ── Séparateur épais entre posts ───────────────────────────────────
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } // fin Column extérieure (full-width)

    // Dialog de modification du post
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modifier le post") },
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Contenu") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedContent.isNotBlank()) {
                            onEdit?.invoke(editedContent)
                            showEditDialog = false
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CommentSheetContent(
                comments = post.comments,
                currentUserHandle = currentUserHandle,
                onCommentValidate = onCommentValidate,
                onCommentLove = onCommentLove,
                onAddComment = { text -> onCommentAdded(text) }
            )
        }
    }
}

@Composable
fun CommentSheetContent(
    comments: List<Comment>,
    currentUserHandle: String = "",
    onCommentValidate: (String) -> Unit = {},
    onCommentLove: (String) -> Unit = {},
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    var timeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            timeTick++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
    ) {
        Text(
            "Commentaires",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (comments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Soyez le premier à commenter !",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(comments) { comment ->
                val ownComment =
                    currentUserHandle.isNotBlank() && handlesEqual(comment.authorHandle, currentUserHandle)
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (comment.authorImageUri != null) {
                            AsyncImage(
                                model = comment.authorImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val cTime = remember(comment.time, comment.id, timeTick) {
                            formatRelativeTimeFr(comment.time)
                        }
                        Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            cTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!ownComment && currentUserHandle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ReactionButton(
                                    icon = if (comment.isValidatedByMe) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                    count = comment.validates,
                                    label = "Je valide",
                                    onClick = { onCommentValidate(comment.id) },
                                    activeColor = GreenGabo,
                                    isActive = comment.isValidatedByMe,
                                    iconButtonSize = 34.dp,
                                    iconSize = 18.dp
                                )
                                ReactionButton(
                                    icon = if (comment.isLovedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    count = comment.loves,
                                    label = "J'adore",
                                    onClick = { onCommentLove(comment.id) },
                                    activeColor = Color.Red,
                                    isActive = comment.isLovedByMe,
                                    iconButtonSize = 34.dp,
                                    iconSize = 18.dp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ajouter un commentaire...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        onAddComment(newCommentText)
                        newCommentText = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = GreenGabo)
            }
        }
    }
}

@Composable
fun ReactionButton(
    icon: ImageVector,
    count: Int,
    label: String,
    onClick: () -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isActive: Boolean = false,
    iconButtonSize: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "reaction_scale"
    )
    // Icône + compteur uniquement — sans texte pour ne pas déborder en petits écrans
    Row(
        modifier = Modifier.scale(scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(iconButtonSize)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Lecteur vidéo ───────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
internal fun buildExoPlayerForStoredVideo(context: Context, videoUrl: String): ExoPlayer {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .build()
            chain.proceed(request)
        }
        .build()
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(videoUrl))
    return ExoPlayer.Builder(context).build().apply {
        setMediaSource(mediaSource)
        repeatMode = Player.REPEAT_MODE_ONE
        prepare()
    }
}

/**
 * Vidéo dans le fil : boucle, mute par défaut, son au tap, lecture seulement quand le post est au centre de l’écran.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoFeedPlayer(
    thumbnailUrl: String?,
    videoUrl: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onTapOpenPlaylist: () -> Unit = {},
) {
    val context = LocalContext.current
    var muted by remember { mutableStateOf(true) }

    LaunchedEffect(isActive) {
        if (!isActive) muted = true
    }

    val player = remember(videoUrl) { buildExoPlayerForStoredVideo(context, videoUrl) }

    LaunchedEffect(isActive) {
        if (isActive) {
            player.playWhenReady = true
            player.play()
        } else {
            player.playWhenReady = false
            player.pause()
        }
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(onTapOpenPlaylist) {
                    detectTapGestures(onTap = { onTapOpenPlaylist() })
                },
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { muted = !muted },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (muted) "Activer le son" else "Couper le son",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Aperçu cliquable en 16:9 avec bouton Play.
 * Si aucun thumbnail : placeholder gradient sombre avec icône caméra.
 */
@Composable
fun VideoThumbnailPlayer(thumbnailUrl: String?, videoUrl: String) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dégradé subtil bas → haut pour lisibilité du bouton Play
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )
        } else {
            // Placeholder quand pas de thumbnail
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Scène Studio",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Bouton Play central (cercle blanc)
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Lire la vidéo",
                modifier = Modifier.size(38.dp),
                tint = Color.Black
            )
        }

        // Badge "Vidéo" coin bas-gauche
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Vidéo", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (showDialog) {
        VideoPlayerDialog(videoUrl = videoUrl, onDismiss = { showDialog = false })
    }
}

/**
 * Lecteur plein écran adaptatif.
 * - usePlatformDefaultWidth=false : dialog occupe toute la largeur
 * - fillMaxSize sur AndroidView → ExoPlayer gère l'aspect ratio nativement (RESIZE_MODE_FIT)
 * - OkHttp avec headers Supabase pour vidéos privées
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val player = remember(videoUrl) {
        buildExoPlayerForStoredVideo(context, videoUrl).apply {
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)        // 90 % de l'écran → place pour tout format de vidéo
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        // FIT : letterbox/pillarbox selon le ratio réel de la vidéo
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()   // ExoPlayer occupe tout et s'adapte
            )

            // Bouton fermeture haut-droite
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
