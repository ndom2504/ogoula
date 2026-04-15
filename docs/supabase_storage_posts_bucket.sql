-- =============================================================================
-- Bucket Storage « posts » — images / vidéos des posts et stories
-- =============================================================================
-- Si l’upload story échoue alors que l’INSERT SQL `stories` réussit, vérifie ce bucket
-- (même nom que StorageRepository.postsBucket = "posts").
-- =============================================================================

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'posts',
  'posts',
  true,
  104857600,
  array['image/jpeg', 'image/png', 'image/webp', 'video/mp4']::text[]
)
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = coalesce(excluded.file_size_limit, storage.buckets.file_size_limit),
      allowed_mime_types = coalesce(excluded.allowed_mime_types, storage.buckets.allowed_mime_types);

drop policy if exists "storage_posts_select_public" on storage.objects;
create policy "storage_posts_select_public"
  on storage.objects for select
  to public
  using (bucket_id = 'posts');

-- Insert : tout utilisateur authentifié (posts = postId/…, stories = userId/stories/…, videos = videos/…).
drop policy if exists "storage_posts_insert_auth" on storage.objects;
create policy "storage_posts_insert_auth"
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'posts');

drop policy if exists "storage_posts_update_auth" on storage.objects;
create policy "storage_posts_update_auth"
  on storage.objects for update
  to authenticated
  using (bucket_id = 'posts')
  with check (bucket_id = 'posts');

drop policy if exists "storage_posts_delete_auth" on storage.objects;
create policy "storage_posts_delete_auth"
  on storage.objects for delete
  to authenticated
  using (bucket_id = 'posts');
