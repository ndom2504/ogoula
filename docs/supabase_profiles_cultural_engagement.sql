-- Exécuter dans Supabase → SQL Editor (une fois)
-- Engagement culturel à l’inscription / création de profil Ogoula

alter table public.profiles add column if not exists cultural_reference_country text;
alter table public.profiles add column if not exists cultural_intentions text;
alter table public.profiles add column if not exists self_role text;
alter table public.profiles add column if not exists contribution_sentence text;

comment on column public.profiles.cultural_reference_country is 'Pays ou option de lien culturel (libellé affiché)';
comment on column public.profiles.cultural_intentions is 'Identifiants d’intention, séparés par des virgules (max 2)';
comment on column public.profiles.self_role is 'Identifiant de rôle auto-déclaré';
comment on column public.profiles.contribution_sentence is 'Phrase de contribution (15–200 car.)';
