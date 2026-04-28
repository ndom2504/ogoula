package com.example.ogoula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ogoula.ui.UserProfile
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.PostViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Product Posts

    // Vérifier si l'utilisateur courant est admin
    val currentUser = userViewModel.userProfile
    val isAdmin = currentUser?.alias?.lowercase()?.contains("admin") == true ||
        currentUser?.userId?.isNotEmpty() == true // TODO: ajouter vérification email

    LaunchedEffect(Unit) {
        if (!isAdmin) {
            error = "Accès non autorisé"
            return@LaunchedEffect
        }
        
        loading = true
        error = null
        // TODO: Implémenter appel API admin via web service
        // profiles = adminRepository.getAllProfiles()
        loading = false
    }

    if (!isAdmin) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = "Accès refusé",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Accès refusé",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Cette fonctionnalité est réservée aux administrateurs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Retour")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Utilisateurs") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Posts Produits") }
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> UsersTab(loading, error, profiles)
                    1 -> ProductPostsTab()
                }
            }
        }
    }
}

@Composable
private fun UsersTab(loading: Boolean, error: String?, profiles: List<UserProfile>) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Erreur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(error ?: "")
            }
        }
    } else {
        Text(
            "Gestion des comptes utilisateurs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles) { profile ->
                UserProfileCard(profile = profile)
            }
        }
    }
}

@Composable
private fun UserProfileCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = profile.alias.takeIf { it.isNotBlank() } 
                                ?: "${profile.firstName} ${profile.lastName}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${profile.userId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badge de statut
                Surface(
                    color = when (profile.accountStatus?.lowercase()) {
                        "active" -> MaterialTheme.colorScheme.primaryContainer
                        "suspended" -> MaterialTheme.colorScheme.secondaryContainer
                        "banned" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = (profile.accountStatus ?: "unknown").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (profile.accountStatus?.lowercase()) {
                            "active" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "suspended" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "banned" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Détails de modération
            if (profile.accountStatus != "active" && profile.accountStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                profile.moderationNote?.let { note ->
                    Text(
                        "Motif: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                profile.suspendedUntil?.let { until ->
                    val suspensionText = try {
                        val endDate = Instant.parse(until)
                        if (endDate.isAfter(Instant.now())) {
                            val formatted = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH)
                                .withZone(ZoneId.systemDefault())
                                .format(endDate)
                            "Suspendu jusqu'au: $formatted"
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                    
                    suspensionText?.let { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions rapides
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TODO: Ajouter boutons d'action (suspendre, bannir, réactiver)
            }
        }
    }
}

@Composable
private fun ProductPostsTab() {
    var productUrl by remember { mutableStateOf("") }
    var productTitle by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productImage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val postViewModel: PostViewModel = viewModel()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Créer un Post Produit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // URL du produit
        OutlinedTextField(
            value = productUrl,
            onValueChange = { productUrl = it },
            label = { Text("URL du produit") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://www.nike.com/ca/fr/...") },
            singleLine = true
        )
        
        // Titre du produit
        OutlinedTextField(
            value = productTitle,
            onValueChange = { productTitle = it },
            label = { Text("Titre du produit") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex: Chaussure Air Force 1") },
            singleLine = true
        )
        
        // Prix du produit
        OutlinedTextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Prix") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex: 120$ CAD") },
            singleLine = true
        )
        
        // Image URL
        OutlinedTextField(
            value = productImage,
            onValueChange = { productImage = it },
            label = { Text("URL de l'image du produit") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://...") }
        )
        
        // Messages
        if (successMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f))
            ) {
                Text(
                    successMessage,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        if (errorMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // Bouton de création
        Button(
            onClick = {
                if (productUrl.isBlank() || productTitle.isBlank()) {
                    errorMessage = "Veuillez remplir l'URL et le titre du produit"
                    return@Button
                }
                isLoading = true
                errorMessage = ""
                successMessage = ""
                
                // Créer le post produit via le ViewModel
                postViewModel.createProductPost(
                    productUrl = productUrl,
                    productTitle = productTitle,
                    productPrice = productPrice,
                    productImage = productImage
                )
                
                isLoading = false
                successMessage = "Post produit créé avec succès!"
                productUrl = ""
                productTitle = ""
                productPrice = ""
                productImage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Créer le post produit")
            }
        }
    }
}
