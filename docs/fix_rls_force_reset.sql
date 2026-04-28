-- CORRECTION FORCÉE DES POLITIQUES RLS - INFINITE RECURSION
-- Approche radicale pour contourner les erreurs de politiques existantes

-- 1. D'abord, vérifier les politiques actuelles
SELECT policyname, cmd, permissive, roles 
FROM pg_policies 
WHERE tablename = 'profiles';

-- 2. Supprimer RLS complètement (approche radicale)
ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;

-- 3. Forcer la suppression des politiques avec une approche différente
DO $$
DECLARE
    policy_record RECORD;
BEGIN
    FOR policy_record IN 
        SELECT policyname 
        FROM pg_policies 
        WHERE tablename = 'profiles'
    LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || policy_record.policyname || '" ON profiles';
        RAISE NOTICE 'Politique supprimée: %', policy_record.policyname;
    END LOOP;
END $$;

-- 4. Recréer la table sans politiques (si nécessaire)
-- ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_pkey;

-- 5. Créer nouvelles politiques très simples
CREATE POLICY "Users view own profile" ON profiles
    FOR SELECT USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

CREATE POLICY "Users update own profile" ON profiles
    FOR UPDATE USING (auth.uid()::text = user_id::text);

CREATE POLICY "Users delete own profile" ON profiles
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- 6. Réactiver RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 7. Vérifier les nouvelles politiques
SELECT policyname, cmd, permissive, roles 
FROM pg_policies 
WHERE tablename = 'profiles'
ORDER BY policyname;

-- 8. Test de lecture simple (décommentez pour tester)
-- SELECT COUNT(*) FROM profiles LIMIT 1;
