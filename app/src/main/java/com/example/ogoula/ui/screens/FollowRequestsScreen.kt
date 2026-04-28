package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ogoula.data.FollowRequest
import com.example.ogoula.data.FollowRequestRepository
import com.example.ogoula.data.FollowRequestStatus
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.GreenGabo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowRequestsScreen(
    userViewModel: UserViewModel,
    onBack: () -> Unit,
    onNavigateToKongossa: (String) -> Unit = {}
) {
    val followRequestRepository = remember { FollowRequestRepository() }
    val scope = rememberCoroutineScope()
    
    var receivedRequests by remember { mutableStateOf<List<FollowRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Charger les demandes reçues
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val requests = followRequestRepository.getReceivedRequests(userViewModel.userProfile.userId)
            receivedRequests = requests.filter { it.status == FollowRequestStatus.PENDING }
        } catch (e: Exception) {
            // Gérer l'erreur
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demandes de suivi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (receivedRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Aucune demande de suivi",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Les nouvelles demandes apparaîtront ici",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(receivedRequests) { request ->
                    FollowRequestCard(
                        request = request,
                        onAccept = {
                            scope.launch {
                                val result = followRequestRepository.acceptRequest(request.id)
                                if (result.isSuccess) {
                                    // Recharger la liste
                                    val updatedRequests = followRequestRepository.getReceivedRequests(userViewModel.userProfile.userId)
                                    receivedRequests = updatedRequests.filter { it.status == FollowRequestStatus.PENDING }
                                    
                                    // Naviguer vers Kongossa pour ouvrir la conversation
                                    onNavigateToKongossa(request.senderId)
                                }
                            }
                        },
                        onReject = {
                            scope.launch {
                                val result = followRequestRepository.rejectRequest(request.id)
                                if (result.isSuccess) {
                                    // Recharger la liste
                                    val updatedRequests = followRequestRepository.getReceivedRequests(userViewModel.userProfile.userId)
                                    receivedRequests = updatedRequests.filter { it.status == FollowRequestStatus.PENDING }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowRequestCard(
    request: FollowRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo de profil
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                request.senderProfileImage?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } ?: run {
                    Text(
                        text = request.senderName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Informations
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.senderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "@${request.senderAlias}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "veut vous suivre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Boutons d'action
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Refuser",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Accepter",
                        tint = GreenGabo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
