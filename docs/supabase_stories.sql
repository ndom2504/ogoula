-- Exécuter dans Supabase → SQL Editor (une fois)
-- Stories « Au Quartier » : persistance + modération admin (suspendre / supprimer).

create table if not exists public.stories (
  id text primary key,
  user_id text not null,
  author_display text not null,
  content_text text,
  image_url text,
  color integer not null,
  status text not null default 'active',
  moderation_note text,
  created_at timestamptz not null default now()
);

do $$
begin
  alter table public.stories
    add constraint stories_status_check
    check (status in ('active', 'suspended'));
exception
  when duplicate_object then null;
end $$;

create index if not exists stories_created_at_idx on public.stories (created_at desc);
create index if not exists stories_status_idx on public.stories (status);
create index if not exists stories_user_id_idx on public.stories (user_id);

alter table public.stories enable row level security;

-- Lecture : l’app filtre `status = 'active'` côté client ; l’admin web liste tout.
drop policy if exists "stories_select_all" on public.stories;
create policy "stories_select_all" on public.stories
  for select
  using (true);

-- Création : uniquement pour le propriétaire (session Supabase = user_id).
-- Comparaison insensible à la casse / espaces (évite les rejets RLS si format UUID différent).
drop policy if exists "stories_insert_own" on public.stories;
create policy "stories_insert_own" on public.stories
  for insert
  to authenticated
  with check (
    auth.uid() is not null
    and lower(trim(user_id::text)) = lower(trim(auth.uid()::text))
  );

-- Mise à jour / suppression : panneau admin (même modèle permissif que communities).
-- À resserrer en prod (Edge Function service_role ou rôle admin dédié).
drop policy if exists "stories_update_all" on public.stories;
create policy "stories_update_all" on public.stories
  for update using (true) with check (true);

drop policy if exists "stories_delete_all" on public.stories;
create policy "stories_delete_all" on public.stories
  for delete using (true);

-- Métriques + handle pour score / filtre 24h (exécuter si la table existait déjà sans ces colonnes)
alter table public.stories add column if not exists author_handle text;
alter table public.stories add column if not exists views integer not null default 0;
alter table public.stories add column if not exists validates integer not null default 0;
