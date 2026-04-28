# 🛍️ Posts Produits - Guide de Création & Test

## Comment créer des posts produits pour Ogoula

### Via l'Admin Panel (Interface graphique)

1. **Ouvre l'AdminScreen**
   - Va dans l'app
   - Accès Admin → Onglet "Posts Produits"

2. **Remplis le formulaire:**
   - **URL du produit**: `https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111`
   - **Titre du produit**: `Chaussure Air Force 1 '07`
   - **Prix**: `120$ CAD`
   - **Image**: `https://static.nike.com/a/images/t_PDP_1728_v2/f_auto,q_auto:eco/...`

3. **Clique sur "Créer le post produit"**

---

## Données de Test - Exemples de Produits Nike

Voici des informations pour créer plusieurs posts de test:

### Post 1: Air Force 1
```
URL: https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111
Titre: Chaussure Air Force 1 '07
Prix: 120$ CAD
Image: https://static.nike.com/a/images/t_PDP_1728_v2/f_auto,q_auto:eco/rhvjxqhrxgpgphkwmgit/CHAUSSURE%20AIR%20FORCE%201%2707-rWtqPn.jpg
```

### Post 2: Nike Blazer Mid
```
URL: https://www.nike.com/ca/fr/t/nike-blazer-mid-77-vintage/DA6624-100
Titre: Nike Blazer Mid '77 Vintage
Prix: 110$ CAD
Image: https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/wqbcrvndjhjcugfvj0vw/NIKE%20BLAZER%20MID%2777%20VINTAGE-DA6624-100.jpg
```

### Post 3: Nike Dunk Low
```
URL: https://www.nike.com/ca/fr/t/nike-dunk-low-retro/DD1391-100
Titre: Nike Dunk Low Retro
Prix: 125$ CAD
Image: https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/jkfnqsw8z0m7r4vb3xpm/NIKE%20DUNK%20LOW%20RETRO-DD1391-100.jpg
```

---

## Architecture Technique

### Posts Produits - Champs Ajoutés à la classe `Post`:

```kotlin
// Champs pour les posts produits
@SerialName("product_url") val productUrl: String? = null
@SerialName("product_title") val productTitle: String? = null
@SerialName("product_price") val productPrice: String? = null
@SerialName("product_image") val productImage: String? = null
```

### Bouton "Voir le produit"

Un bouton spécial `🔗 Voir le produit` apparaît dans le feed pour les posts avec une URL produit. Clique le bouton pour ouvrir le lien dans le navigateur.

---

## Fonctionnalités Implémentées

✅ **Création de posts produits** via AdminScreen
✅ **Champs produit** (URL, titre, prix, image)
✅ **Bouton "Voir le produit"** dans les posts
✅ **Sauvegarde en base de données** via Supabase
✅ **Affichage dans le feed** sans changement d'interface

---

## Prochaines Étapes (Optionnel)

1. **Web Scraping automatique**: Extraire le titre/prix/image directement du lien
   - Utiliser une API comme Cheerio.js ou BeautifulSoup
   - Remplir automatiquement les champs

2. **Galerie de produits**: Afficher les posts produits dans une section dédiée

3. **Analytics**: Tracker les clics sur "Voir le produit"

4. **Marques partenaires**: Tags spéciaux pour Nike, Adidas, etc.

