-- Script pour rechercher et nettoyer les publications contenant "Scène Studio"
-- Exécuter ce script dans Supabase → SQL Editor

-- 1. Rechercher les publications contenant "Scène Studio"
SELECT 
    id,
    content,
    author,
    handle,
    created_at,
    updated_at
FROM posts 
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%'
ORDER BY created_at DESC;

-- 2. Mettre à jour les publications pour supprimer "Scène Studio"
-- Option A: Remplacer "Scène Studio" par une chaîne vide
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

-- 3. Nettoyer les espaces multiples et les espaces en début/fin après suppression
UPDATE posts 
SET content = TRIM(REGEXP_REPLACE(content, '\s+', ' ', 'g'))
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%';

-- 4. Si le contenu est vide après nettoyage, mettre "Vidéo" par défaut
UPDATE posts 
SET content = 'Vidéo'
WHERE (content IS NULL OR content = '' OR content = ' ')
  AND (
    content ILIKE '%Scène Studio%' 
    OR content ILIKE '%Scene Studio%'
    OR content ILIKE '%scene studio%'
    OR content ILIKE '%scène studio%'
  );

-- 5. Vérifier les résultats après nettoyage
SELECT 
    id,
    content,
    author,
    handle,
    created_at,
    updated_at
FROM posts 
WHERE content ILIKE '%Scène Studio%' 
   OR content ILIKE '%Scene Studio%'
   OR content ILIKE '%scene studio%'
   OR content ILIKE '%scène studio%'
ORDER BY created_at DESC;

-- 6. Afficher les publications qui ont été modifiées (avec "Vidéo" par défaut)
SELECT 
    id,
    content,
    author,
    handle,
    created_at,
    updated_at,
    CASE 
        WHEN content = 'Vidéo' THEN 'Modifié - Vidéo par défaut'
        ELSE 'Modifié - Titre nettoyé'
    END as statut
FROM posts 
WHERE updated_at > NOW() - INTERVAL '1 minute'
ORDER BY updated_at DESC;
