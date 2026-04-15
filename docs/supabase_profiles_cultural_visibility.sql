-- Visibilité des infos d’engagement (pays, intentions, rôle, phrase) pour les autres utilisateurs
alter table public.profiles add column if not exists cultural_profile_visibility text default 'everyone';

update public.profiles
set cultural_profile_visibility = 'everyone'
where cultural_profile_visibility is null
   or cultural_profile_visibility not in ('everyone', 'followers_only', 'hidden');

comment on column public.profiles.cultural_profile_visibility is
  'everyone | followers_only | hidden — qui peut voir le bloc engagement sur le profil public';

do $$
begin
  alter table public.profiles
    add constraint profiles_cultural_visibility_check
    check (cultural_profile_visibility in ('everyone', 'followers_only', 'hidden'));
exception
  when duplicate_object then null;
end $$;
