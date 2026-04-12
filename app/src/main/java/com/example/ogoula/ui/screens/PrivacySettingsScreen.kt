package com.example.ogoula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.theme.GreenGabo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {

    // États locaux des paramètres
    var profilPublic        by remember { mutableStateOf(true) }
    var indexationRecherche by remember { mutableStateOf(true) }
    var messagesOuverts     by remember { mutableStateOf(false) }
    var afficherEnLigne     by remember { mutableStateOf(true) }
    var partageActivite     by remember { mutableStateOf(true) }
    var publicationVisible  by remember { mutableStateOf(true) }
    var mentionsAutorisees  by remember { mutableStateOf(true) }
    var analysePersonnalisee by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confidentialité", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Section : Visibilité du profil ──────────────────────────────
            item {
                PrivacySectionHeader(
                    icon = Icons.Default.Person,
                    title = "Visibilité du profil"
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Profil public",
                    subtitle = "Tout le monde peut voir votre profil",
                    checked = profilPublic,
                    onCheckedChange = { profilPublic = it }
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Indexation dans la recherche",
                    subtitle = "Votre profil apparaît dans les résultats de recherche",
                    checked = indexationRecherche,
                    onCheckedChange = { indexationRecherche = it }
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Afficher mon statut en ligne",
                    subtitle = "Les autres voient quand vous êtes actif",
                    checked = afficherEnLigne,
                    onCheckedChange = { afficherEnLigne = it }
                )
            }

            // ── Section : Publications & interactions ────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item {
                PrivacySectionHeader(
                    icon = Icons.Default.Feed,
                    title = "Publications & interactions"
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Publications visibles par tous",
                    subtitle = "Si désactivé, seuls vos abonnés voient vos posts",
                    checked = publicationVisible,
                    onCheckedChange = { publicationVisible = it }
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Partager mon activité",
                    subtitle = "Vos réactions et commentaires sont visibles",
                    checked = partageActivite,
                    onCheckedChange = { partageActivite = it }
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Autoriser les mentions",
                    subtitle = "Les autres peuvent vous mentionner dans leurs publications",
                    checked = mentionsAutorisees,
                    onCheckedChange = { mentionsAutorisees = it }
                )
            }

            // ── Section : Messagerie ─────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item {
                PrivacySectionHeader(
                    icon = Icons.Default.Message,
                    title = "Messagerie Kongossa"
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Messages ouverts à tous",
                    subtitle = "Si désactivé, seuls vos contacts peuvent vous écrire",
                    checked = messagesOuverts,
                    onCheckedChange = { messagesOuverts = it }
                )
            }

            // ── Section : Données & personnalisation ─────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item {
                PrivacySectionHeader(
                    icon = Icons.Default.BarChart,
                    title = "Données & personnalisation"
                )
            }
            item {
                PrivacyToggleItem(
                    title = "Personnalisation du fil",
                    subtitle = "Ogoula adapte votre fil selon vos interactions",
                    checked = analysePersonnalisee,
                    onCheckedChange = { analysePersonnalisee = it }
                )
            }

            // ── Note légale ──────────────────────────────────────────────────
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = GreenGabo,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = "Ces paramètres sont sauvegardés localement. " +
                                    "La synchronisation avec nos serveurs sera disponible dans une prochaine mise à jour.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = GreenGabo, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GreenGabo
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun PrivacyToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = GreenGabo, checkedTrackColor = GreenGabo.copy(alpha = 0.4f))
        )
    }
}
