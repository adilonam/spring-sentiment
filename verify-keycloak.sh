#!/bin/bash

# Keycloak Configuration Verification Script
# This script helps verify that your Keycloak is configured correctly for user registration

echo "🔍 Keycloak Configuration Verification"
echo "========================================"

# Load environment variables if .env exists
if [ -f .env ]; then
    echo "📁 Loading environment variables from .env file..."
    export $(grep -v '^#' .env | xargs)
else
    echo "⚠️  No .env file found. Make sure environment variables are set."
fi

# Set defaults if not provided
KEYCLOAK_URL=${KEYCLOAK_URL:-"http://localhost:8080"}
KEYCLOAK_REALM=${KEYCLOAK_REALM:-"master"}
KEYCLOAK_CLIENT_ID=${KEYCLOAK_CLIENT_ID:-"spring-gateway"}

echo ""
echo "🔧 Configuration:"
echo "  Keycloak URL: $KEYCLOAK_URL"
echo "  Realm: $KEYCLOAK_REALM"
echo "  Client ID: $KEYCLOAK_CLIENT_ID"
echo "  Client Secret: ${KEYCLOAK_CLIENT_SECRET:+[SET]}${KEYCLOAK_CLIENT_SECRET:-[NOT SET]}"

echo ""
echo "🌐 Testing Keycloak connectivity..."

# Test if Keycloak is reachable
TOKEN_ENDPOINT="$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token"
USERS_ENDPOINT="$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM/users"

echo "  Token endpoint: $TOKEN_ENDPOINT"
echo "  Users endpoint: $USERS_ENDPOINT"

# Check if Keycloak is running
if curl -s --connect-timeout 5 "$KEYCLOAK_URL" > /dev/null; then
    echo "  ✅ Keycloak is reachable"
else
    echo "  ❌ Keycloak is not reachable at $KEYCLOAK_URL"
    echo "     Make sure Keycloak is running and the URL is correct"
    exit 1
fi

# Check if realm exists
if curl -s "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM" > /dev/null; then
    echo "  ✅ Realm '$KEYCLOAK_REALM' exists"
else
    echo "  ❌ Realm '$KEYCLOAK_REALM' not found"
    echo "     Please create the realm or check the realm name"
    exit 1
fi

echo ""
echo "🔐 Testing client credentials..."

if [ -z "$KEYCLOAK_CLIENT_SECRET" ]; then
    echo "  ❌ Client secret not set"
    echo "     Please set KEYCLOAK_CLIENT_SECRET in your .env file"
    exit 1
fi

# Try to get admin token
echo "  🔄 Attempting to get admin token..."

TOKEN_RESPONSE=$(curl -s -X POST "$TOKEN_ENDPOINT" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=$KEYCLOAK_CLIENT_ID" \
    -d "client_secret=$KEYCLOAK_CLIENT_SECRET" 2>/dev/null)

if echo "$TOKEN_RESPONSE" | grep -q "access_token"; then
    echo "  ✅ Successfully obtained admin token"
    
    # Extract token
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)
    
    echo ""
    echo "🛡️  Testing admin permissions..."
    
    # Test if we can access users endpoint
    USERS_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$USERS_ENDPOINT")
    
    if [ "$USERS_RESPONSE" = "200" ]; then
        echo "  ✅ Admin permissions verified - can access users endpoint"
        echo ""
        echo "🎉 Configuration looks good!"
        echo ""
        echo "📝 Next steps:"
        echo "   1. Restart your Spring Boot application"
        echo "   2. Test user registration endpoint"
        echo "   3. Check application logs for any remaining issues"
    elif [ "$USERS_RESPONSE" = "403" ]; then
        echo "  ❌ 403 Forbidden - Client doesn't have admin permissions"
        echo ""
        echo "🔧 To fix this:"
        echo "   1. Go to Keycloak Admin Console: $KEYCLOAK_URL/admin"
        echo "   2. Navigate to Clients → $KEYCLOAK_CLIENT_ID"
        echo "   3. Go to 'Service account roles' tab"
        echo "   4. Click 'Assign role'"
        echo "   5. Filter by 'realm-management'"
        echo "   6. Assign 'manage-users' and 'view-users' roles"
        echo ""
        echo "   See KEYCLOAK_ADMIN_SETUP.md for detailed instructions"
    elif [ "$USERS_RESPONSE" = "401" ]; then
        echo "  ❌ 401 Unauthorized - Token is invalid"
        echo "     Check if client authentication is enabled and secret is correct"
    else
        echo "  ❌ Unexpected response: $USERS_RESPONSE"
        echo "     Check Keycloak logs for more details"
    fi
    
else
    echo "  ❌ Failed to get admin token"
    echo ""
    echo "🔧 Common issues:"
    echo "   - Wrong client secret"
    echo "   - Client authentication not enabled"
    echo "   - Service accounts roles not enabled"
    echo "   - Client doesn't exist"
    echo ""
    echo "Token response: $TOKEN_RESPONSE"
fi

echo ""
echo "📚 For detailed setup instructions, see:"
echo "   - KEYCLOAK_SETUP.md"
echo "   - KEYCLOAK_ADMIN_SETUP.md"
