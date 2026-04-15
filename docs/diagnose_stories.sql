-- =============================================================================
-- Diagnostic des problèmes de stories
-- =============================================================================
-- Exécuter dans Supabase → SQL Editor

-- 1. Vérifier si la table stories existe
SELECT table_name, table_type 
FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name = 'stories';

-- 2. Vérifier la structure de la table stories
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_schema = 'public' AND table_name = 'stories'
ORDER BY ordinal_position;

-- 3. Vérifier les politiques RLS sur stories
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
WHERE tablename = 'stories'
ORDER BY policyname;

-- 4. Vérifier si RLS est activé sur stories
SELECT 
  schemaname, 
  tablename, 
  rowsecurity 
FROM pg_tables 
WHERE tablename = 'stories';

-- 5. Vérifier les stories existantes
SELECT 
  id,
  user_id,
  status,
  created_at,
  content_text,
  author_display
FROM stories 
ORDER BY created_at DESC LIMIT 5;

-- 6. Créer la table stories si elle n'existe pas
CREATE TABLE IF NOT EXISTS public.stories (
  id text PRIMARY KEY,
  user_id text NOT NULL,
  status text DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'archived')),
  content_text text,
  image_url text,
  author_display text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now(),
  moderation_note text,
  expires_at timestamptz
);

-- Activer RLS
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;

-- Politiques RLS pour stories
DROP POLICY IF EXISTS "stories_insert_own" ON public.stories;
CREATE POLICY "stories_insert_own" ON public.stories
  FOR INSERT
  TO authenticated
  WITH CHECK (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );

DROP POLICY IF EXISTS "stories_select_own" ON public.stories;
CREATE POLICY "stories_select_own" ON public.stories
  FOR SELECT
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );

DROP POLICY IF EXISTS "stories_update_own" ON public.stories;
CREATE POLICY "stories_update_own" ON public.stories
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

DROP POLICY IF EXISTS "stories_delete_own" ON public.stories;
CREATE POLICY "stories_delete_own" ON public.stories
  FOR DELETE
  TO authenticated
  USING (
    auth.uid() IS NOT NULL
    AND user_id = lower(trim(auth.uid()::text))
  );
