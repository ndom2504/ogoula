package com.example.ogoula.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.theme.BlueGabo
import com.example.ogoula.ui.theme.YellowGabo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    innerPadding: PaddingValues, 
    onStudioClick: () -> Unit,
    onLiveClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onCreateCommunityClick: () -> Unit
) {
    var showSceneSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Partagez avec le Bled",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        PostOption(
            title = "Post Classique",
            subtitle = "Textes, images ou memes",
            icon = Icons.Default.Add,
            color = MaterialTheme.colorScheme.surface,
            onClick = onCreatePostClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PostOption(
            title = "Créer ma Communauté",
            subtitle = "Lancez votre groupe d'intérêt",
            icon = Icons.Default.Group,
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = onCreateCommunityClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PostOption(
            title = "Créer une Scène",
            subtitle = "Direct ou Studio Montage",
            icon = Icons.Default.VideoCall,
            color = BlueGabo,
            onClick = { showSceneSheet = true },
        )
    }

    if (showSceneSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSceneSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Choisir un mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Direct (Live)") },
                    supportingContent = { Text("Lancer un live vidéo au quartier") },
                    leadingContent = { Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Red) },
                    modifier = Modifier.clickable { 
                        showSceneSheet = false
                        onLiveClick()
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Studio Montage") },
                    supportingContent = { Text("Éditer une vidéo avec des filtres fun") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    modifier = Modifier.clickable {
                        showSceneSheet = false
                        onStudioClick()
                    }
                )
            }
        }
    }
}

@Composable
fun PostOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
