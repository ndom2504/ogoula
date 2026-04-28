-- =============================================================================
-- public.posts — activer la RLS (correctif « table is public but RLS not enabled »)
-- =============================================================================
-- Exécuter dans Supabase → SQL Editor (une fois, ou après modification des noms
-- de politiques). L’avertissement « destructive » à cause de `drop policy` est normal.
--
-- Politiques alignées sur docs/supabase_communities.sql : le fil d’actualité et
-- les réactions (validates, loves, comments) restent accessibles avec la clé anon
-- et une session utilisateur, comme avant l’activation de la RLS.
--
-- Pour durcir en prod : ajouter une colonne user_id, la renseigner côté app,
-- puis remplacer insert/update/delete par des conditions auth.uid() / rôles admin.
-- =============================================================================

alter table public.posts enable row level security;

drop policy if exists "posts_select_all" on public.posts;
create policy "posts_select_all" on public.posts
  for select
  using (true);

drop policy if exists "posts_insert_all" on public.posts;
create policy "posts_insert_all" on public.posts
  for insert
  with check (true);

drop policy if exists "posts_update_all" on public.posts;
create policy "posts_update_all" on public.posts
  for update
  using (true)
  with check (true);

drop policy if exists "posts_delete_all" on public.posts;
create policy "posts_delete_all" on public.posts
  for delete
  using (true);
