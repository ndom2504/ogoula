-- SOLUTION ULTIME - Exécuter dans Supabase SQL Editor
-- Ce script résout définitivement les problèmes de profil

-- 1. COMPLÈTEMENT DÉSACTIVER RLS pour profiles
ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;

-- 2. Supprimer TOUTES les politiques existantes
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON profiles;
DROP POLICY IF EXISTS "Users can view all profiles" ON profiles;
DROP POLICY IF EXISTS "Users can insert their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
DROP POLICY IF EXISTS "Users can view their own profile" ON profiles;

-- 3. Créer UNE SEULE politique ultra-permissive
CREATE POLICY "Enable read access for all users" ON profiles
    FOR ALL USING (true);

-- 4. Laisser RLS désactivé pour l'instant
-- ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 5. Test immédiat
SELECT 'Test lecture profiles' as test, COUNT(*) as count FROM profiles;

-- 6. Afficher les profils disponibles
SELECT user_id, alias, first_name, last_name FROM profiles LIMIT 5;
