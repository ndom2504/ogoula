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
create policy "stories_select_all" on public.stories
  for select using (true);

-- Création : uniquement pour le propriétaire (session Supabase = user_id).
create policy "stories_insert_own" on public.stories
  for insert with check (auth.uid()::text = user_id);

-- Mise à jour / suppression : panneau admin (même modèle permissif que communities).
-- À resserrer en prod (Edge Function service_role ou rôle admin dédié).
create policy "stories_update_all" on public.stories
  for update using (true) with check (true);

create policy "stories_delete_all" on public.stories
  for delete using (true);
