-- Supabase → SQL Editor
-- Supprime toutes les stories (actives ou suspendues) pour repartir à zéro.
-- Les nouvelles stories seront recréées depuis l’app après publication.

delete from public.stories;
