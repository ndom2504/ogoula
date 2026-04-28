package com.example.ogoula.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.theme.GreenGabo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    // État du thème : "dark", "light", "system"
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            
            // Section Apparence
            item {
                Text(
                    text = "Apparence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenGabo,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Option Mode Sombre
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Mode sombre",
                    subtitle = "Utiliser le thème sombre",
                    isSelected = themeMode == "dark",
                    onClick = { 
                        themeMode = "dark"
                        prefs.edit().putString("theme_mode", "dark").apply()
                    }
                )
            }
            
            // Option Mode Clair
            item {
                SettingsItem(
                    icon = Icons.Default.LightMode,
                    title = "Mode clair",
                    subtitle = "Thème blanc et vert",
                    isSelected = themeMode == "light",
                    onClick = { 
                        themeMode = "light"
                        prefs.edit().putString("theme_mode", "light").apply()
                    }
                )
            }
            
            // Option Système
            item {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Selon le système",
                    subtitle = "Utiliser les paramètres du système",
                    isSelected = themeMode == "system",
                    onClick = { 
                        themeMode = "system"
                        prefs.edit().putString("theme_mode", "system").apply()
                    }
                )
            }
            
            // Section Informations
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Informations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenGabo,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Version de l'application
            item {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Version",
                    subtitle = "Ogoula 1.0.0",
                    isSelected = false,
                    onClick = { },
                    showArrow = false
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                GreenGabo.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) GreenGabo else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) GreenGabo else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            GreenGabo.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            if (showArrow) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isSelected) GreenGabo else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
