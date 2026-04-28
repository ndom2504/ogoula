-- Script pour vérifier la structure de la table posts
-- Exécuter ce script dans Supabase → SQL Editor

-- 1. Vérifier la structure de la table posts
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'posts' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- 2. Voir quelques exemples de données pour comprendre la structure
SELECT * FROM posts LIMIT 3;

-- 3. Rechercher les publications contenant "Scène Studio" (sans colonnes created_at/updated_at)
SELECT 
    id,
    content,
    author,
    handle
FROM posts 
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%'
ORDER BY id DESC
LIMIT 10;
