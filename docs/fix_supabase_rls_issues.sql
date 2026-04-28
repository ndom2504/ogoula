-- CORRECTION PRÉCISE DES PROBLÈMES SUPABASE
-- Exécuter ce script dans Supabase SQL Editor

-- 1. Activer RLS sur la table profiles (résout les 2 issues)
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 2. Conserver les politiques existantes qui sont correctes
-- Les politiques suivantes existent déjà et sont correctes :
-- - "Enable read access for all users"
-- - "Users delete own profile"
-- - "Users insert own profile" 
-- - "Users update own profile"
-- - "Users view own profile"
-- - profiles_select_directory_authenticated
-- - profiles_select_own
-- - profiles_update_own

-- 3. Vérifier que RLS est maintenant activé
SELECT 
    schemaname,
    tablename,
    rowsecurity as rls_enabled
FROM pg_tables 
WHERE tablename = 'profiles' AND schemaname = 'public';

-- 4. Lister toutes les politiques actuelles pour vérification
SELECT 
    policyname,
    permissive,
    roles,
    cmd,
    CASE 
        WHEN qual = 'true' THEN 'Accès total'
        ELSE 'Accès restreint'
    END as access_type
FROM pg_policies 
WHERE tablename = 'profiles' 
ORDER BY policyname;

-- 5. Test de lecture pour vérifier que ça fonctionne
SELECT 'Test lecture profiles - RLS activé' as status, COUNT(*) as count FROM profiles;

-- 6. Afficher quelques profils pour confirmation
SELECT user_id, alias, first_name, last_name FROM profiles LIMIT 3;
