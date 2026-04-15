-- À exécuter dans Supabase → SQL Editor si, après inscription, la reconnexion
-- renvoie au formulaire vide ou si upsert / lecture du profil échoue (RLS).
--
-- NOTE « Query has destructive operations » : Supabase affiche cet avertissement
-- à cause des lignes `drop policy if exists` ci-dessous. Ce n’est pas un DROP TABLE :
-- c’est voulu pour remplacer des politiques en double. Tu peux confirmer
-- « Run this query » si le script correspond à ton intention.
--
-- Symptômes typiques :
-- - INSERT ok mais UPDATE refusé → l’upsert « merge » ne persiste pas les champs.
-- - Aucune politique SELECT sur sa propre ligne → getProfile() retourne null après login.

-- Lecture : l’utilisateur authentifié lit sa ligne (comparaison insensible casse / espaces).
drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
  for select
  to authenticated
  using (
    auth.uid() is not null
    and lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- Mise à jour : l’utilisateur modifie uniquement son profil (requis pour upsert / merge PostgREST).
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
  for update
  to authenticated
  using (
    auth.uid() is not null
    and lower(trim(user_id)) = lower(trim(auth.uid()::text))
  )
  with check (
    auth.uid() is not null
    and lower(trim(user_id)) = lower(trim(auth.uid()::text))
  );

-- L’INSERT « sa ligne » est décrit dans supabase_profiles_self_insert.sql ; garde une seule politique insert cohérente.
