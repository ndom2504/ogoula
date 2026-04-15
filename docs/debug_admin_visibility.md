# Debug : Pourquoi les comptes créés n'apparaissent pas dans l'admin

## Étapes de diagnostic

### 1. Vérifier les variables d'environnement web

Dans `web/.env.local` (doit exister) :
```bash
NEXT_PUBLIC_SUPABASE_URL=https://votre-projet.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=votre_cle_anon
SUPABASE_SERVICE_ROLE_KEY=eyJ...  # Clé service role OBLIGATOIRE
ADMIN_EMAIL_ALLOWLIST=votre@email.com,info@misterdil.ca
```

### 2. Tester l'API admin directement

```bash
# Depuis le dossier web
npm run dev

# Dans un autre terminal, tester l'API :
curl -X GET "http://localhost:3000/api/admin/profiles" \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT"
```

### 3. Vérifier les politiques RLS Supabase

Exécuter dans Supabase → SQL Editor :
```sql
-- Voir les politiques actuelles sur profiles
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'profiles';

-- Tester si l'admin peut lire tous les profils
SELECT count(*) FROM profiles;
```

### 4. Corriger les politiques RLS si nécessaire

Si les politiques sont trop restrictives, exécuter :
```sql
-- Politique pour admin via email
DROP POLICY IF EXISTS "profiles_select_admin_jwt_email" ON public.profiles;
CREATE POLICY "profiles_select_admin_jwt_email" ON public.profiles
  FOR SELECT TO authenticated
  USING (
    coalesce((select auth.jwt() ->> 'email'), '') ILIKE ANY(ARRAY[
      'info@misterdil.ca',
      'admin@ogoula.com', 
      'votre@email.com'
    ])
  );
```

### 5. Vérifier la connexion Android → Supabase

Les comptes créés depuis Android doivent bien être dans la table `profiles` :
```sql
SELECT user_id, first_name, last_name, alias, created_at 
FROM profiles 
ORDER BY created_at DESC LIMIT 10;
```

## Causes fréquentes

1. **SERVICE_ROLE_KEY manquante** → L'API admin échoue et utilise le fallback client
2. **Email non dans ALLOWLIST** → L'API renvoie 403 Forbidden  
3. **Politiques RLS trop restrictives** → Même l'API service role est bloquée
4. **Table profiles vide** → Les comptes Android ne synchronisent pas vers Supabase

## Solution rapide

1. Créer `web/.env.local` avec les bonnes clés
2. Redémarrer le serveur web (`npm run dev`)
3. Se reconnecter à l'admin web
4. Vérifier l'onglet "Utilisateurs"
