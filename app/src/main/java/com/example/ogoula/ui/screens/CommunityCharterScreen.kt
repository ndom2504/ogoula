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
 * Charte communautaire Ogoula : plateforme de valorisation et d'influence
 * pour les marques, produits et personnalités. Règles de publication et cadre de modération.
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
                    text = "Ogoula est une plateforme de valorisation et d'influence où les marques, les produits " +
                        "et les personnalités gagnent en visibilité grâce aux interactions, aux votes et aux retours de " +
                        "la communauté. Pour réussir ensemble, nous nous engageons sur ces piliers fondamentaux.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Person,
                    title = "Respect et authenticité",
                    body = "Traite les autres avec respect : pas d'insultes, de harcèlement ni d'irrespect. " +
                        "Les marques, produits et personnalités gagnent en crédibilité par l'authenticité et la " +
                        "bienveillance. Chaque retour de la communauté est une opportunité d'amélioration."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.School,
                    title = "Transparence et honnêteté",
                    body = "Partage des informations vérifiables et honnêtes sur tes marques, produits ou services. " +
                        "Pas de désinformation grave ni d'arnaques. La communauté valorise la transparence et " +
                        "sanctionne la malhonnêteté par des votes négatifs et des retours critiques."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Groups,
                    title = "Visibilité et valorisation",
                    body = "Ogoula valorise les marques, produits et personnalités qui enrichissent la communauté. " +
                        "Partage du contenu de qualité, des initiatives locales innovantes et des solutions pertinentes. " +
                        "La communauté vote et commente pour soutenir ou améliorer ce qu'elle estime précieux."
                )
            }
            item {
                CharterCard(
                    icon = Icons.AutoMirrored.Filled.Feed,
                    title = "Contenu de qualité",
                    body = "Pas de contenu violent, pornographique ou exploitant des mineur·e·s. " +
                        "Pas de spam, d'arnaques ni de publicités trompeuses. Les contenus doivent être pertinents, " +
                        "professionnels et respectueux. Crédite les sources et respecte les droits d'auteur."
                )
            }
            item {
                CharterCard(
                    icon = Icons.Default.Shield,
                    title = "Justice et modération",
                    body = "En cas de non-respect de cette charte, l'équipe peut retirer un contenu, " +
                        "suspendre temporairement un compte ou exclure un compte en dernier recours. " +
                        "Les votes négatifs et les retours critiques de la communauté jouent aussi un rôle de régulation. " +
                        "Contacter info@misterdil.ca pour contester une modération."
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "En utilisant Ogoula, tu acceptes cette charte et t'engages à contribuer à une " +
                            "communauté où les marques, produits et personnalités prospèrent grâce à des interactions " +
                            "authentiques, respectueuses et constructives. Merci de faire vivre cet espace d'influence " +
                            "et de valorisation.",
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
