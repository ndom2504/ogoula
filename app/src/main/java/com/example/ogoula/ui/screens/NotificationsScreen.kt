package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
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
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.GreenGabo
import com.example.ogoula.ui.theme.YellowGabo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: PostViewModel, onBack: () -> Unit) {
    val notifications = viewModel.notifications

    // Marquer comme lu quand on quitte l'écran
    LaunchedEffect(Unit) {
        // En théorie on le ferait ici ou via un bouton
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markNotificationsAsRead() }) {
                        Text("Tout lire", color = GreenGabo)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Aucune notification pour le moment.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(notifications) { notification ->
                    NotificationListItem(notification)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(notification: Notification) {
    val icon = when (notification.type) {
        NotificationType.POST -> Icons.Default.Public
        NotificationType.KONGOSSA -> Icons.Default.Chat
        NotificationType.COMMUNITY -> Icons.Default.Groups
        NotificationType.SYSTEM -> Icons.Default.Notifications
    }
    
    val iconColor = when (notification.type) {
        NotificationType.POST -> BlueGabo
        NotificationType.KONGOSSA -> GreenGabo
        NotificationType.COMMUNITY -> YellowGabo
        NotificationType.SYSTEM -> Color.Gray
    }

    ListItem(
        headlineContent = { 
            Text(
                text = notification.title, 
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold 
            ) 
        },
        supportingContent = { Text(notification.description) },
        overlineContent = { Text(notification.time, style = MaterialTheme.typography.labelSmall) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        },
        modifier = Modifier.background(
            if (notification.isRead) Color.Transparent else GreenGabo.copy(alpha = 0.05f)
        )
    )
}
