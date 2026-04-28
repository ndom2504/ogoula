-- Script de diagnostic pour le problème "profil introuvable"
-- Exécuter ce script dans Supabase → SQL Editor

-- 1. Vérifier si la table profiles existe et sa structure
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'profiles' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- 2. Vérifier s'il y a des profils dans la table
SELECT 
    COUNT(*) as total_profiles,
    COUNT(CASE WHEN alias IS NOT NULL AND alias != '' THEN 1 END) as profiles_with_alias
FROM profiles;

-- 3. Afficher quelques exemples de profils avec leurs alias
SELECT 
    user_id,
    alias,
    first_name,
    last_name
FROM profiles 
WHERE alias IS NOT NULL AND alias != ''
LIMIT 5;

-- 4. Vérifier les politiques RLS actuelles sur la table profiles
SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies 
WHERE tablename = 'profiles';

-- 5. Vérifier si RLS est activé sur la table profiles
SELECT 
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables 
WHERE tablename = 'profiles' AND schemaname = 'public';

-- 6. Test direct de recherche par alias (remplacer @test_alias par un alias réel)
-- Décommentez et adaptez cette ligne avec un alias réel que vous connaissez
-- SELECT * FROM profiles WHERE alias = '@test_alias';

-- 7. Vérifier la structure complète de la table profiles
SELECT * FROM profiles LIMIT 1;
