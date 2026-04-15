package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.theme.GreenGabo

private fun inviteTextForKongossa(inviteeQuery: String): String {
    val who = inviteeQuery.trim().ifBlank { "toi" }
    return """
        Salut $who,

        Je voudrais poursuivre notre conversation sur Ogoula (espace Kongossa).
        Télécharge l’application Ogoula pour qu’on puisse échanger directement dans l’app.

        À bientôt !
    """.trimIndent()
}

@Composable
fun KongossaScreen(
    innerPadding: PaddingValues,
    postViewModel: PostViewModel,
    onChatClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val posts by postViewModel.posts.collectAsState()
    val followedUsers = postViewModel.followedUsers
    val context = LocalContext.current

    val contacts = remember(followedUsers, posts) {
        postViewModel.getFollowedUsersInfo()
    }

    val filteredContacts = contacts.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.handle.contains(searchQuery, ignoreCase = true)
    }

    val canInviteNonUser =
        searchQuery.isNotBlank() && filteredContacts.isEmpty()

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
            placeholder = { Text("Chercher un contact…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = CircleShape,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            contacts.isEmpty() && searchQuery.isBlank() -> {
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
                            text = "Abonne-toi à des membres depuis le fil d’accueil pour les retrouver ici, ou cherche un nom ci-dessus pour inviter quelqu’un sur Ogoula.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            contacts.isEmpty() && searchQuery.isNotBlank() -> {
                KongossaInviteCard(
                    searchQuery = searchQuery,
                    onInvite = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Invitation à rejoindre Ogoula")
                            putExtra(Intent.EXTRA_TEXT, inviteTextForKongossa(searchQuery))
                        }
                        context.startActivity(Intent.createChooser(send, "Inviter sur Ogoula"))
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {
                if (filteredContacts.isNotEmpty()) {
                    Text(
                        text = "Contacts — fais défiler",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenGabo,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredContacts, key = { it.handle }) { user ->
                            KongossaContactCarouselItem(
                                name = user.name,
                                handle = user.handle,
                                imageUri = user.imageUri,
                                onClick = { onChatClick(user.name) }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (canInviteNonUser) {
                    KongossaInviteCard(
                        searchQuery = searchQuery,
                        onInvite = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Invitation à rejoindre Ogoula")
                                putExtra(Intent.EXTRA_TEXT, inviteTextForKongossa(searchQuery))
                            }
                            context.startActivity(Intent.createChooser(send, "Inviter sur Ogoula"))
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Text(
                    text = "Abonnements · ${contacts.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenGabo,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn {
                    items(filteredContacts, key = { it.handle }) { user ->
                        KongossaContactItem(
                            name = user.name,
                            handle = user.handle,
                            imageUri = user.imageUri,
                            lastMessage = "Tape pour ouvrir la conversation",
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
}

@Composable
private fun KongossaInviteCard(
    searchQuery: String,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenGabo.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cette personne n’est pas dans tes abonnements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "« ${searchQuery.trim()} » ne correspond à aucun contact suivi. Tu peux lui envoyer une invitation à installer Ogoula pour continuer la conversation dans Kongossa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onInvite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Inviter à rejoindre Ogoula")
            }
        }
    }
}

@Composable
private fun KongossaContactCarouselItem(
    name: String,
    handle: String,
    imageUri: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(GreenGabo.copy(alpha = 0.88f)),
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
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = handle,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(GreenGabo.copy(alpha = 0.88f)),
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

data class ChatItem(
    val name: String,
    val lastMessage: String,
    val time: String,
    val isOnline: Boolean,
    val unreadCount: Int = 0
)
