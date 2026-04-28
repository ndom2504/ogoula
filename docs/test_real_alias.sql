-- TEST AVEC L'ALIAS RÉEL
-- Exécuter dans Supabase SQL Editor

SELECT 
    'Test avec alias réel' as test_type,
    COUNT(*) as found
FROM profiles 
WHERE alias = '@morel_stevens_ndong';

-- Vérification complète du profil
SELECT 
    user_id,
    alias,
    first_name,
    last_name,
    'Profil trouvé et accessible' as status
FROM profiles 
WHERE alias = '@morel_stevens_ndong';
