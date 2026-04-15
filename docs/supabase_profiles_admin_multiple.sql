-- =============================================================================
-- Politiques RLS admin pour plusieurs administrateurs
-- =============================================================================
-- Remplace la politique hardcoded par une approche plus flexible
-- Exécuter dans Supabase → SQL Editor

-- Table pour gérer la liste des admins (optionnel, plus maintenable)
-- create table if not exists public.admin_emails (
--   id serial primary key,
--   email text unique not null,
--   created_at timestamptz default now()
-- );

-- Insérer les admins initiaux (décommenter si table créée)
-- insert into public.admin_emails (email) values 
--   ('info@misterdil.ca'),
--   ('admin@ogoula.com')
-- on conflict (email) do nothing;

-- Politique SELECT pour les admins (emails multiples + alias @admin)
drop policy if exists "profiles_select_admin_multiple" on public.profiles;
create policy "profiles_select_admin_multiple" on public.profiles
  for select
  to authenticated
  using (
    -- Admin par email
    coalesce((select auth.jwt() ->> 'email'), '') ilike any(array['info@misterdil.ca', 'admin@ogoula.com', '%@ogoula.com'])
    -- OU admin par alias (cast explicite de UUID vers text)
    or coalesce((select alias from public.profiles where user_id::text = auth.uid()::text), '') ilike '%admin%'
  );

-- Politique UPDATE pour les admins (permet de modifier n'importe quel profil)
drop policy if exists "profiles_update_admin_multiple" on public.profiles;
create policy "profiles_update_admin_multiple" on public.profiles
  for update
  to authenticated
  using (
    coalesce((select auth.jwt() ->> 'email'), '') ilike any(array['info@misterdil.ca', 'admin@ogoula.com', '%@ogoula.com'])
    or coalesce((select alias from public.profiles where user_id::text = auth.uid()::text), '') ilike '%admin%'
  )
  with check (
    coalesce((select auth.jwt() ->> 'email'), '') ilike any(array['info@misterdil.ca', 'admin@ogoula.com', '%@ogoula.com'])
    or coalesce((select alias from public.profiles where user_id::text = auth.uid()::text), '') ilike '%admin%'
  );

-- Note: En production, utilisez plutôt la web admin avec SERVICE_ROLE_KEY
-- pour éviter d'exposer trop de permissions via les politiques RLS
