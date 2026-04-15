-- =============================================================================
-- Correction complète des politiques RLS pour la table profiles
-- =============================================================================
-- Exécuter dans Supabase → SQL Editor pour résoudre les erreurs d'enregistrement

-- Vérifier que RLS est activé sur la table profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Politique INSERT : permettre aux utilisateurs de créer leur propre profil
DROP POLICY IF EXISTS "profiles_insert_own_user_id" ON public.profiles;
CREATE POLICY "profiles_insert_own_user_id" ON public.profiles
  FOR INSERT
  TO authenticated
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Politique SELECT : permettre aux utilisateurs de lire leur propre profil
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
CREATE POLICY "profiles_select_own" ON public.profiles
  FOR SELECT
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Politique UPDATE : permettre aux utilisateurs de modifier leur propre profil
DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own" ON public.profiles
  FOR UPDATE
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND lower(trim(user_id)) = lower(trim(auth.uid()::text))
  )
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Politique DELETE : permettre aux utilisateurs de supprimer leur propre profil (optionnel)
DROP POLICY IF EXISTS "profiles_delete_own" ON public.profiles;
CREATE POLICY "profiles_delete_own" ON public.profiles
  FOR DELETE
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Conserver les politiques admin (si elles existent)
-- Les politiques ci-dessus sont cumulatives (OR logique)

-- Test de vérification (optionnel)
-- SELECT count(*) FROM pg_policies WHERE tablename = 'profiles';
