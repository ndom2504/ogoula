package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.GreenGabo

@Composable
fun KongossaScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    onChatClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val posts by postViewModel.posts.collectAsState()
    val followedUsers = postViewModel.followedUsers

    // Contacts = utilisateurs suivis enrichis avec infos from posts
    val contacts = remember(followedUsers, posts) {
        postViewModel.getFollowedUsersInfo()
    }

    // Filtrer selon recherche
    val filteredContacts = contacts.filter {
        searchQuery.isBlank() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.handle.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Text(
            text = "Espace Kongossa",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Chercher un contact...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = CircleShape,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (contacts.isEmpty()) {
            // État vide — pas encore d'abonnements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(GreenGabo.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = GreenGabo,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = "Aucune conversation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Abonne-toi à des membres depuis le fil d'accueil pour les retrouver ici et leur envoyer un message.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Section : Abonnements (contacts suivis)
            if (filteredContacts.isNotEmpty()) {
                Text(
                    text = "Abonnements · ${contacts.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenGabo,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            LazyColumn {
                if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun résultat pour \"$searchQuery\"", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }

                items(filteredContacts) { user ->
                    KongossaContactItem(
                        name = user.name,
                        handle = user.handle,
                        imageUri = user.imageUri,
                        lastMessage = "Tap pour envoyer un message",
                        isOnline = false,
                        unreadCount = 0,
                        onClick = { onChatClick(user.name) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun KongossaContactItem(
    name: String,
    handle: String,
    imageUri: String?,
    lastMessage: String,
    isOnline: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(name, fontWeight = FontWeight.Bold)
        },
        supportingContent = {
            Text(
                text = lastMessage,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        overlineContent = {
            Text(handle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (unreadCount > 0) {
                    Badge(containerColor = GreenGabo) {
                        Text("$unreadCount", color = Color.White)
                    }
                }
            }
        },
        leadingContent = {
            Box(modifier = Modifier.size(52.dp)) {
                // Avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(BlueGabo),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                // Point vert "en ligne"
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .align(Alignment.BottomEnd)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
        }
    )
}

// Structure conservée pour ChatDetailScreen
data class ChatItem(
    val name: String,
    val lastMessage: String,
    val time: String,
    val isOnline: Boolean,
    val unreadCount: Int = 0
)
