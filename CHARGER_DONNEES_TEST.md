# 📦 GUIDE COMPLET - CHARGER LES DONNÉES DE TEST

## 🎯 OBJECTIF

Charger **5 produits Nike** dans Supabase pour tester le système de posts produits.

---

## 📋 PRÉREQUIS

✅ APK compilée: `app/build/outputs/apk/debug/app-debug.apk`
✅ Accès Supabase: https://app.supabase.com
✅ Credentials: `supabase.url` et `supabase.anon.key` (depuis `local.properties`)

---

## 🚀 MÉTHODE 1: SCRIPT AUTOMATISÉ (RECOMMANDÉ)

### Étape 1: Lance le script

```bash
cd /Users/morelsttevensndong/ogoula
./setup-product-posts.sh
```

### Étape 2: Suis les prompts

Le script te demandera:
- URL Supabase
- Clé Anon
- Confirmation de la migration
- Confirmation de l'insertion

### Résultat

✅ 5 posts produits créés
✅ Données visibles dans Supabase
✅ Prêt à tester dans l'app

---

## 🛠️ MÉTHODE 2: MANUEL VIA SUPABASE DASHBOARD

### Étape 1: Ouvre le SQL Editor

1. Accède https://app.supabase.com
2. Sélectionne ton projet "Ogoula"
3. Va à **SQL Editor**
4. Clique **New Query**

### Étape 2: Exécute la MIGRATION

Copie-colle ce SQL:

```sql
-- Migration: Add Product Post Columns
BEGIN;

ALTER TABLE posts 
ADD COLUMN IF NOT EXISTS product_url TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_title TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_price TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_image TEXT DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_posts_product_url ON posts(product_url) 
WHERE product_url IS NOT NULL;

COMMIT;
```

Clique **Run** (Ctrl+Enter)

✅ **Résultat attendu**: "Query executed successfully"

### Étape 3: Insère les DONNÉES DE TEST

Crée une **nouvelle query** et colle:

```sql
-- Test Data: 5 Nike Products

BEGIN;

-- Nike Air Force 1
INSERT INTO posts (
  id, author, handle, content, time, postType,
  product_url, product_title, product_price, product_image
) VALUES (
  gen_random_uuid(),
  'Ogoula Admin',
  '@admin',
  '👟 Chaussure iconique depuis 1982 - Air Force 1 ''07',
  (extract(epoch from now()) * 1000)::bigint,
  'classique',
  'https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111',
  'Chaussure Air Force 1 ''07',
  '120$ CAD',
  'https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/f33f81c5-1da7-4f76-ad05-d1e871f4a33f/chaussure-air-force-1-07-pour-CW2288-111.png'
);

-- Nike Air Max 90
INSERT INTO posts (
  id, author, handle, content, time, postType,
  product_url, product_title, product_price, product_image
) VALUES (
  gen_random_uuid(),
  'Ogoula Admin',
  '@admin',
  '🌪️ Technologie Air Cushioning visible - Confort légendaire',
  (extract(epoch from now()) * 1000 - 3600000)::bigint,
  'classique',
  'https://www.nike.com/ca/fr/t/chaussure-air-max-90/CN8490-100',
  'Nike Air Max 90',
  '145$ CAD',
  'https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/1d3e4e73-bc6b-4c5e-a6dd-8234f9e5b6c7/chaussure-air-max-90.png'
);

-- Nike Revolution 7
INSERT INTO posts (
  id, author, handle, content, time, postType,
  product_url, product_title, product_price, product_image
) VALUES (
  gen_random_uuid(),
  'Ogoula Admin',
  '@admin',
  '⚡ Chaussure de running légère et abordable - Parfait pour débuter',
  (extract(epoch from now()) * 1000 - 7200000)::bigint,
  'classique',
  'https://www.nike.com/ca/fr/t/chaussure-nike-revolution-7-mens-zaZKqV/FB2207-004',
  'Nike Revolution 7',
  '85$ CAD',
  'https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/4a2b1c0d-5e6f-7a8b-9c0d-1e2f3a4b5c6d/chaussure-nike-revolution-7.png'
);

-- Nike Court Legacy
INSERT INTO posts (
  id, author, handle, content, time, postType,
  product_url, product_title, product_price, product_image
) VALUES (
  gen_random_uuid(),
  'Ogoula Admin',
  '@admin',
  '🎾 Inspirée des années 80 - Style vintage moderne',
  (extract(epoch from now()) * 1000 - 10800000)::bigint,
  'classique',
  'https://www.nike.com/ca/fr/t/chaussure-court-legacy-pour-GVvhfR/DA7255-100',
  'Nike Court Legacy',
  '95$ CAD',
  'https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/7f8e9d0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a/chaussure-court-legacy.png'
);

-- Nike Cortez
INSERT INTO posts (
  id, author, handle, content, time, postType,
  product_url, product_title, product_price, product_image
) VALUES (
  gen_random_uuid(),
  'Ogoula Admin',
  '@admin',
  '🏃 Classique des années 70 - Confort suprême',
  (extract(epoch from now()) * 1000 - 14400000)::bigint,
  'classique',
  'https://www.nike.com/ca/fr/t/chaussure-nike-cortez-pour-5qvT2R/749571-100',
  'Nike Cortez',
  '100$ CAD',
  'https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/chaussure-nike-cortez.png'
);

COMMIT;
```

Clique **Run**

✅ **Résultat attendu**: 5 rows inserted

### Étape 4: Vérifie l'insertion

Crée une **nouvelle query**:

```sql
SELECT 
  id,
  product_title,
  product_price,
  product_url
FROM posts
WHERE product_url IS NOT NULL
ORDER BY time DESC;
```

Clique **Run**

✅ **Résultat**: Tu dois voir 5 produits Nike listés

---

## 🧪 MÉTHODE 3: VIA L'APPLICATION

Si tu veux créer les posts directement depuis l'app:

### Étape 1: Crée un compte admin

```
Email: admin@ogoula.app
Alias: admin_test
Password: Admin123!
```

### Étape 2: Ouvre l'app

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Étape 3: Accède au Admin Panel

1. Ouvre l'app
2. Scrolle en bas → **Settings/Profile**
3. Tu verras **"👨‍💼 Admin Panel"** (visible si alias contient "admin")
4. Clique dessus

### Étape 4: Va à "Posts Produits"

- Onglet 1: Utilisateurs
- Onglet 2: **Posts Produits** ← Clique ici

### Étape 5: Crée un post

Remplis le formulaire:

```
URL:   https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111
Titre: Chaussure Air Force 1 '07
Prix:  120$ CAD
Image: https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/f33f81c5-1da7-4f76-ad05-d1e871f4a33f/chaussure-air-force-1-07-pour-CW2288-111.png
```

Clique **Créer le post produit**

✅ Message de succès: "✅ Post créé avec succès!"

### Étape 6: Vois le post dans le feed

1. Scrolle en bas de l'app
2. Clique sur **HomeScreen** (ou retour)
3. Tu verras ton post au début du feed
4. Clique **"🔗 Voir le produit"**
5. Browser ouvre nike.com ✅

---

## 🔍 VÉRIFICATIONS

### Test 1: Posts visibles dans le feed

```
✅ Ouvre HomeScreen
✅ Scrolle en bas
✅ Tu vois "👟 Chaussure Air Force 1 '07"
✅ Image produit s'affiche
✅ Bouton "🔗 Voir le produit" visible
```

### Test 2: Clique sur le bouton

```
✅ Clique "🔗 Voir le produit"
✅ Browser ouvre automatiquement
✅ URL = https://www.nike.com/...
✅ Page Nike charge ✅
```

### Test 3: Interaction normale

```
✅ Autres boutons fonctionnent (👍 💬 📤 🔖)
✅ Pas de crash, pas de lag
✅ Feed reste rapide même avec images
```

### Test 4: Admin Panel

```
✅ Accès limité aux admins (alias contient "admin")
✅ Formulaire valide URL et titre
✅ Success message affiche
✅ Post créé immédiatement
```

---

## 📊 DASHBOARD

Après insertion, tu peux vérifier dans Supabase:

1. Va à **Table Editor**
2. Clique sur **posts**
3. Filtre: `product_url is not null`
4. Tu verras les 5 posts Nike

---

## 🎉 RÉSULTAT FINAL

```
┌─────────────────────────────────────────┐
│         FEED OGOULA - HOME SCREEN       │
├─────────────────────────────────────────┤
│                                         │
│ 👤 Ogoula Admin @admin                  │
│ ⏰ il y a 2 heures                      │
│                                         │
│ 👟 Chaussure Air Force 1 '07            │
│ [Image Nike]                            │
│                                         │
│ [🔗 Voir le produit] ← CLIQUE ICI      │
│ [👍 23] [💬 5] [📤 2] [🔖 10]           │
│                                         │
└─────────────────────────────────────────┘
        ↓ (user clicks button)
        ↓
    BROWSER OUVRE
        ↓
    https://nike.com/...
```

---

## ✅ CHECKLIST FINALE

```
□ Migration SQL exécutée
□ 5 produits Nike insérés
□ Données visibles dans Supabase Dashboard
□ APK installée sur appareil
□ Admin Panel accessible
□ Posts Produits tab fonctionne
□ Formulaire créé avec succès
□ Post visible dans le feed
□ Bouton "Voir le produit" fonctionne
□ Browser s'ouvre avec bonne URL
□ Pas d'erreurs, pas de crash
```

---

## 🆘 TROUBLESHOOTING

### ❌ "Erreur: FOREIGN KEY constraint failed"
**Solution**: La table 'posts' n'existe pas. Crée-la d'abord.

### ❌ "Erreur: Insufficient privileges"
**Solution**: Désactive RLS en développement:
- Dashboard → Authentication → Policies
- Désactive RLS sur table 'posts'

### ❌ "Posts ne s'affichent pas dans l'app"
**Solution**: Vérifie RLS policies permettent SELECT

### ❌ "Bouton 'Voir le produit' n'apparaît pas"
**Solution**: Vérifie que `product_url` n'est pas NULL

### ❌ "Browser ne s'ouvre pas"
**Solution**: Vérifie URL format: `https://...`

---

## 📚 FICHIERS DOCUMENTS

```
├─ SUPABASE_SETUP.md              (ce fichier)
├─ README_PRODUCT_POSTS.md        (overview visuel)
├─ PRODUCT_POSTS_SUMMARY.md       (architecture détaillée)
├─ PRODUCT_POSTS_TEST.md          (guide test)
└─ setup-product-posts.sh          (script automatisé)
```

---

## 🚀 C'EST PRÊT!

Tu as 3 options:

**OPTION 1** (Rapide): Lance le script
```bash
./setup-product-posts.sh
```

**OPTION 2** (Dashboard): Va sur Supabase et exécute le SQL

**OPTION 3** (App): Crée les posts depuis l'Admin Panel

Chaque option aboutit au même résultat: **5 posts produits visibles dans le feed**! 🎉

