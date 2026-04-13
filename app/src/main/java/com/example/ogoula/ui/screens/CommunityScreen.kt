package com.example.ogoula.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.ogoula.ui.theme.GreenGabo

private const val OGUALA_INVITE_URL = "https://www.ogoula.com/invite"
private const val OGUALA_WEB_URL = "https://www.ogoula.com/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(innerPadding: PaddingValues, viewModel: PostViewModel) {
    val notifications = viewModel.communityNotifications
    val communities = viewModel.communities
    var selectedCommunity by remember { mutableStateOf<Community?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Ma Communauté",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        items(communities) { community ->
            CommunityCard(
                community = community,
                onClick = { selectedCommunity = community }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = CardDefaults.cardColors(containerColor = GreenGabo.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(40.dp), tint = GreenGabo)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Activité du Bled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${notifications.size} nouvelles mises à jour", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Text("Dernières alertes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        items(notifications) { note -> NotificationItem(note) }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Bottom sheet paramètres de la communauté sélectionnée
    selectedCommunity?.let { community ->
        CommunitySettingsSheet(
            community = community,
            onDismiss = { selectedCommunity = null },
            onDelete = {
                viewModel.deleteCommunity(community.id)
                selectedCommunity = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySettingsSheet(
    community: Community,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // Un seul membre réel (toi / fondateur) ; les autres s’ajoutent via invitation.
    val members = remember(community.id, community.name, community.coverImageUri) {
        listOf(Triple(community.name, community.coverImageUri, true))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header communauté
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GreenGabo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (community.coverImageUri != null) {
                    AsyncImage(
                        model = community.coverImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = GreenGabo)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(community.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(community.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text("${members.size} membre(s)", style = MaterialTheme.typography.labelMedium, color = GreenGabo)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Membres ──────────────────────────────────────────────────────
            Text("Membres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            members.forEach { (name, imageUri, _) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenGabo),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$name (Vous)", fontWeight = FontWeight.Medium)
                        Text("Admin", style = MaterialTheme.typography.labelSmall, color = GreenGabo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val text =
                        "Rejoins la communauté « ${community.name} » sur Ogoula — réseau pour nos cultures et le lien entre nous. " +
                            "Télécharge l’app : $OGUALA_INVITE_URL — infos : $OGUALA_WEB_URL"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Inviter des membres"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Inviter des membres")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Paramètres ───────────────────────────────────────────────────
            Text("Paramètres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Notifications de la communauté") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingContent = {
                    var enabled by remember { mutableStateOf(true) }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            )
            ListItem(
                headlineContent = { Text("Communauté publique") },
                leadingContent = { Icon(Icons.Default.Public, contentDescription = null) },
                trailingContent = {
                    var isPublic by remember { mutableStateOf(true) }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            )
            ListItem(
                headlineContent = { Text("Modération des posts") },
                supportingContent = { Text("Approuver chaque post avant publication") },
                leadingContent = { Icon(Icons.Default.Shield, contentDescription = null) },
                trailingContent = {
                    var modEnabled by remember { mutableStateOf(false) }
                    Switch(checked = modEnabled, onCheckedChange = { modEnabled = it })
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer la communauté", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la communauté ?") },
            text = { Text("Cette action est irréversible. Tous les membres seront retirés.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun CommunityCard(community: Community, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp).background(GreenGabo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (community.coverImageUri != null) {
                    AsyncImage(
                        model = community.coverImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = GreenGabo.copy(alpha = 0.5f))
                }
            }
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(community.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(community.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                    Text("${community.memberCount} membre(s)", style = MaterialTheme.typography.labelSmall, color = GreenGabo)
                }
                Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun NotificationItem(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(GreenGabo, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
