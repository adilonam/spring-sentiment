# Keycloak Admin Configuration for User Registration

This guide will help you configure Keycloak properly to allow your Spring Boot application to register users.

## Problem
You're getting a 403 FORBIDDEN error when trying to register users because your client doesn't have the necessary admin permissions.

## Solution Steps

### Step 1: Configure Client for Service Account

1. **Access Keycloak Admin Console**
   - URL: http://localhost:5050/admin (based on your error message)
   - Or: http://localhost:8080/admin
   - Username: admin
   - Password: admin

2. **Configure the Client**
   - Go to **Clients** → Find your client `spring-gateway`
   - If it doesn't exist, create it:
     - Client ID: `spring-gateway`
     - Client Protocol: `openid-connect`
     - Access Type: `confidential`

3. **Enable Service Account**
   - In your client settings:
     - **Client authentication**: ON
     - **Authorization**: OFF (unless you need fine-grained permissions)
     - **Authentication flow**:
       - Standard flow: ON
       - Direct access grants: ON
       - **Service accounts roles**: ON ← **This is crucial!**
   - Click **Save**

4. **Get Client Secret**
   - Go to **Credentials** tab
   - Copy the **Client Secret**
   - Update your `.env` file with this secret

### Step 2: Assign Admin Roles to Service Account

1. **Go to Service Account Roles**
   - In your client → **Service account roles** tab
   - Click **Assign role**

2. **Add Realm Management Roles**
   - Click **Filter by clients**
   - Select **realm-management** from the dropdown
   - Assign these roles:
     - `manage-users` ← **Essential for creating users**
     - `view-users` ← **Essential for user operations**
     - `manage-clients` (optional, for client management)
     - `view-realm` (optional, for realm info)

3. **Alternative: Assign realm-admin Role**
   - If you want full admin access, you can assign:
     - `realm-admin` (gives full realm administration rights)

### Step 3: Verify Client Configuration

Your client should have these settings:

```
Client ID: spring-gateway
Client authentication: ON
Service accounts roles: ON
Valid redirect URIs: http://localhost:8080/*
Web origins: http://localhost:8080
```

### Step 4: Update Environment Variables

Make sure your `.env` file has the correct values:

```bash
# Keycloak Configuration
KEYCLOAK_URL=http://localhost:5050  # Use the correct port
KEYCLOAK_REALM=master
KEYCLOAK_CLIENT_ID=spring-gateway
KEYCLOAK_CLIENT_SECRET=your-actual-client-secret-from-step-1.4
```

### Step 5: Alternative Realm Configuration (Recommended)

Instead of using the `master` realm, consider creating a dedicated realm:

1. **Create a New Realm**
   - Go to **Realm** dropdown (top-left) → **Add realm**
   - Name: `spring-gateway-realm`
   - Click **Create**

2. **Create Client in New Realm**
   - Follow steps 1-2 above but in the new realm

3. **Update Configuration**
   ```bash
   KEYCLOAK_REALM=spring-gateway-realm
   ```

## Testing the Fix

1. **Restart your Spring Boot application**
   ```bash
   mvn spring-boot:run
   ```

2. **Test user registration**
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "testuser",
       "email": "test@example.com",
       "password": "password123",
       "firstName": "Test",
       "lastName": "User"
     }'
   ```

## Verification Commands

### Check if service account has correct roles:
1. Go to your client → **Service account roles** tab
2. Verify you see `manage-users` and `view-users` roles listed

### Check if admin token is working:
Look at your application logs for:
- "Successfully obtained admin token for client: spring-gateway"
- "Creating user in Keycloak with payload: ..."

## Common Issues

### Issue 1: Still getting 403 after configuration
- **Solution**: Make sure you saved the client configuration
- **Verify**: Service account roles are properly assigned
- **Check**: You're using the correct realm

### Issue 2: Client secret mismatch
- **Solution**: Copy the secret exactly from Keycloak Credentials tab
- **Verify**: No extra spaces or characters in `.env` file

### Issue 3: Wrong Keycloak URL
- **Solution**: Check which port Keycloak is running on
- **Verify**: Your error shows `localhost:5050`, update accordingly

## Security Note

The `manage-users` role gives significant permissions. In production:
1. Use a dedicated realm (not `master`)
2. Consider creating custom roles with minimal required permissions
3. Use proper secret management (not `.env` files)
4. Enable TLS/HTTPS

## Next Steps

After fixing the 403 error:
1. Test user registration thoroughly
2. Verify users are created in both Keycloak and your database
3. Test the complete authentication flow
4. Consider implementing proper error handling for different scenarios
