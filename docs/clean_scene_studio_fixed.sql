-- Script corrigé pour rechercher et nettoyer les publications contenant "Scène Studio"
-- Exécuter ce script dans Supabase → SQL Editor

-- 1. D'abord, vérifier la structure de la table posts
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'posts' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- 2. Rechercher les publications contenant "Scène Studio" (sans colonnes created_at/updated_at)
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
LIMIT 20;

-- 3. Mettre à jour les publications pour supprimer "Scène Studio"
UPDATE posts 
SET content = 
    CASE 
        WHEN content ILIKE '%Scène Studio%' THEN REPLACE(content, 'Scène Studio', '')
        WHEN content ILIKE '%Scene Studio%' THEN REPLACE(content, 'Scene Studio', '')
        WHEN content ILIKE '%scene studio%' THEN REPLACE(content, 'scene studio', '')
        WHEN content ILIKE '%scène studio%' THEN REPLACE(content, 'scène studio', '')
        ELSE content
    END
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%';

-- 4. Nettoyer les espaces multiples et les espaces en début/fin après suppression
UPDATE posts 
SET content = TRIM(REGEXP_REPLACE(content, '\s+', ' ', 'g'))
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%';

-- 5. Si le contenu est vide après nettoyage, mettre "Vidéo" par défaut
UPDATE posts 
SET content = 'Vidéo'
WHERE (content IS NULL OR content = '' OR content = ' ');

-- 6. Vérifier les résultats après nettoyage
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
ORDER BY id DESC;

-- 7. Afficher les publications qui ont été modifiées (celles avec "Vidéo" ou titres nettoyés)
SELECT 
    id,
    content,
    author,
    handle,
    CASE 
        WHEN content = 'Vidéo' THEN 'Modifié - Vidéo par défaut'
        WHEN LENGTH(TRIM(content)) > 0 THEN 'Modifié - Titre nettoyé'
        ELSE 'Modifié - Vide'
    END as statut
FROM posts 
WHERE content = 'Vidéo' 
   OR LENGTH(TRIM(content)) = 0
ORDER BY id DESC
LIMIT 10;
