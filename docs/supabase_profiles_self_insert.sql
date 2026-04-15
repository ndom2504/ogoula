-- Si la table public.profiles a RLS activé et que l’INSERT depuis l’admin (ou l’app) échoue,
-- exécute ces politiques dans Supabase → SQL Editor.
-- Adapte ou supprime les politiques en double si elles existent déjà.

-- Création du profil par l’utilisateur connecté (même user_id que auth.uid(), casse normalisée).
drop policy if exists "profiles_insert_own_user_id" on public.profiles;
create policy "profiles_insert_own_user_id" on public.profiles
  for insert
  to authenticated
  with check (
    auth.uid() is not null
    and lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Mise à jour de son propre profil (souvent déjà couverte par une politique "own profile")
-- create policy "profiles_update_own" on public.profiles
--   for update to authenticated
--   using (auth.uid()::text = user_id)
--   with check (auth.uid()::text = user_id);
