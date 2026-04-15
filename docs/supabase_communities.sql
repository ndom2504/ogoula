-- Exécuter dans Supabase → SQL Editor (une fois)
-- Communautés : visibles dans l’admin web et synchronisées depuis l’app Android.

create table if not exists public.communities (
  id text primary key,
  name text not null,
  description text not null default '',
  cover_url text,
  member_count integer not null default 1,
  created_at timestamptz not null default now()
);

create index if not exists communities_created_at_idx on public.communities (created_at desc);

-- Créateur (Bled : l’app ne liste que « mes » communautés via filtre user_id).
alter table public.communities add column if not exists user_id text;
create index if not exists communities_user_id_idx on public.communities (user_id);

alter table public.communities enable row level security;

-- Politiques permissives (à resserrer en prod : insert/delete réservés aux comptes authentifiés / admin).
-- Nécessaires si RLS est activé et que l’app utilise la clé anon avec ou sans session.
-- `drop policy if exists` permet de relancer tout le script sans erreur 42710.
drop policy if exists "communities_select_all" on public.communities;
create policy "communities_select_all" on public.communities
  for select using (true);

drop policy if exists "communities_insert_all" on public.communities;
create policy "communities_insert_all" on public.communities
  for insert with check (true);

drop policy if exists "communities_update_all" on public.communities;
create policy "communities_update_all" on public.communities
  for update using (true) with check (true);

drop policy if exists "communities_delete_all" on public.communities;
create policy "communities_delete_all" on public.communities
  for delete using (true);
