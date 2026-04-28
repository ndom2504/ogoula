package com.example.ogoula.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ogoula.data.Leader
import com.example.ogoula.data.Promise
import com.example.ogoula.ui.PromiseViewModel
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.OgoulaWhite

// ── Couleurs statuts ──────────────────────────────────────────────────────────

private val StatusTenu    = Color(0xFF2E7D32)
private val StatusEnCours = Color(0xFFF57C00)
private val StatusPromis  = Color(0xFF1565C0)
private val StatusRompu   = Color(0xFFC62828)

private fun statusColor(status: String) = when (status) {
    "tenu"     -> StatusTenu
    "en_cours" -> StatusEnCours
    "rompu"    -> StatusRompu
    else       -> StatusPromis
}

private fun statusLabel(status: String) = when (status) {
    "tenu"     -> "✅ Tenu"
    "en_cours" -> "🔄 En cours"
    "rompu"    -> "❌ Rompu"
    else       -> "🗣️ Promis"
}

// ── Écran principal : liste des leaders ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromiseTrackerScreen(
    viewModel: PromiseViewModel,
    innerPadding: PaddingValues = PaddingValues(),
    onLeaderClick: (Leader) -> Unit = {},
) {
    val leaders by viewModel.leaders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()

    val countries = remember(leaders) {
        leaders.map { it.country }.distinct().sorted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ── Hero banner ──────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(listOf(GreenGabo, GreenGabo.copy(alpha = 0.7f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OgoulaWhite.copy(alpha = 0.85f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tracker de Promesses",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OgoulaWhite,
                    )
                    Text(
                        "Tenus ou brisés — les citoyens jugent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OgoulaWhite.copy(alpha = 0.85f),
                    )
                }
            }
        }

        // ── Filtre pays ──────────────────────────────────────────────────────
        if (countries.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        CountryChip(
                            label = "Tous",
                            selected = selectedCountry == null,
                            onClick = { viewModel.loadLeadersByCountry(null) },
                        )
                    }
                    items(countries) { country ->
                        CountryChip(
                            label = country,
                            selected = selectedCountry == country,
                            onClick = { viewModel.loadLeadersByCountry(country) },
                        )
                    }
                }
            }
        }

        // ── Liste des leaders ─────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GreenGabo)
                }
            }
        } else if (leaders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Aucun dirigeant encore enregistré.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.loadLeaders() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenGabo),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(GreenGabo, GreenGabo))
                        ),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Actualiser")
                    }
                }
            }
        } else {
            items(leaders) { leader ->
                LeaderCard(leader = leader, onClick = { onLeaderClick(leader) })
            }
        }
    }
}

// ── Chip filtre pays ──────────────────────────────────────────────────────────

@Composable
private fun CountryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) GreenGabo else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg",
    )
    val textColor by animateColorAsState(
        if (selected) OgoulaWhite else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_text",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ── Carte leader ──────────────────────────────────────────────────────────────

@Composable
fun LeaderCard(leader: Leader, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GreenGabo.copy(alpha = 0.15f))
                    .border(2.dp, GreenGabo.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (leader.photoUrl != null) {
                    AsyncImage(
                        model = leader.photoUrl,
                        contentDescription = leader.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = GreenGabo, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(leader.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(leader.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(GreenGabo.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "🌍 ${leader.country}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenGabo,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = GreenGabo.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Écran détail leader : liste des promesses ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderDetailScreen(
    leader: Leader,
    viewModel: PromiseViewModel,
    innerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    val promises by viewModel.promises.collectAsState()
    val myVotes  by viewModel.myVotes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var filterStatus by remember { mutableStateOf<String?>(null) }

    val filtered = remember(promises, filterStatus) {
        if (filterStatus == null) promises else promises.filter { it.status == filterStatus }
    }

    LaunchedEffect(leader.id) {
        viewModel.loadPromisesForLeader(leader.id)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header dirigeant ─────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GreenGabo, GreenGabo.copy(alpha = 0.6f), Color.Transparent)))
                    .padding(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(OgoulaWhite.copy(alpha = 0.2f))
                            .border(2.dp, OgoulaWhite.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (leader.photoUrl != null) {
                            AsyncImage(
                                model = leader.photoUrl,
                                contentDescription = leader.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OgoulaWhite, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(leader.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = OgoulaWhite)
                        Text("${leader.role} · ${leader.country}", style = MaterialTheme.typography.bodyMedium, color = OgoulaWhite.copy(alpha = 0.85f))
                        if (!leader.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(leader.bio, style = MaterialTheme.typography.bodySmall, color = OgoulaWhite.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // ── Stats résumé ─────────────────────────────────────────────────────
        item {
            PromiseStats(promises = promises)
        }

        // ── Filtre statut ─────────────────────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val filters = listOf(null to "Toutes", "promis" to "🗣️ Promis", "en_cours" to "🔄 En cours", "tenu" to "✅ Tenus", "rompu" to "❌ Rompus")
                items(filters) { (value, label) ->
                    CountryChip(
                        label = label,
                        selected = filterStatus == value,
                        onClick = { filterStatus = value },
                    )
                }
            }
        }

        // ── Liste promesses ───────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenGabo)
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    "Aucune promesse dans cette catégorie.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(filtered) { promise ->
                PromiseCard(
                    promise = promise,
                    myVote = myVotes[promise.id],
                    onVote = { vote -> viewModel.castVote(promise.id, vote) },
                )
            }
        }
    }
}

// ── Résumé stats du leader ────────────────────────────────────────────────────

@Composable
private fun PromiseStats(promises: List<Promise>) {
    if (promises.isEmpty()) return
    val total     = promises.size
    val kept      = promises.count { it.status == "tenu" }
    val broken    = promises.count { it.status == "rompu" }
    val inProgress = promises.count { it.status == "en_cours" }
    val score     = if (total > 0) (kept * 100 / total) else 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bilan citoyen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = GreenGabo)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = StatusTenu,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip("$total Total", MaterialTheme.colorScheme.onSurfaceVariant)
                StatChip("$kept Tenus", StatusTenu)
                StatChip("$inProgress En cours", StatusEnCours)
                StatChip("$broken Rompus", StatusRompu)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
}

// ── Carte promesse ────────────────────────────────────────────────────────────

@Composable
fun PromiseCard(
    promise: Promise,
    myVote: String?,
    onVote: (String) -> Unit,
) {
    val totalVotes = promise.votesKept + promise.votesBroken
    val keptPct   = if (totalVotes > 0) promise.votesKept  * 100 / totalVotes else 0
    val brokenPct = if (totalVotes > 0) promise.votesBroken * 100 / totalVotes else 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Statut + catégorie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor(promise.status).copy(alpha = 0.13f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(statusLabel(promise.status), style = MaterialTheme.typography.labelSmall, color = statusColor(promise.status), fontWeight = FontWeight.Bold)
                }
                Text(promise.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            // Titre
            Text(promise.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)

            // Description
            if (!promise.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(promise.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            // Année
            if (promise.year != null && promise.year > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Année : ${promise.year}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))

            // Barre vote citoyen
            if (totalVotes > 0) {
                Text("Avis citoyen · $totalVotes votes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(StatusRompu.copy(alpha = 0.25f))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(keptPct / 100f).background(StatusTenu))
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("✅ $keptPct% tenus", style = MaterialTheme.typography.labelSmall, color = StatusTenu)
                    Text("❌ $brokenPct% rompus", style = MaterialTheme.typography.labelSmall, color = StatusRompu)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Boutons vote
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoteButton(
                    label = "✅ Tenu",
                    active = myVote == "tenu",
                    activeColor = StatusTenu,
                    modifier = Modifier.weight(1f),
                    onClick = { onVote("tenu") },
                )
                VoteButton(
                    label = "❌ Rompu",
                    active = myVote == "rompu",
                    activeColor = StatusRompu,
                    modifier = Modifier.weight(1f),
                    onClick = { onVote("rompu") },
                )
            }
        }
    }
}

@Composable
private fun VoteButton(label: String, active: Boolean, activeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        label = "vote_bg",
    )
    val textColor by animateColorAsState(
        if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "vote_text",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
