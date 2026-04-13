package com.example.ogoula.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ogoula.ui.theme.GreenGabo

@Composable
fun ExplorerScreen(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        item {
            SearchBar(modifier = Modifier.padding(16.dp))
        }
        
        item {
            SectionTitle("Le Pouls de la Société")
            TrendingHashtags()
        }
        
        item {
            SectionTitle("Les Sages Recommandés")
            SuggestedUsers()
        }
        
        item {
            SectionTitle("Archives Vivantes")
        }
        
        item {
            PhotoGrid()
        }
    }
}

@Composable
fun SearchBar(modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Rechercher sur Ogoula...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun TrendingHashtags() {
    val tags = listOf("#Humour241", "#ValeursGabo", "#Société", "#Can2025", "#GabonTech")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            SuggestionChip(
                onClick = { },
                label = { Text(tag) }
            )
        }
    }
}

@Composable
fun SuggestedUsers() {
    val users = listOf("Mba Aubame", "Prisca G.", "Le Guide", "Yannick K.")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(users) { user ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(GreenGabo.copy(alpha = 0.22f))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Vérifié",
                        modifier = Modifier.size(12.dp),
                        tint = GreenGabo
                    )
                }
                Button(
                    onClick = { },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Suivre", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun PhotoGrid() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color.Gray))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
            }
        }
    }
}
