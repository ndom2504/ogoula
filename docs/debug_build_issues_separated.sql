-- SCRIPT DE DIAGNOSTIC SÉPARÉ - Exécuter chaque requête individuellement

-- REQUÊTE 1: Total des profils
SELECT 
    'Total profils' as type,
    COUNT(*) as count
FROM profiles;

-- REQUÊTE 2: Profils avec alias
SELECT 
    'Profils avec alias' as type,
    COUNT(*) as count
FROM profiles 
WHERE alias IS NOT NULL AND alias != '';

-- REQUÊTE 3: Profils avec prénom
SELECT 
    'Profils avec prénom' as type,
    COUNT(*) as count
FROM profiles 
WHERE first_name IS NOT NULL AND first_name != '';

-- REQUÊTE 4: Afficher les profils existants
SELECT 
    user_id,
    alias,
    first_name,
    last_name,
    'Profil existant' as status
FROM profiles 
LIMIT 10;

-- REQUÊTE 5: Test avec un alias spécifique (remplacer @votre_alias)
SELECT 
    'Test direct alias' as test_type,
    COUNT(*) as found
FROM profiles 
WHERE alias = '@votre_alias';
