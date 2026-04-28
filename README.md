# 🌍 Ogoula - Application Android Sociale

## 📱 Description

Ogoula est une application sociale innovante conçue pour les communautés gabonaises et africaines. Elle combine les fonctionnalités d'un réseau social avec des outils de communication avancés, tout en mettant l'accent sur la culture locale et les valeurs communautaires.

## ✨ Fonctionnalités Principales

### 🏠 **Accueil (Accueil)**
- Fil d'actualité avec publications, stories et vidéos
- Système de validation, j'aime, favoris et commentaires
- Support multimédia (images, vidéos, audio)
- Recherche avancée de contenu et de profils

### 👥 **Communauté (Bled)**
- Création et gestion de communautés
- Charte communautaire intégrée
- Notifications de nouvelles communautés
- Interface de gestion des membres

### 📝 **Publication (Publier)**
- Création de publications avec texte, images et vidéos
- Studio de montage vidéo intégré
- Publication en direct (Live)
- Stories éphémères
- Création de communautés

### 💬 **Messagerie (Kongossa)**
- Messagerie instantanée entre utilisateurs
- Système de notifications pour demandes d'abonnement
- Gestion des contacts et abonnements
- Interface de conversation épurée

### 👤 **Profil (Ma Bulle)**
- Profil personnalisable avec photo et informations
- Système de cote de popularité basé sur les contributions
- Stories personnelles
- Paramètres de confidentialité

### 🎵 **Playlist Vidéo**
- Lecture vidéo en plein écran avec scrolling horizontal
- Contrôles de lecture (volume, pause, lecture)
- Options de partage et de suppression
- Adaptation automatique des vidéos à l'écran

## 🛠 Architecture Technique

### 📋 **Stack Technologique**
- **Langage** : Kotlin
- **Framework UI** : Jetpack Compose
- **Architecture** : MVVM (Model-View-ViewModel)
- **Base de données** : Supabase (PostgreSQL)
- **Authentification** : Supabase Auth
- **Stockage** : Supabase Storage
- **Navigation** : Jetpack Navigation Compose

### 🏗️ **Structure du Projet**
```
app/
├── src/main/java/com/example/ogoula/
│   ├── data/                    # Sources de données et repositories
│   │   ├── AuthRepository.kt
│   │   ├── CommunityRepository.kt
│   │   ├── PostRepository.kt
│   │   ├── UserRepository.kt
│   │   └── SupabaseClient.kt
│   ├── ui/                      # Interface utilisateur
│   │   ├── screens/             # Écrans de l'application
│   │   ├── components/          # Composants réutilisables
│   │   ├── theme/               # Thème et styles
│   │   ├── PostViewModel.kt     # ViewModels
│   │   ├── UserViewModel.kt
│   │   └── StoryViewModel.kt
│   └── MainActivity.kt          # Activité principale
└── build.gradle                 # Configuration du build
```

### 🎨 **Système de Thèmes**
- **Mode sombre** : Thème par défaut avec fond sombre
- **Mode clair** : Thème blanc et vert (identité visuelle Ogoula)
- **Mode système** : Adaptation automatique aux paramètres du système

### 🔄 **Architecture MVVM**
- **Models** : Classes de données (Post, User, Community, etc.)
- **Views** : Composables Jetpack Compose
- **ViewModels** : Gestion de l'état et logique métier
- **Repositories** : Accès aux données (Supabase)

## 🗄 Base de Données Supabase

### 📊 **Tables Principales**
- **profiles** : Profils utilisateurs
- **posts** : Publications et contenu
- **communities** : Communautés et groupes
- **comments** : Commentaires sur les publications
- **follow_requests** : Demandes d'abonnement
- **notifications** : Notifications système

### 🔐 **Politiques RLS (Row Level Security)**
- Accès limité aux utilisateurs authentifiés
- Permissions granulaires par type de contenu
- Protection des données personnelles

## 🚀 Installation et Configuration

### 📋 **Prérequis**
- Android Studio Arctic Fox ou supérieur
- JDK 11 ou supérieur
- Android SDK API 21+ (Android 5.0)
- Compte Supabase

### 🔧 **Configuration**

1. **Cloner le projet**
```bash
git clone https://github.com/votre-repo/ogoula.git
cd ogoula
```

2. **Configurer Supabase**
```kotlin
// Dans SupabaseClient.kt
object SupabaseClient {
    private const val SUPABASE_URL = "VOTRE_URL_SUPABASE"
    private const val SUPABASE_KEY = "VOTRE_CLE_SUPABASE"
    
    // Initialisation du client
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
```

3. **Configurer les permissions RLS**
```sql
-- Exemple de politique pour les posts
CREATE POLICY "Users can view posts" ON posts
FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Users can insert their own posts" ON posts
FOR INSERT WITH CHECK (auth.uid() = user_id);
```

4. **Lancer l'application**
```bash
./gradlew assembleDebug
# Ou depuis Android Studio : Run > Run 'app'
```

## 🔍 Fonctionnalités Avancées

### 🔎 **Recherche Multicritères**
- Recherche de profils par nom, prénom, alias
- Recherche de publications par contenu, auteur
- Recherche de communautés par nom, description
- Sanitisation des requêtes pour éviter les injections SQL

### 🎯 **Système de Popularité**
```kotlin
fun getPopularityScore(userAlias: String): Int {
    var total = 0
    // Calcul basé sur les contributions
    total += (validations * 10) + (likes * 5) + (favorites * 8)
    total += (comments.size * 2) + (shares * 15)
    total += 1 // Bonus par publication
    return total
}
```

### 📱 **Notifications Push**
- Notifications pour les nouvelles abonnements
- Alertes pour les mentions et interactions
- Notifications de communauté
- Système de notification intelligent

### 🎥 **Gestion Vidéo**
- Lecture avec ExoPlayer
- Mode plein écran avec scrolling horizontal
- Contrôles de volume et lecture
- Support des formats vidéo courants

## 🧪 Tests

### 📋 **Tests Unitaires**
```bash
./gradlew test
```

### 📱 **Tests d'Intégration**
```bash
./gradlew connectedAndroidTest
```

### 🔍 **Tests UI**
- Tests de navigation
- Tests de composants UI
- Tests d'interaction utilisateur

## 📦 Déploiement

### 🔧 **Build de Production**
```bash
./gradlew assembleRelease
```

### 📱 **Signature APK**
```bash
./gradlew assembleRelease
# Configurer la signature dans build.gradle
```

### 🚀 **Google Play Store**
- Créer un compte développeur
- Configurer la fiche application
- Uploader l'APK signé
- Remplir les informations de la fiche

## 🤝 Contribution

### 📋 **Guides de Contribution**
1. Fork le projet
2. Créer une branche de fonctionnalité
3. Implémenter les changements
4. Ajouter des tests
5. Soumettre une Pull Request

### 🎯 **Bonnes Pratiques**
- Code en Kotlin avec conventions officielles
- Architecture MVVM respectée
- Tests unitaires pour chaque nouvelle fonctionnalité
- Documentation des APIs et fonctions complexes

## 📝 Notes de Version

### 🆕 **Version 1.0.0**
- Initialisation du projet
- Fonctionnalités sociales de base
- Intégration Supabase
- Système de thèmes clair/sombre
- Playlist vidéo avec scrolling horizontal

### 🔮 **Fonctionnalités Futures**
- Appels vidéo/voix
- Fil d'actualité algorithmique
- Système de modération avancé
- Intégration avec d'autres plateformes sociales

## 📞 Support

### 🐛 **Rapport de Bugs**
- Créer une issue sur GitHub
- Décrire le problème avec détails
- Ajouter captures d'écran et logs

### 💬 **Contact**
- Email : support@ogoula.com
- Site web : https://ogoula.com
- Documentation : https://docs.ogoula.com

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 🙏 Remerciements

- Communauté gabonaise pour l'inspiration
- Équipe de développement Supabase
- Contributeurs open source
- Utilisateurs beta-testeurs

---

**Made with ❤️ for the Gabonese community**
