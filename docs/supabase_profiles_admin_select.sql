-- =============================================================================
-- Option : laisser l’admin web lire TOUS les profils avec la clé anon + JWT
-- =============================================================================
-- Par défaut, si la RLS sur `public.profiles` n’autorise que « sa » ligne,
-- le dashboard admin (même connecté) ne voit qu’un utilisateur.
--
-- Préféré en prod : route API Next.js + SUPABASE_SERVICE_ROLE_KEY (voir web/.env.example).
--
-- Si tu ne veux pas exposer la service role sur le serveur web, tu peux ajouter
-- UNE politique SELECT supplémentaire (les politiques sont combinées en OU) :
-- remplace l’email par celui du compte administrateur Supabase Auth.
-- =============================================================================

drop policy if exists "profiles_select_admin_jwt_email" on public.profiles;
create policy "profiles_select_admin_jwt_email" on public.profiles
  for select
  to authenticated
  using (
    coalesce((select auth.jwt() ->> 'email'), '') ilike 'info@misterdil.ca'
  );
