package com.example.ogoula.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ogoula.ui.components.PostPublicationImagePreview
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.XBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PostTypeOption(
    val key: String,
    val emoji: String,
    val label: String,
    val color: Color,
    val placeholder: String,
    val publishLabel: String,
)

private val POST_TYPE_GRID = listOf(
    PostTypeOption("vote",     "🗳️", "Vote",     Color(0xFF6A1B9A), "Compare marques/produits/talents...",      "Lancer le vote"),
    PostTypeOption("duel",     "🆚", "Duel",     Color(0xFFD32F2F), "Quel est le meilleur ? (A vs B)",          "Lancer le duel"),
    PostTypeOption("sondage",  "📊", "Sondage",  Color(0xFF1565C0), "Pose ta question pour découvrir les tendances...", "Lancer le sondage"),
    PostTypeOption("enquete",  "📋", "Enquête",  Color(0xFF00695C), "Recueille l'avis du continent...",         "Lancer l'enquête"),
    PostTypeOption("concours", "🏆", "Concours", Color(0xFFF57F17), "Mets en avant les talents...",             "Lancer le concours"),
)

private val CLASSIQUE = PostTypeOption(
    key = "classique", emoji = "📝", label = "Classique",
    color = Color(0xFF37474F), placeholder = "Parle-nous des choses de notre bled...", publishLabel = "Publier",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    userViewModel: UserViewModel,
    onBack: () -> Unit,
    onPostCreated: (
        text: String,
        images: List<ByteArray>,
        postType: String,
        pollOptions: List<String>,
        voteOptionImageUris: List<Uri?>,
        goalCount: Int,
        deadlineDays: Int,
    ) -> Unit,
) {
    var postText by remember { mutableStateOf("") }
    val selectedImages = remember { mutableStateListOf<Uri>() }
    val pollOptions = remember { mutableStateListOf("", "") }
    val voteOptionImages = remember { mutableStateListOf<Uri?>(null, null) }
    var selectedType by remember { mutableStateOf<PostTypeOption?>(null) }
    var pickingForOptionIndex by remember { mutableStateOf<Int?>(null) }
    var goalCountText by remember { mutableStateOf("") }
    var deadlineDaysText by remember { mutableStateOf("") }
    
    // Nouveaux champs pour l'affiliation et les produits
    var affiliateLink by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    
    val profile = userViewModel.userProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPublishing by remember { mutableStateOf(false) }

    val effectiveType = selectedType ?: CLASSIQUE
    val showPollOptions = effectiveType.key in listOf("sondage", "vote", "duel")

    val canPublish = !isPublishing && postText.isNotBlank() &&
        (!showPollOptions || pollOptions.count { it.isNotBlank() } >= 2)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> selectedImages.addAll(uris) }
    )
    val optionImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            val idx = pickingForOptionIndex ?: return@rememberLauncherForActivityResult
            while (voteOptionImages.size <= idx) voteOptionImages.add(null)
            voteOptionImages[idx] = uri
            pickingForOptionIndex = null
        }
    )

    // Sync voteOptionImages size with pollOptions
    LaunchedEffect(pollOptions.size) {
        while (voteOptionImages.size < pollOptions.size) voteOptionImages.add(null)
        while (voteOptionImages.size > pollOptions.size) voteOptionImages.removeLastOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${effectiveType.emoji} ${effectiveType.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            scope.launch {
                                isPublishing = true
                                val byteArrays = withContext(Dispatchers.IO) {
                                    selectedImages.mapNotNull { uri ->
                                        try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                                        catch (e: Exception) { null }
                                    }
                                }
                                val filteredOptions = pollOptions.filter { it.isNotBlank() }
                                val optionUris = if (effectiveType.key in listOf("vote", "duel")) voteOptionImages.toList() else emptyList()
                                
                                // Enrichir le texte avec le lien et le prix si présents
                                val enrichedText = buildString {
                                    append(postText)
                                    if (affiliateLink.isNotBlank()) append("\n\n🔗 [Link]($affiliateLink)")
                                    if (productPrice.isNotBlank()) append("\n💰 Prix: $productPrice")
                                }

                                onPostCreated(
                                    enrichedText, byteArrays, effectiveType.key, filteredOptions, optionUris,
                                    goalCountText.toIntOrNull() ?: 0,
                                    deadlineDaysText.toIntOrNull() ?: 0,
                                )
                            }
                        },
                        enabled = canPublish,
                        colors = ButtonDefaults.buttonColors(containerColor = effectiveType.color),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(effectiveType.publishLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Profil auteur ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    if (profile.profileImageUri != null) {
                        AsyncImage(model = profile.profileImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    val displayName = if (profile.firstName.isNotEmpty()) "${profile.firstName} ${profile.lastName}" else "Moi"
                    Text(displayName, fontWeight = FontWeight.Bold)
                    if (selectedType != null) {
                        Text(effectiveType.label, style = MaterialTheme.typography.labelSmall, color = effectiveType.color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Champ texte ────────────────────────────────────────────────────
            TextField(
                value = postText,
                onValueChange = { postText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                placeholder = { Text(effectiveType.placeholder, color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            // ── Champs Affiliation / Prix ──────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = affiliateLink,
                    onValueChange = { affiliateLink = it },
                    placeholder = { Text("Lien produit (optionnel)", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XBlue)
                )
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = { productPrice = it },
                    placeholder = { Text("Prix", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XBlue)
                )
            }

            // ── Options sondage / vote / enquête / duel ───────────────────────
            AnimatedVisibility(visible = showPollOptions) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val optionLabel = when (effectiveType.key) {
                        "vote"    -> "Options de vote (min. 2)"
                        "duel"    -> "Duel de produits (2 options)"
                        "enquete" -> "Questions de l'enquête (min. 2, max. 4)"
                        else      -> "Options du sondage (min. 2, max. 4)"
                    }
                    Text(optionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = effectiveType.color, modifier = Modifier.padding(bottom = 8.dp))

                    pollOptions.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Image illustrative pour vote et duel
                            if (effectiveType.key in listOf("vote", "duel")) {
                                val imgUri = voteOptionImages.getOrNull(index)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, effectiveType.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            pickingForOptionIndex = index
                                            optionImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (imgUri != null) {
                                        AsyncImage(model = imgUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = effectiveType.color, modifier = Modifier.size(20.dp))
                                            Text("Photo", style = MaterialTheme.typography.labelSmall, color = effectiveType.color)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            OutlinedTextField(
                                value = option,
                                onValueChange = { pollOptions[index] = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Produit/Option ${index + 1}") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveType.color),
                            )
                            if (pollOptions.size > 2 && effectiveType.key != "duel") {
                                IconButton(onClick = {
                                    pollOptions.removeAt(index)
                                    if (voteOptionImages.size > index) voteOptionImages.removeAt(index)
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    if (pollOptions.size < 4 && effectiveType.key != "duel") {
                        TextButton(
                            onClick = { pollOptions.add(""); voteOptionImages.add(null) },
                            colors = ButtonDefaults.textButtonColors(contentColor = effectiveType.color),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter une option")
                        }
                    }
                }
            }

            // ── Images du post ─────────────────────────────────────────────────
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(selectedImages) { uri ->
                        Box(modifier = if (selectedImages.size == 1) Modifier.fillMaxWidth() else Modifier.widthIn(max = 180.dp)) {
                            PostPublicationImagePreview(imageModel = uri, modifier = Modifier.fillMaxWidth())
                            IconButton(
                                onClick = { selectedImages.remove(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Paramètres avancés (objectif + durée) ───────────────────────────────
            AnimatedVisibility(visible = selectedType != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "Paramètres optionnels",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = effectiveType.color,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = goalCountText,
                            onValueChange = { goalCountText = it.filter { c -> c.isDigit() } },
                            label = { Text("🎯 Objectif") },
                            placeholder = { Text("ex: 1000") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveType.color),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = deadlineDaysText,
                            onValueChange = { deadlineDaysText = it.filter { c -> c.isDigit() } },
                            label = { Text("⏱️ Durée (j)") },
                            placeholder = { Text("ex: 7") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveType.color),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Barre d'outils : photo + grille 3×2 de types ──────────────────
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Ajouter une photo", tint = GreenGabo)
                }
                Text("Photo", style = MaterialTheme.typography.labelSmall, color = GreenGabo)
            }

            // Grille 3 colonnes × 2 lignes
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                POST_TYPE_GRID.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { opt ->
                            val isSelected = opt.key == effectiveType.key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) opt.color else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        selectedType = if (isSelected) null else opt
                                        // reset poll options size
                                        if (selectedType?.key in listOf("sondage", "vote", "duel")) {
                                            while (pollOptions.size < 2) pollOptions.add("")
                                            while (voteOptionImages.size < 2) voteOptionImages.add(null)
                                        }
                                        if (selectedType?.key == "duel") {
                                            while (pollOptions.size > 2) {
                                                pollOptions.removeLast()
                                                voteOptionImages.removeLast()
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(opt.emoji, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        opt.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        // Remplir les cellules vides si la ligne est incomplète
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
