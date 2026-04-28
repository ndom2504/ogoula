-- Trace d’adhésion au cadre pro-contribution à l’inscription (phases de test, conformité charte)
alter table public.profiles add column if not exists pro_contribution_charter_version text;
alter table public.profiles add column if not exists pro_contribution_acknowledged_at timestamptz;

comment on column public.profiles.pro_contribution_charter_version is 'Version du texte charte / pro-contribution acceptée à l’inscription (ex. 2026.04.1)';
comment on column public.profiles.pro_contribution_acknowledged_at is 'Horodatage d’acceptation explicite à la création du profil';
