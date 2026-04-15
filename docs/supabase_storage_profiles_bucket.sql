-- =============================================================================
-- Bucket Storage « profiles » + politiques RLS (upload photo / bannière)
-- =============================================================================
-- L’app Ogoula utilise le bucket **profiles** (pluriel), voir StorageRepository :
--   profiles / {user_id}/profile.jpg  et  profiles / {user_id}/banner.jpg
--
-- Si tu as créé un bucket nommé **profil** (singulier), ce n’est pas celui que
-- l’app appelle : renomme-le en « profiles » dans le dashboard, ou recrée
-- le bucket avec l’id exact **profiles** ci-dessous.
--
-- Dans l’UI « New policy », il faut cocher **INSERT** (et souvent UPDATE) pour
-- que l’upload depuis l’app fonctionne. Une policy UPDATE sans INSERT bloque
-- les nouveaux fichiers.
-- =============================================================================

-- Création du bucket (id = nom affiché dans l’UI). public = true pour URLs publiques (getPublicUrl).
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'profiles',
  'profiles',
  true,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp']::text[]
)
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = coalesce(excluded.file_size_limit, storage.buckets.file_size_limit),
      allowed_mime_types = coalesce(excluded.allowed_mime_types, storage.buckets.allowed_mime_types);

-- Les politiques portent sur storage.objects (pas sur public.profiles).

-- Nettoyage idempotent (déclenche parfois l’avertissement « destructive » dans l’éditeur : normal).
drop policy if exists "storage_profiles_select_public" on storage.objects;
drop policy if exists "storage_profiles_insert_own" on storage.objects;
drop policy if exists "storage_profiles_update_own" on storage.objects;
drop policy if exists "storage_profiles_delete_own" on storage.objects;

-- Lecture : tout le monde peut lire les fichiers du bucket profiles (avatars publics).
-- Si tu préfères restreindre la lecture, remplace par une condition plus stricte.
create policy "storage_profiles_select_public"
  on storage.objects for select
  to public
  using (bucket_id = 'profiles');

-- Upload : uniquement dans le dossier dont le 1er segment = auth.uid() (même format que en base).
create policy "storage_profiles_insert_own"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'profiles'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Mise à jour / remplacement (upsert côté client) sur ses propres fichiers.
create policy "storage_profiles_update_own"
  on storage.objects for update
  to authenticated
  using (
    bucket_id = 'profiles'
    and (storage.foldername(name))[1] = auth.uid()::text
  )
  with check (
    bucket_id = 'profiles'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Suppression de ses propres fichiers (optionnel).
create policy "storage_profiles_delete_own"
  on storage.objects for delete
  to authenticated
  using (
    bucket_id = 'profiles'
    and (storage.foldername(name))[1] = auth.uid()::text
  );
