# 🗄️ SUPABASE SETUP - PRODUCT POSTS

## 📋 PRÉREQUIS

Pour charger les données de test dans Supabase, tu as besoin de:

1. **Credentials Supabase** (depuis `local.properties`):
   ```
   supabase.url = https://[YOUR-PROJECT].supabase.co
   supabase.anon.key = [YOUR-ANON-KEY]
   ```

2. **Accès au Dashboard Supabase**:
   - https://app.supabase.com

3. **Table 'posts' existante** avec au minimum ces colonnes:
   ```sql
   id (UUID)
   author (TEXT)
   handle (TEXT)
   content (TEXT)
   time (BIGINT)
   postType (TEXT)
   ```

---

## ✨ MIGRATION: AJOUTER LES COLONNES PRODUIT

### Via Dashboard Supabase SQL Editor

1. Ouvre: https://app.supabase.com/project/[YOUR-PROJECT]/sql/new
2. Colle ce SQL:

```sql
-- Migration: Add Product Post Columns
-- Date: 2026-04-25

BEGIN;

-- Add product-related columns to posts table
ALTER TABLE posts 
ADD COLUMN IF NOT EXISTS product_url TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_title TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_price TEXT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS product_image TEXT DEFAULT NULL;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_posts_product_url ON posts(product_url) 
WHERE product_url IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN posts.product_url IS 'URL du produit pour redirection (ex: https://www.nike.com/...)';
COMMENT ON COLUMN posts.product_title IS 'Titre du produit';
COMMENT ON COLUMN posts.product_price IS 'Prix du produit';
COMMENT ON COLUMN posts.product_image IS 'URL de l''image du produit';

COMMIT;
```

3. Clique **Run** (ou Ctrl+Enter)

✅ **Résultat attendu**: "Query executed successfully"

---

## 🧪 INSERTION: TEST DATA (5 PRODUITS NIKE)

Après la migration, colle ce SQL:

```sql
-- Test Data: Nike Product Posts
-- These will appear in the feed immediately after insertion

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

-- Verify insertion
SELECT COUNT(*) as total_posts, COUNT(product_url) as product_posts 
FROM posts 
WHERE product_url IS NOT NULL;
```

✅ **Résultat attendu**: 
```
 total_posts | product_posts
-------------+---------------
      5      |       5
```

---

## 🔍 VÉRIFICATION APRÈS INSERTION

### Via Dashboard SQL Editor

```sql
-- Vérifie les produits créés
SELECT 
  id,
  author,
  product_title,
  product_price,
  product_url,
  time
FROM posts
WHERE product_url IS NOT NULL
ORDER BY time DESC;
```

**Résultat attendu**: 5 lignes avec données Nike

---

## 🚀 ALTERNATIVE: VIA APPLICATION

Si tu veux créer les posts directement depuis l'app:

1. **Build et install l'APK**:
   ```bash
   ./gradlew app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Crée un compte admin** (alias contient "admin"):
   - Email: `admin@ogoula.app`
   - Password: `Admin123!`

3. **Accède au Admin Panel**:
   - Ouvre l'app
   - Scrolle bas → Settings/Profile
   - Tu verras "👨‍💼 Admin Panel" (si alias contient "admin")

4. **Clique "Posts Produits"** tab

5. **Remplit le formulaire**:
   ```
   URL: https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111
   Titre: Chaussure Air Force 1 '07
   Prix: 120$ CAD
   Image: https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/f33f81c5-1da7-4f76-ad05-d1e871f4a33f/chaussure-air-force-1-07-pour-CW2288-111.png
   ```

6. **Clique "Créer le post produit"**

---

## ⚙️ CONFIGURATION POSTMAN (ALTERNATIVE AVANCÉE)

Si tu veux faire des requêtes HTTP directes:

### 1. Create New Request

- **Method**: POST
- **URL**: `https://[YOUR-PROJECT].supabase.co/rest/v1/posts`
- **Headers**:
  ```
  apikey: [YOUR-ANON-KEY]
  Authorization: Bearer [YOUR-ANON-KEY]
  Content-Type: application/json
  Prefer: return=representation
  ```

### 2. Body (Raw JSON)

```json
{
  "author": "Ogoula Admin",
  "handle": "@admin",
  "content": "Chaussure Air Force 1 '07",
  "time": 1713894000000,
  "postType": "classique",
  "product_url": "https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111",
  "product_title": "Chaussure Air Force 1 '07",
  "product_price": "120$ CAD",
  "product_image": "https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/f33f81c5-1da7-4f76-ad05-d1e871f4a33f/chaussure-air-force-1-07-pour-CW2288-111.png"
}
```

### 3. Send

✅ **Résultat**: HTTP 201 Created avec le post retourné

---

## 🔐 SUPABASE TABLE POLICIES (RLS)

Si tu as des erreurs d'accès, vérifie les Row Level Security (RLS) policies:

### Politique recommandée pour posts:

```sql
-- Allow anyone to SELECT posts
CREATE POLICY "Anyone can view posts" ON posts
  FOR SELECT USING (true);

-- Allow authenticated users to INSERT their own posts
CREATE POLICY "Users can create posts" ON posts
  FOR INSERT WITH CHECK (
    auth.uid()::text = user_id OR 
    (SELECT EXISTS(
      SELECT 1 FROM auth.users WHERE id = auth.uid() AND raw_user_meta_data->>'role' = 'admin'
    ))
  );

-- Allow users to UPDATE their own posts
CREATE POLICY "Users can update own posts" ON posts
  FOR UPDATE USING (auth.uid()::text = user_id)
  WITH CHECK (auth.uid()::text = user_id);

-- Allow admins to DELETE posts
CREATE POLICY "Admins can delete posts" ON posts
  FOR DELETE USING (
    SELECT EXISTS(
      SELECT 1 FROM auth.users WHERE id = auth.uid() AND raw_user_meta_data->>'role' = 'admin'
    )
  );
```

---

## ✅ CHECKLIST COMPLÈTE

```
□ Ouvrir https://app.supabase.com
□ Aller au project Ogoula
□ Ouvrir SQL Editor
□ Exécuter migration SQL (colonnes produit)
□ Vérifier: "Query executed successfully"
□ Exécuter insertion SQL (5 produits Nike)
□ Vérifier: Count retourne 5
□ Ouvrir l'app (APK installé)
□ Accéder Admin Panel
□ Aller à "Posts Produits" tab
□ Vérifier les 5 posts Nike dans le feed
□ Cliquer "Voir le produit" → Browser ouvre URL
□ Vérifier redirection fonctionne
```

---

## 🆘 TROUBLESHOOTING

### Erreur: "FOREIGN KEY constraint failed"
**Cause**: Table 'posts' ou colonnes n'existent pas
**Solution**: Vérifier que la table existe avant migration

### Erreur: "Insufficient privileges"
**Cause**: RLS policies restrictives
**Solution**: Vérifier les policies, ou désactiver RLS en dev

### Erreur: "column does not exist"
**Cause**: Migration SQL n'a pas été exécutée
**Solution**: Re-exécuter la migration d'abord

### Posts ne s'affichent pas dans l'app
**Cause**: RLS empêche la lecture
**Solution**: Vérifier "Anyone can view posts" policy exists

### Erreur "product_url must be UUID"
**Cause**: Mauvais type de champ
**Solution**: Vérifier que product_url est TEXT, pas UUID

---

## 📊 VERIFIER LES DONNÉES

Après insertion, tu peux vérifier avec:

```bash
# Via curl (remplace les variables)
curl -s \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
  "https://$SUPABASE_URL/rest/v1/posts?product_url=not.is.null&limit=5" | jq .

# Ou via Supabase Dashboard
# Table editor → posts → Filter: product_url is not null
```

---

**Prêt?** Lance la migration! 🚀
