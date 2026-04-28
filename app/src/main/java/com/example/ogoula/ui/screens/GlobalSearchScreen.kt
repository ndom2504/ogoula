package com.example.ogoula.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.data.CommunityRepository
import com.example.ogoula.data.PostRepository
import com.example.ogoula.data.UserRepository
import com.example.ogoula.ui.Community
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.components.Post
import com.example.ogoula.ui.components.formatRelativeTimeFr
import com.example.ogoula.ui.theme.GreenGabo
import kotlinx.coroutines.delay

private fun Post.hasVideo(): Boolean =
    !videoUrl.isNullOrBlank() || imageUrls.any { it.startsWith("video:") }

private fun postMatchesQuery(post: Post, q: String): Boolean {
    if (q.isBlank()) return false
    return post.content.contains(q, ignoreCase = true) ||
        post.author.contains(q, ignoreCase = true) ||
        post.handle.contains(q, ignoreCase = true)
}

private data class UserHit(val handle: String, val name: String, val imageUri: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onOpenVideoPlaylist: (String) -> Unit,
    onNavigateToCommunityTab: () -> Unit = {},
) {
    val posts by postViewModel.posts.collectAsState()
    val followedUsers = postViewModel.followedUsers
    val communities = postViewModel.communities
    var query by remember { mutableStateOf("") }
    var previewPost by remember { mutableStateOf<Post?>(null) }

    val userRepo = remember { UserRepository() }
    val postRepo = remember { PostRepository() }
    val communityRepo = remember { CommunityRepository() }

    var remoteUserHits by remember { mutableStateOf<List<UserHit>>(emptyList()) }
    var remotePublicationPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var remoteVideoPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var remoteCommunityList by remember { mutableStateOf<List<Community>>(emptyList()) }
    var isSearchingRemote by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val qTrim = query.trim()
    val q = qTrim.lowercase()

    LaunchedEffect(qTrim) {
        if (qTrim.isEmpty()) {
            remoteUserHits = emptyList()
            remotePublicationPosts = emptyList()
            remoteVideoPosts = emptyList()
            remoteCommunityList = emptyList()
            searchError = null
            return@LaunchedEffect
        }
        delay(400)
        isSearchingRemote = true
        searchError = null
        try {
            val profiles = userRepo.searchProfilesByText(qTrim)
            remoteUserHits = profiles.mapNotNull { prof ->
                if (prof.alias.isBlank()) null
                else UserHit(
                    prof.alias,
                    "${prof.firstName} ${prof.lastName}".trim().ifEmpty { prof.alias },
                    prof.profileImageUri,
                )
            }
            val remotePosts = postRepo.searchPostsByText(qTrim)
            remotePublicationPosts = remotePosts.filter { !it.hasVideo() }
            remoteVideoPosts = remotePosts.filter { it.hasVideo() }
            remoteCommunityList = communityRepo.searchCommunitiesByText(qTrim)
        } catch (e: Exception) {
            searchError = e.message ?: "Erreur réseau"
        } finally {
            isSearchingRemote = false
        }
    }

    val userHits = remember(posts, followedUsers) {
        val map = linkedMapOf<String, UserHit>()
        posts.forEach { p ->
            if (p.handle.isBlank()) return@forEach
            val key = p.handle.lowercase()
            if (key !in map) map[key] = UserHit(p.handle, p.author, p.authorImageUri)
        }
        postViewModel.getFollowedUsersInfo().forEach { f ->
            val key = f.handle.lowercase()
            if (key !in map) map[key] = UserHit(f.handle, f.name, f.imageUri)
        }
        map.values.toList()
    }

    val userResultsLocal = remember(q, userHits) {
        if (q.isEmpty()) emptyList()
        else userHits.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.handle.contains(q, ignoreCase = true)
        }.take(40)
    }

    val publicationResultsLocal = remember(q, posts) {
        if (q.isEmpty()) emptyList()
        else posts.filter { postMatchesQuery(it, q) && !it.hasVideo() }.take(35)
    }

    val videoResultsLocal = remember(q, posts) {
        if (q.isEmpty()) emptyList()
        else posts.filter { postMatchesQuery(it, q) && it.hasVideo() }.take(35)
    }

    val communityResultsLocal = remember(q, communities) {
        if (q.isEmpty()) emptyList()
        else communities.filter { c ->
            c.name.contains(q, ignoreCase = true) ||
                c.description.contains(q, ignoreCase = true)
        }.take(25)
    }

    val mergedUserResults = remember(q, userResultsLocal, remoteUserHits) {
        if (q.isEmpty()) emptyList()
        else buildList {
            val seen = linkedSetOf<String>()
            for (u in userResultsLocal) {
                if (seen.add(u.handle.lowercase())) add(u)
            }
            for (u in remoteUserHits) {
                if (seen.add(u.handle.lowercase())) add(u)
            }
        }
    }

    val mergedPublications = remember(q, publicationResultsLocal, remotePublicationPosts) {
        if (q.isEmpty()) emptyList()
        else (publicationResultsLocal + remotePublicationPosts).distinctBy { it.id }
    }

    val mergedVideos = remember(q, videoResultsLocal, remoteVideoPosts) {
        if (q.isEmpty()) emptyList()
        else (videoResultsLocal + remoteVideoPosts).distinctBy { it.id }
    }

    val mergedCommunities = remember(q, communityResultsLocal, remoteCommunityList) {
        if (q.isEmpty()) emptyList()
        else (communityResultsLocal + remoteCommunityList).distinctBy { it.id }
    }

    val hasAnyResult =
        mergedUserResults.isNotEmpty() || mergedPublications.isNotEmpty() ||
            mergedVideos.isNotEmpty() || mergedCommunities.isNotEmpty()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text("Recherche Ogoula", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Utilisateurs, publications, vidéos, communautés…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            if (qTrim.isEmpty()) {
                item {
                    Text(
                        "Tapez un mot-clé : recherche dans le cache local et sur Supabase (profils, posts, communautés).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                searchError?.let { err ->
                    item {
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (isSearchingRemote) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                if (!isSearchingRemote && searchError == null && !hasAnyResult) {
                    item {
                        Text(
                            "Aucun résultat pour « $qTrim ».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (mergedUserResults.isNotEmpty()) {
                item {
                    SearchSectionTitle("Utilisateurs", Icons.Default.Person)
                }
                items(mergedUserResults, key = { it.handle }) { u ->
                    UserSearchRow(
                        hit = u,
                        onClick = { onOpenUserProfile(u.handle) },
                    )
                }
            }

            if (mergedPublications.isNotEmpty()) {
                item {
                    SearchSectionTitle("Publications", Icons.Default.TextSnippet)
                }
                items(mergedPublications, key = { "pub-${it.id}" }) { post ->
                    PostSearchRow(
                        post = post,
                        subtitle = "Publication · ${formatRelativeTimeFr(post.time)}",
                        onClick = { previewPost = post },
                    )
                }
            }

            if (mergedVideos.isNotEmpty()) {
                item {
                    SearchSectionTitle("Vidéos", Icons.Default.PlayCircle)
                }
                items(mergedVideos, key = { "vid-${it.id}" }) { post ->
                    PostSearchRow(
                        post = post,
                        subtitle = "Vidéo · ${formatRelativeTimeFr(post.time)}",
                        onClick = { onOpenVideoPlaylist(post.id) },
                    )
                }
            }

            if (mergedCommunities.isNotEmpty()) {
                item {
                    SearchSectionTitle("Communautés", Icons.Default.Groups)
                }
                items(mergedCommunities, key = { "com-${it.id}" }) { c ->
                    CommunitySearchRow(
                        community = c,
                        onClick = {
                            onNavigateToCommunityTab()
                            onBack()
                        },
                    )
                }
            }
        }
    }

    previewPost?.let { p ->
        AlertDialog(
            onDismissRequest = { previewPost = null },
            title = { Text(p.author, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        p.handle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(p.content)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (p.handle.isNotBlank()) onOpenUserProfile(p.handle)
                        previewPost = null
                    },
                ) {
                    Text("Voir le profil")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewPost = null }) {
                    Text("Fermer")
                }
            },
        )
    }
}

@Composable
private fun SearchSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = GreenGabo, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UserSearchRow(hit: UserHit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hit.imageUri != null) {
                AsyncImage(
                    model = hit.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(hit.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(hit.handle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PostSearchRow(
    post: Post,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(post.author, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                post.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommunitySearchRow(
    community: Community,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(community.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (community.description.isNotBlank()) {
                Text(
                    community.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
