#!/bin/bash
# 🚀 Quick Commands - Ogoula Product Posts Testing

# ═══════════════════════════════════════════════════════════════

# 1️⃣ COMPILATION RAPIDE
echo "🔨 Compiling Kotlin..."
cd /Users/morelsttevensndong/ogoula
./gradlew app:compileDebugKotlin

# 2️⃣ BUILD APK
echo "📦 Building APK..."
./gradlew app:assembleDebug

# 3️⃣ INSTALLATION (nécessite Android Device/Emulator)
echo "📱 Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4️⃣ VOIR LES LOGS
echo "📋 Watching logs..."
adb logcat | grep "AdminScreen\|ProductPost"

# ═══════════════════════════════════════════════════════════════

# 5️⃣ CLEAN BUILD (si problèmes)
echo "🧹 Cleaning build..."
./gradlew clean
./gradlew app:assembleDebug

# ═══════════════════════════════════════════════════════════════

# 📊 STATISTIQUES BUILDS
echo "📈 Build Statistics..."
ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print "APK Size: " $5}'

# ═══════════════════════════════════════════════════════════════

# 🧪 TESTING CHECKLIST
cat << "EOF"

✅ TESTING CHECKLIST - Posts Produits

1. AdminScreen Access:
   ✓ Ouvre l'app
   ✓ Accès Admin (alias contient "admin")
   ✓ Vérifie que "Posts Produits" tab existe

2. Create Product Post:
   ✓ Remplis URL, Title, Price, Image
   ✓ Clique "Créer le post produit"
   ✓ Voir success message

3. Feed Verification:
   ✓ Va à HomeScreen
   ✓ Post apparaît avec auteur "Ogoula Admin"
   ✓ Bouton "🔗 Voir le produit" visible
   ✓ Clique le bouton → Browser opens URL

4. Database Check (Supabase):
   SELECT COUNT(*) FROM posts WHERE product_url IS NOT NULL;
   ✓ Compte > 0 posts produits

5. Performance:
   ✓ Feed loads < 2s
   ✓ Scroll smooth (pas de lag)
   ✓ Pagination works (load more at bottom)

EOF

# ═══════════════════════════════════════════════════════════════

# 🔐 ADMIN CREDENTIALS (FOR TESTING)
cat << "EOF"

🔑 TEST CREDENTIALS

Pour accéder à AdminScreen:
- Username/Email: admin@ogoula.com
- Password: admin123

OU

- Handle contenant "admin"
- Example: @admin, @ogoula.admin, etc.

EOF

# ═══════════════════════════════════════════════════════════════

# 🎯 PRODUCT URLS (FOR QUICK TESTING)
cat << "EOF"

🛍️ QUICK TEST PRODUCT URLS

Copy/Paste dans AdminScreen:

1. Air Force 1:
   URL: https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111
   Title: Chaussure Air Force 1 '07
   Price: 120$ CAD

2. Blazer Mid:
   URL: https://www.nike.com/ca/fr/t/nike-blazer-mid-77-vintage/DA6624-100
   Title: Nike Blazer Mid '77 Vintage
   Price: 110$ CAD

3. Dunk Low:
   URL: https://www.nike.com/ca/fr/t/nike-dunk-low-retro/DD1391-100
   Title: Nike Dunk Low Retro
   Price: 125$ CAD

EOF

echo "✅ Setup complete!"
echo "Run: ./quick_test.sh to execute all commands"
