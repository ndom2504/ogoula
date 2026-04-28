-- CORRECTION DES POLITIQUES RLS - INFINITE RECURSION
-- Problème: Les politiques RLS actuelles causent une boucle infinie
-- Solution: Simplifier les politiques et éviter les références circulaires

-- 1. Désactiver RLS temporairement pour pouvoir modifier les politiques
ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;

-- 2. Supprimer toutes les politiques existantes
DROP POLICY IF EXISTS "Users can view their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can insert their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON profiles;
DROP POLICY IF EXISTS "Users can delete their own profile" ON profiles;
DROP POLICY IF EXISTS "Service role can manage all profiles" ON profiles;

-- 3. Créer des politiques simplifiées sans récursion

-- Politique SELECT très simple
CREATE POLICY "Users can view own profile" ON profiles
    FOR SELECT USING (auth.uid()::text = user_id::text);

-- Politique INSERT simple
CREATE POLICY "Users can insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- Politique UPDATE simple
CREATE POLICY "Users can update own profile" ON profiles
    FOR UPDATE USING (auth.uid()::text = user_id::text);

-- Politique DELETE simple
CREATE POLICY "Users can delete own profile" ON profiles
    FOR DELETE USING (auth.uid()::text = user_id::text);

-- 4. Réactiver RLS avec les nouvelles politiques
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- 5. Vérifier que les politiques sont bien appliquées
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
WHERE tablename = 'profiles'
ORDER BY policyname;

-- 6. Test simple pour vérifier qu'il n'y a plus de récursion
-- (À exécuter manuellement après avoir appliqué ce script)
-- SELECT * FROM profiles WHERE user_id = '06fff0c7-b023-4888-a8ed-c1da2e41a0f0' LIMIT 1;
