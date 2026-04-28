-- Script de force fix pour le problème de profil
-- Exécuter ce script dans Supabase → SQL Editor

-- 1. Désactiver complètement RLS temporairement
ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;

-- 2. Créer une politique très permissive pour les profils publics
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON profiles;
CREATE POLICY "Public profiles are viewable by everyone" ON profiles
    FOR SELECT USING (true);

-- 3. Créer une politique pour les utilisateurs authentifiés
DROP POLICY IF EXISTS "Users can view all profiles" ON profiles;
CREATE POLICY "Users can view all profiles" ON profiles
    FOR SELECT USING (auth.role() = 'authenticated');

-- 4. Réactiver RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 5. Test direct pour vérifier que ça fonctionne
SELECT 'Test de lecture directe' as status, COUNT(*) as count FROM profiles;

-- 6. Afficher quelques profils pour vérification
SELECT user_id, alias, first_name, last_name FROM profiles LIMIT 3;
