-- =============================================================================
-- Diagnostic et correction des problèmes RLS profiles
-- =============================================================================
-- Exécuter dans Supabase → SQL Editor

-- 1. Vérifier l'état actuel des politiques
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

-- 2. Vérifier si RLS est activé
SELECT 
  schemaname, 
  tablename, 
  rowsecurity 
FROM pg_tables 
WHERE tablename = 'profiles';

-- 3. Tester les formats d'UUID (pour debug)
SELECT 
  auth.uid() as auth_uid_original,
  auth.uid()::text as auth_uid_text,
  lower(auth.uid()::text) as auth_uid_lower,
  trim(auth.uid()::text) as auth_uid_trimmed,
  lower(trim(auth.uid()::text)) as auth_uid_lower_trimmed;

-- 4. Vérifier les profils existants et leurs formats
SELECT 
  user_id,
  lower(user_id) as user_id_lower,
  trim(user_id) as user_id_trimmed,
  lower(trim(user_id)) as user_id_lower_trimmed,
  first_name,
  last_name,
  alias
FROM profiles 
ORDER BY user_id DESC LIMIT 5;

-- 5. Politiques corrigées (qui devraient résoudre le problème)
-- Exécuter ces commandes si les politiques actuelles ne fonctionnent pas

-- Supprimer anciennes politiques
DROP POLICY IF EXISTS "profiles_insert_own_user_id" ON public.profiles;
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_delete_own" ON public.profiles;

-- Créer nouvelles politiques avec format cohérent
CREATE POLICY "profiles_insert_own_user_id" ON public.profiles
  FOR INSERT
  TO authenticated
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );

CREATE POLICY "profiles_select_own" ON public.profiles
  FOR SELECT
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );

CREATE POLICY "profiles_update_own" ON public.profiles
  FOR UPDATE
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  )
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );
