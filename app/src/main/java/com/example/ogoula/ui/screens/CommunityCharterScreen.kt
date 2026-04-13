package com.example.ogoula.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ogoula.ui.theme.GreenGabo

/**
 * Charte communautaire Ogoula : valeurs culturelles, éducatives et humaines,
 * règles de publication et cadre de modération.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityCharterScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charte Ogoula", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = GreenGabo,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Notre espace se veut d'abord culturel, éducatif et humain. " +
                        "Chaque voix compte ; le respect et l'intégrité en sont les piliers.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Person,
                    title = "Respect et dignité",
                    body = "Traite les autres comme tu aimerais l'être : pas d'insultes, de harcèlement, " +
                        "de menaces ni de contenu humiliant. Nous valorisons les échanges constructifs entre Gabonais·e·s " +
                        "et ami·e·s du Gabon, ici et ailleurs."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.School,
                    title = "Vérité et intégrité",
                    body = "Privilégie des informations vérifiables et honnêtes. " +
                        "Pas de désinformation grave, pas d'incitation à la haine. " +
                        "Les débats peuvent être vifs, mais restent dans le cadre du dialogue."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Groups,
                    title = "Culture et partage",
                    body = "Ogoula célèbre nos langues, nos traditions, nos actualités et nos initiatives. " +
                        "Partage ce qui nourrit la communauté : arts, savoir-faire, initiatives locales, " +
                        "questions citoyennes — avec bienveillance."
                )
            }
            item {
                CharterCard(
                    icon = Icons.AutoMirrored.Filled.Feed,
                    title = "Publications responsables",
                    body = "Pas de contenu violent ou pornographique, pas d'exploitation de mineur·e·s, " +
                        "pas de spam ni d'arnaques. Respecte les droits d'auteur. " +
                        "Les contenus publicitaires excessifs ou trompeurs ne sont pas les bienvenus."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Shield,
                    title = "Modération et sanctions",
                    body = "En cas de non-respect de cette charte, l'équipe peut retirer un contenu, " +
                        "suspendre temporairement un compte ou exclure un compte en dernier recours, " +
                        "avec un souci d'équité. Tu peux contacter le support pour faire appel " +
                        "(info@misterdil.ca) en expliquant ta situation."
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "En utilisant Ogoula, tu acceptes cette charte. Merci de faire vivre un espace " +
                            "où chacun·e peut s'exprimer avec dignité.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CharterCard(icon: ImageVector, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = GreenGabo)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
