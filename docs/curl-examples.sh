#!/bin/bash

# Instagram API Integration - cURL Examples
# ==========================================
#
# Prerequisites:
# 1. Application running on http://localhost:8080
# 2. PostgreSQL database configured
# 3. Environment variables set (INSTAGRAM_CLIENT_ID, INSTAGRAM_CLIENT_SECRET)

BASE_URL="http://localhost:8080"
USER_ID="user123"

echo "========================================"
echo "Instagram API Integration - Test Suite"
echo "========================================"
echo ""

# ===========================================
# STEP 1: Check Link Status (should be false)
# ===========================================
echo "1. Checking link status (before linking)..."
echo "-------------------------------------------"
curl -s -X GET "${BASE_URL}/api/instagram/status?userId=${USER_ID}" | python3 -m json.tool 2>/dev/null || curl -s -X GET "${BASE_URL}/api/instagram/status?userId=${USER_ID}"
echo ""
echo ""

# ===========================================
# STEP 2: Initiate OAuth Flow
# ===========================================
echo "2. Initiating OAuth flow..."
echo "---------------------------"
echo "Request: GET ${BASE_URL}/api/instagram/link?userId=${USER_ID}"
echo ""
RESPONSE=$(curl -s -X GET "${BASE_URL}/api/instagram/link?userId=${USER_ID}")
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
echo ">>> Copy the 'authorizationUrl' from above and open it in your browser"
echo ">>> After authorization, Facebook will redirect to /api/instagram/callback"
echo ""

# ===========================================
# STEP 3: Fetch Data (after OAuth completion)
# ===========================================
echo "3. Fetching Instagram data..."
echo "-----------------------------"
echo "Request: GET ${BASE_URL}/api/instagram/data?userId=${USER_ID}&mediaLimit=10"
echo ""
curl -s -X GET "${BASE_URL}/api/instagram/data?userId=${USER_ID}&mediaLimit=10" | python3 -m json.tool 2>/dev/null || curl -s -X GET "${BASE_URL}/api/instagram/data?userId=${USER_ID}&mediaLimit=10"
echo ""
echo ""

# ===========================================
# STEP 4: Refresh Token
# ===========================================
echo "4. Refreshing access token..."
echo "-----------------------------"
echo "Request: POST ${BASE_URL}/api/instagram/refresh?userId=${USER_ID}"
echo ""
curl -s -X POST "${BASE_URL}/api/instagram/refresh?userId=${USER_ID}" | python3 -m json.tool 2>/dev/null || curl -s -X POST "${BASE_URL}/api/instagram/refresh?userId=${USER_ID}"
echo ""
echo ""

# ===========================================
# STEP 5: Check Link Status (should be true)
# ===========================================
echo "5. Checking link status (after linking)..."
echo "------------------------------------------"
curl -s -X GET "${BASE_URL}/api/instagram/status?userId=${USER_ID}" | python3 -m json.tool 2>/dev/null || curl -s -X GET "${BASE_URL}/api/instagram/status?userId=${USER_ID}"
echo ""
echo ""

# ===========================================
# STEP 6: Unlink Account (optional)
# ===========================================
echo "6. Unlinking account (keeping data)..."
echo "--------------------------------------"
echo "Request: DELETE ${BASE_URL}/api/instagram/unlink?userId=${USER_ID}&deleteData=false"
echo ""
# Uncomment to execute:
# curl -s -X DELETE "${BASE_URL}/api/instagram/unlink?userId=${USER_ID}&deleteData=false" | python3 -m json.tool
echo "(Skipped - uncomment in script to execute)"
echo ""
echo ""

echo "========================================"
echo "Individual cURL Commands for Reference"
echo "========================================"
echo ""

cat << 'EOF'
# 1. Initiate OAuth Flow
curl -X GET "http://localhost:8080/api/instagram/link?userId=user123"

# 2. Fetch Instagram Data (profile + media)
curl -X GET "http://localhost:8080/api/instagram/data?userId=user123&mediaLimit=25"

# 3. Refresh Token
curl -X POST "http://localhost:8080/api/instagram/refresh?userId=user123"

# 4. Check Link Status
curl -X GET "http://localhost:8080/api/instagram/status?userId=user123"

# 5. Unlink Account (keep data)
curl -X DELETE "http://localhost:8080/api/instagram/unlink?userId=user123&deleteData=false"

# 6. Unlink Account (delete all data)
curl -X DELETE "http://localhost:8080/api/instagram/unlink?userId=user123&deleteData=true"
EOF

echo ""
echo "========================================"
echo "Done!"
echo "========================================"
