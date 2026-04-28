-- SCRIPT DE DIAGNOSTIC POUR VÉRIFIER SI LES DONNÉES EXISTENT
-- Exécuter dans Supabase SQL Editor

-- 1. Vérifier s'il y a des profils dans la base
SELECT 
    'Total profils' as type,
    COUNT(*) as count
FROM profiles

UNION ALL

SELECT 
    'Profils avec alias' as type,
    COUNT(*) as count
FROM profiles 
WHERE alias IS NOT NULL AND alias != ''

UNION ALL

SELECT 
    'Profils avec prénom' as type,
    COUNT(*) as count
FROM profiles 
WHERE first_name IS NOT NULL AND first_name != '';

-- 2. Afficher tous les profils existants
SELECT 
    user_id,
    alias,
    first_name,
    last_name,
    'Profil existant' as status
FROM profiles 
LIMIT 10;

-- 3. Tester l'accès direct avec un alias spécifique
-- Remplacer @votre_alias par un alias réel que vous testez
SELECT 
    'Test direct alias' as test_type,
    COUNT(*) as found
FROM profiles 
WHERE alias = '@votre_alias';
