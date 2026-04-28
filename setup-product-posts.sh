#!/bin/bash

# 🚀 OGOULA PRODUCT POSTS - SETUP SCRIPT
# This script helps you set up product posts in Supabase

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  OGOULA PRODUCT POSTS - SETUP GUIDE  ${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Check if running on macOS or Linux
OS=$(uname -s)
if [[ "$OS" != "Darwin" && "$OS" != "Linux" ]]; then
    echo -e "${RED}❌ This script only works on macOS or Linux${NC}"
    exit 1
fi

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to display step
step() {
    echo -e "${YELLOW}Step $1:${NC} $2"
}

# Function to display success
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to display error
error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

# Function to display info
info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 1. CHECK PREREQUISITES
echo -e "\n${YELLOW}=== CHECKING PREREQUISITES ===${NC}\n"

if ! command_exists jq; then
    error "jq is not installed. Install with: brew install jq"
fi
success "jq is installed"

if ! command_exists curl; then
    error "curl is not installed"
fi
success "curl is installed"

# 2. LOAD CONFIGURATION
echo -e "\n${YELLOW}=== LOADING CONFIGURATION ===${NC}\n"

# Check for local.properties
if [ ! -f "app/build.gradle.kts" ]; then
    error "Please run this script from the Ogoula project root directory"
fi
success "Found app/build.gradle.kts"

# Try to find Supabase credentials from build.gradle.kts
echo ""
info "To proceed, we need your Supabase credentials."
info "You can find them in: https://app.supabase.com/project/[YOUR-PROJECT]/settings/api"

echo ""
read -p "Enter Supabase URL (https://xxx.supabase.co): " SUPABASE_URL
read -p "Enter Supabase Anon Key: " SUPABASE_ANON_KEY

if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_ANON_KEY" ]; then
    error "Supabase credentials are required"
fi
success "Credentials loaded"

# 3. TEST CONNECTION
echo -e "\n${YELLOW}=== TESTING SUPABASE CONNECTION ===${NC}\n"

step "3.1" "Testing connection to Supabase..."

RESPONSE=$(curl -s \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
  "$SUPABASE_URL/rest/v1/posts?limit=1")

if echo "$RESPONSE" | jq . >/dev/null 2>&1; then
    success "Connection to Supabase successful"
else
    error "Failed to connect to Supabase. Check your credentials."
fi

# 4. CHECK IF TABLE EXISTS
echo -e "\n${YELLOW}=== CHECKING DATABASE SCHEMA ===${NC}\n"

step "4.1" "Checking if 'posts' table exists..."

TABLE_CHECK=$(curl -s \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
  "$SUPABASE_URL/rest/v1/posts?limit=0")

if echo "$TABLE_CHECK" | grep -q "error"; then
    error "Table 'posts' not found. Please create it first."
fi
success "Table 'posts' exists"

# 5. MIGRATION
echo -e "\n${YELLOW}=== MIGRATION: ADD PRODUCT COLUMNS ===${NC}\n"

step "5.1" "You need to run the migration in Supabase SQL Editor"
echo ""
info "Go to: https://app.supabase.com/project/[YOUR-PROJECT]/sql/new"
info "Then paste and run the SQL from: SUPABASE_SETUP.md (Migration section)"
echo ""
read -p "Have you run the migration? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    error "Migration is required to proceed"
fi
success "Migration confirmed"

# 6. INSERT TEST DATA
echo -e "\n${YELLOW}=== OPTION 1: INSERT TEST DATA VIA POSTMAN/CURL ===${NC}\n"

step "6.1" "Would you like to insert 5 Nike test products?"
read -p "Insert test data? (y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    step "6.2" "Inserting Nike Air Force 1..."
    
    INSERT_RESPONSE=$(curl -s -X POST \
      -H "apikey: $SUPABASE_ANON_KEY" \
      -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
      -H "Content-Type: application/json" \
      -H "Prefer: return=representation" \
      -d '{
        "author": "Ogoula Admin",
        "handle": "@admin",
        "content": "👟 Chaussure iconique depuis 1982",
        "time": '$(date +%s000)',
        "postType": "classique",
        "product_url": "https://www.nike.com/ca/fr/t/chaussure-air-force-1-07-pour-rWtqPn/CW2288-111",
        "product_title": "Chaussure Air Force 1 '"'"'07",
        "product_price": "120$ CAD",
        "product_image": "https://static.nike.com/a/images/c_limit,w_592,f_auto,q_auto/t_PDP_1728_v3/f33f81c5-1da7-4f76-ad05-d1e871f4a33f/chaussure-air-force-1-07-pour-CW2288-111.png"
      }' \
      "$SUPABASE_URL/rest/v1/posts")
    
    if echo "$INSERT_RESPONSE" | jq . >/dev/null 2>&1; then
        success "Test post inserted successfully"
    else
        error "Failed to insert test post: $INSERT_RESPONSE"
    fi
fi

# 7. VERIFY DATA
echo -e "\n${YELLOW}=== VERIFICATION ===${NC}\n"

step "7.1" "Verifying inserted data..."

VERIFY=$(curl -s \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
  "$SUPABASE_URL/rest/v1/posts?product_url=not.is.null&select=id,product_title,product_price")

COUNT=$(echo "$VERIFY" | jq 'length')
success "Found $COUNT product posts in database"

echo ""
echo "$VERIFY" | jq -r '.[] | "  • \(.product_title) - \(.product_price)"'

# 8. BUILD & TEST
echo -e "\n${YELLOW}=== BUILD & TEST ===${NC}\n"

step "8.1" "Build the app?"
read -p "Build APK? (y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    info "Building app..."
    if ./gradlew app:assembleDebug >/dev/null 2>&1; then
        success "APK built successfully"
        success "Location: app/build/outputs/apk/debug/app-debug.apk"
        
        info "To install: adb install -r app/build/outputs/apk/debug/app-debug.apk"
        info "Then open app → Admin Panel → Posts Produits tab"
    else
        error "Failed to build APK"
    fi
fi

# FINAL SUMMARY
echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}  ✅ SETUP COMPLETE${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo "Next steps:"
echo "  1. Install APK on device/emulator"
echo "  2. Open Admin Panel (must be logged as admin)"
echo "  3. Go to 'Posts Produits' tab"
echo "  4. Create a new product post"
echo "  5. View in feed - click 'Voir le produit' button"
echo ""
info "Documentation: cat SUPABASE_SETUP.md"
echo ""

