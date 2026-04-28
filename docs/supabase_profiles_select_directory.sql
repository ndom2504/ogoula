-- =============================================================================
-- Lecture des profils des AUTRES utilisateurs (fil d’actualité, profil public)
-- =============================================================================
-- À exécuter dans Supabase → SQL Editor en entier.
--
-- Si « rien ne change » dans l’app après la 1re version : exécute aussi les GRANT
-- ci-dessous (un schéma peut avoir révoqué SELECT par défaut).
--
-- Les politiques SELECT PERMISSIVES sont cumulées en OU avec « sa ligne ».
-- =============================================================================

alter table public.profiles enable row level security;

grant usage on schema public to authenticated;
grant select on table public.profiles to authenticated;

drop policy if exists "profiles_select_directory_authenticated" on public.profiles;

-- true : tout utilisateur JWT « authenticated » peut lire toutes les lignes.
create policy "profiles_select_directory_authenticated" on public.profiles
  as permissive
  for select
  to authenticated
  using (true);

-- Vérification rapide (doit lister au moins profiles_select_own + celle-ci) :
-- select policyname, cmd, roles from pg_policies where tablename = 'profiles';
