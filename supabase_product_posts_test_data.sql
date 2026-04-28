-- Posts Produits - Données de Test pour Ogoula
-- Exécute ce script dans Supabase SQL Editor pour charger les posts de test

-- Post 1: Nike Air Force 1
INSERT INTO posts (
  id,
  author,
  handle,
  content,
  time,
  validates,
  disvalidates,
  loves,
  favorites,
  shares,
  views,
  post_type,
  author_image_uri,
  product_url,
  product_title,
  product_price,
  product_image
) VALUES (
  'prod-001-nike-af1',
  'Ogoula Admin',
  '@admin',
  'Chaussure Air Force 1 ''07',
  extract(epoch from now()) * 1000,
  15,
  2,
  42,
  18,
  7,
  234,
  'classique',
  NULL,
  'https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111',
  'Chaussure Air Force 1 ''07',
  '120$ CAD',
  'https://static.nike.com/a/images/t_PDP_1728_v2/f_auto,q_auto:eco/rhvjxqhrxgpgphkwmgit/CHAUSSURE%20AIR%20FORCE%201%2707-rWtqPn.jpg'
) ON CONFLICT (id) DO NOTHING;

-- Post 2: Nike Blazer Mid
INSERT INTO posts (
  id,
  author,
  handle,
  content,
  time,
  validates,
  disvalidates,
  loves,
  favorites,
  shares,
  views,
  post_type,
  author_image_uri,
  product_url,
  product_title,
  product_price,
  product_image
) VALUES (
  'prod-002-nike-blazer',
  'Ogoula Admin',
  '@admin',
  'Nike Blazer Mid ''77 Vintage',
  extract(epoch from now()) * 1000 - 3600000,
  12,
  1,
  38,
  14,
  5,
  189,
  'classique',
  NULL,
  'https://www.nike.com/ca/fr/t/nike-blazer-mid-77-vintage/DA6624-100',
  'Nike Blazer Mid ''77 Vintage',
  '110$ CAD',
  'https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/wqbcrvndjhjcugfvj0vw/NIKE%20BLAZER%20MID%2777%20VINTAGE-DA6624-100.jpg'
) ON CONFLICT (id) DO NOTHING;

-- Post 3: Nike Dunk Low
INSERT INTO posts (
  id,
  author,
  handle,
  content,
  time,
  validates,
  disvalidates,
  loves,
  favorites,
  shares,
  views,
  post_type,
  author_image_uri,
  product_url,
  product_title,
  product_price,
  product_image
) VALUES (
  'prod-003-nike-dunk',
  'Ogoula Admin',
  '@admin',
  'Nike Dunk Low Retro',
  extract(epoch from now()) * 1000 - 7200000,
  18,
  3,
  56,
  22,
  9,
  312,
  'classique',
  NULL,
  'https://www.nike.com/ca/fr/t/nike-dunk-low-retro/DD1391-100',
  'Nike Dunk Low Retro',
  '125$ CAD',
  'https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/jkfnqsw8z0m7r4vb3xpm/NIKE%20DUNK%20LOW%20RETRO-DD1391-100.jpg'
) ON CONFLICT (id) DO NOTHING;

-- Post 4: Nike Jordan 1 Retro
INSERT INTO posts (
  id,
  author,
  handle,
  content,
  time,
  validates,
  disvalidates,
  loves,
  favorites,
  shares,
  views,
  post_type,
  author_image_uri,
  product_url,
  product_title,
  product_price,
  product_image
) VALUES (
  'prod-004-jordan-1',
  'Ogoula Admin',
  '@admin',
  'Air Jordan 1 Retro High',
  extract(epoch from now()) * 1000 - 10800000,
  21,
  2,
  67,
  28,
  12,
  423,
  'classique',
  NULL,
  'https://www.nike.com/ca/fr/t/air-jordan-1-retro-high/555088-123',
  'Air Jordan 1 Retro High',
  '185$ CAD',
  'https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/4d0c1e4b2f8a3c9e1b7d/AIR%20JORDAN%201%20RETRO%20HIGH-555088-123.jpg'
) ON CONFLICT (id) DO NOTHING;

-- Post 5: Nike React Infinity Run
INSERT INTO posts (
  id,
  author,
  handle,
  content,
  time,
  validates,
  disvalidates,
  loves,
  favorites,
  shares,
  views,
  post_type,
  author_image_uri,
  product_url,
  product_title,
  product_price,
  product_image
) VALUES (
  'prod-005-react-infinity',
  'Ogoula Admin',
  '@admin',
  'Nike React Infinity Run Flyknit',
  extract(epoch from now()) * 1000 - 14400000,
  9,
  1,
  31,
  11,
  4,
  156,
  'classique',
  NULL,
  'https://www.nike.com/ca/fr/t/nike-react-infinity-run-flyknit/CT2357-001',
  'Nike React Infinity Run Flyknit',
  '175$ CAD',
  'https://static.nike.com/a/images/t_PDP_1728/f_auto,q_auto:eco/7f2e1a4c8b3d9e5f1c2a/NIKE%20REACT%20INFINITY%20RUN%20FLYKNIT-CT2357-001.jpg'
) ON CONFLICT (id) DO NOTHING;

-- Optionnel: Vérifie les posts insérés
SELECT COUNT(*) as total_product_posts, 
       COUNT(CASE WHEN product_url IS NOT NULL THEN 1 END) as posts_with_urls
FROM posts
WHERE product_url IS NOT NULL;
