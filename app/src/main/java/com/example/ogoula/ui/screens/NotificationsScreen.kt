package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.Notification
import com.example.ogoula.ui.NotificationType
import com.example.ogoula.ui.PostViewModel
import com.example.ogoula.ui.UserViewModel
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.XBlue
import com.example.ogoula.ui.theme.XBlack
import com.example.ogoula.ui.theme.XWhite
import com.example.ogoula.ui.theme.XTextGray
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.ogoula.data.FollowRequestRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: PostViewModel,
    userViewModel: UserViewModel,
    onBack: () -> Unit,
    onNavigateToKongossa: (String) -> Unit = {}
) {
    val notifications = viewModel.notifications

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = XBlack,
                    titleContentColor = XWhite,
                    navigationIconContentColor = XWhite
                ),
                title = { Text("Alertes & Oracles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markNotificationsAsRead() }) {
                        Text("Tout lire", color = XBlue)
                    }
                }
            )
        },
        containerColor = XBlack
    ) { innerPadding ->
        val scope = rememberCoroutineScope()
        val followRequestRepository = remember { FollowRequestRepository() }
        var followRequestsCount by remember { mutableStateOf(0) }
        
        // Charger le nombre de demandes de suivi
        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val requests = followRequestRepository.getReceivedRequests(userViewModel.userProfile.userId)
                    followRequestsCount = requests.size
                } catch (e: Exception) {
                    followRequestsCount = 0
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section des demandes de suivi
            if (followRequestsCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = XBlue.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Demandes de suivi",
                                    tint = XBlue
                                )
                                Column {
                                    Text(
                                        "Demandes de suivi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = XWhite
                                    )
                                    Text(
                                        "$followRequestsCount demande(s) en attente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = XTextGray
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    onNavigateToKongossa("follow_requests")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = XBlue
                                )
                            ) {
                                Text("Voir")
                            }
                        }
                    }
                }
            }
            
            // Notifications existantes
            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Notifications, null, tint = XTextGray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Silence radio au quartier...",
                                color = XTextGray
                            )
                        }
                    }
                }
            } else {
                items(notifications) { notification ->
                    NotificationListItem(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(notification: Notification) {
    val isExpertValidation = notification.description.contains("expert", ignoreCase = true) || 
                             notification.description.contains("oracle", ignoreCase = true)
    
    val icon = when {
        isExpertValidation -> Icons.Default.Verified
        notification.type == NotificationType.POST -> Icons.Default.TrendingUp
        notification.type == NotificationType.KONGOSSA -> Icons.Default.Chat
        notification.type == NotificationType.COMMUNITY -> Icons.Default.Groups
        else -> Icons.Default.Notifications
    }
    
    val iconColor = when {
        isExpertValidation -> XBlue
        notification.type == NotificationType.POST -> Color(0xFFFFA000)
        notification.type == NotificationType.KONGOSSA -> GreenGabo
        notification.type == NotificationType.COMMUNITY -> XBlue
        else -> XTextGray
    }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { 
            Text(
                text = notification.title, 
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                color = XWhite
            ) 
        },
        supportingContent = { 
            Text(
                notification.description, 
                color = if (isExpertValidation) XBlue else XTextGray,
                fontWeight = if (isExpertValidation) FontWeight.Medium else FontWeight.Normal
            ) 
        },
        overlineContent = { 
            Text(
                notification.time, 
                style = MaterialTheme.typography.labelSmall,
                color = XTextGray
            ) 
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        },
        modifier = Modifier.background(
            if (notification.isRead) Color.Transparent else XBlue.copy(alpha = 0.05f)
        )
    )
}
