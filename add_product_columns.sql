-- Ajouter les colonnes produits à la table posts
-- Exécute ce script dans Supabase → SQL Editor

ALTER TABLE public.posts 
ADD COLUMN IF NOT EXISTS product_url TEXT,
ADD COLUMN IF NOT EXISTS product_title TEXT,
ADD COLUMN IF NOT EXISTS product_price TEXT,
ADD COLUMN IF NOT EXISTS product_image TEXT;

-- Vérifier que les colonnes sont bien créées
SELECT column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'posts' 
  AND table_schema = 'public'
  AND column_name LIKE 'product_%'
ORDER BY column_name;
