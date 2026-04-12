-- Exécuter dans Supabase → SQL Editor (une fois)
-- Modération des comptes : statut, fin de suspension, motif

alter table public.profiles
  add column if not exists account_status text default 'active';

alter table public.profiles
  add column if not exists suspended_until timestamptz null;

alter table public.profiles
  add column if not exists moderation_note text null;

update public.profiles
set account_status = 'active'
where account_status is null;

-- Optionnel : empêcher les valeurs invalides (ignorer si la contrainte existe déjà)
do $$
begin
  alter table public.profiles
    add constraint profiles_account_status_check
    check (account_status in ('active', 'suspended', 'banned'));
exception
  when duplicate_object then null;
end $$;

-- Les mises à jour depuis le panneau admin (clé anon) exigent une politique RLS adaptée.
-- En production : Edge Function avec service_role, ou politique limitée aux emails admin.
-- Exemple minimal (à affiner) :
-- create policy "profiles_admin_update" on public.profiles for update
--   to authenticated using (true) with check (true);
