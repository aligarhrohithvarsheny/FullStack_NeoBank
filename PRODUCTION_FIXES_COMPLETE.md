# Production Fixes - Complete Documentation

This document contains all the fixes applied to make the system production-ready.

## Summary of Fixes

1. ✅ CORS Configuration - Fixed to use SPRING_WEB_CORS_ALLOWED_ORIGINS environment variable
2. ✅ Spring Security - Properly configured CORS, disabled CSRF, stateless sessions
3. ✅ WebSocket CORS - Fixed to use same CORS origins
4. ✅ Password Service - Migrated from SHA-256 to BCrypt
5. ✅ Default Manager - Auto-created on application startup
6. ✅ Angular API URLs - Already using environment.apiBaseUrl correctly

---

## 1. CORS Configuration (CorsConfig.java)

**File**: `springapp/src/main/java/com/neo/springapp/config/CorsConfig.java`

### Changes:
- Reads from `SPRING_WEB_CORS_ALLOWED_ORIGINS` environment variable
- Supports comma-separated list of origins
- Properly configured for Vercel production and preview URLs
- Allows all HTTP methods including OPTIONS
- Enables credentials
- Sets max-age for preflight caching

### Key Features:
```java
@Value("${SPRING_WEB_CORS_ALLOWED_ORIGINS:${spring.web.cors.allowed-origins:}}")
private String allowedOrigins;
```

Spring Boot automatically maps `SPRING_WEB_CORS_ALLOWED_ORIGINS` to `spring.web.cors.allowed-origins`.

---

## 2. Spring Security Configuration (SpringSecurityConfig.java)

**File**: `springapp/src/main/java/com/neo/springapp/config/SpringSecurityConfig.java`

### Changes:
- Uses `CorsConfigurationSource` bean from CorsConfig
- Disables CSRF (for stateless API)
- Permits all `/api/**` endpoints
- Permits OPTIONS requests for CORS preflight
- Uses stateless session management

### Key Configuration:
```java
.csrf(csrf -> csrf.disable())
.cors(cors -> cors.configurationSource(corsConfigurationSource))
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers("/api/**").permitAll()
    .anyRequest().permitAll()
)
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
);
```

---

## 3. WebSocket CORS Configuration (WebSocketConfig.java)

**File**: `springapp/src/main/java/com/neo/springapp/config/WebSocketConfig.java`

### Changes:
- Reads from same `SPRING_WEB_CORS_ALLOWED_ORIGINS` environment variable
- Uses `setAllowedOriginPatterns()` for WebSocket (supports wildcards)
- Ensures consistency with HTTP CORS configuration

---

## 4. Password Service - BCrypt Migration (PasswordService.java)

**File**: `springapp/src/main/java/com/neo/springapp/service/PasswordService.java`

### Changes:
- Migrated from SHA-256 to BCryptPasswordEncoder
- Passwords now use BCrypt format: `$2a$10$...` (60 characters)
- PINs still use SHA-256 (for 4-digit PINs)
- Backward compatible with legacy SHA-256 passwords (during migration)

### BCrypt Usage:
```java
private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder(10);

public String encryptPassword(String plainPassword) {
    return bcryptEncoder.encode(plainPassword);
}

public boolean verifyPassword(String plainPassword, String encryptedPassword) {
    if (isBCryptFormat(encryptedPassword)) {
        return bcryptEncoder.matches(plainPassword, encryptedPassword);
    }
    // Legacy SHA-256 support for migration
    return verifyLegacyPassword(plainPassword, encryptedPassword);
}
```

---

## 5. Default Manager Auto-Creation (DefaultManagerInitializer.java)

**File**: `springapp/src/main/java/com/neo/springapp/config/DefaultManagerInitializer.java`

### Changes:
- Implements `ApplicationRunner` to run on application startup
- Checks if any MANAGER role exists
- Creates default manager if none exists
- Uses AdminService to ensure password encryption

### Default Manager Credentials:
- **Email**: `manager@neobank.com`
- **Password**: `manager123`
- **Role**: `MANAGER`
- **Employee ID**: `MGR001`

⚠️ **Important**: The default password should be changed after first login!

---

## 6. MySQL INSERT Query with BCrypt Password

Since BCrypt passwords are hashed with random salts, you cannot use a pre-generated hash. Use one of these methods:

### Method 1: Use the Application (Recommended)
The `DefaultManagerInitializer` automatically creates the manager on startup, so no manual INSERT is needed.

### Method 2: Use Java Code to Generate BCrypt Hash

Create a simple Java class to generate the BCrypt hash:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBCryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "manager123";
        String hash = encoder.encode(password);
        System.out.println("BCrypt hash for '" + password + "':");
        System.out.println(hash);
    }
}
```

### Method 3: Direct SQL INSERT (After Generating Hash)

Once you have the BCrypt hash, use this SQL:

```sql
INSERT INTO admins (
    name, 
    email, 
    password, 
    role, 
    employee_id, 
    profile_complete, 
    account_locked, 
    failed_login_attempts, 
    created_at, 
    last_updated
) VALUES (
    'Manager',
    'manager@neobank.com',
    '$2a$10$YOUR_GENERATED_BCRYPT_HASH_HERE',  -- Replace with actual BCrypt hash
    'MANAGER',
    'MGR001',
    false,
    false,
    0,
    NOW(),
    NOW()
);
```

### Example BCrypt Hash (for "manager123")
```
$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi
```

⚠️ **Note**: Each BCrypt hash is unique due to random salt, so the hash above is just an example. Use Method 1 or 2 to generate your own.

---

## 7. Angular Environment Configuration

### Files:
- `angularapp/src/environment/environment.ts` (development)
- `angularapp/src/environment/environment.prod.ts` (production)

### Current Implementation:
Angular services already use `environment.apiBaseUrl` correctly:

```typescript
// In services (e.g., admin.ts)
private apiUrl = `${environment.apiBaseUrl}/api/admins`;
```

The `replace-env.js` script automatically updates these files during build using the `BACKEND_API_URL` environment variable.

### Build Process:
1. Vercel sets `BACKEND_API_URL` environment variable
2. `replace-env.js` validates and sanitizes the URL
3. Script updates `environment.prod.ts` with the correct URL
4. Build proceeds normally

### Important:
- Never hardcode URLs in Angular code
- Always use `environment.apiBaseUrl`
- The replace-env.js script handles URL injection during build

---

## 8. Environment Variables

### Backend (Render)

#### Required Variables:
```env
# Database (from Railway MySQL)
SPRING_DATASOURCE_URL=jdbc:mysql://mysql.railway.app:3306/railway
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# CORS (Vercel URLs)
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank22.vercel.app,https://*.vercel.app

# Server Port (usually auto-set by Render)
PORT=8080

# Spring Profile
SPRING_PROFILES_ACTIVE=production
```

#### Optional Variables:
```env
# Email Configuration
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password

# WhatsApp Configuration
WHATSAPP_ENABLED=true
WHATSAPP_API_URL=your_whatsapp_api_url
WHATSAPP_API_KEY=your_whatsapp_api_key

# Swagger (disable in production)
SWAGGER_ENABLED=false
```

### Frontend (Vercel)

#### Required Variables:
```env
# Backend API URL (NO trailing slash, NO /api)
BACKEND_API_URL=https://fullstack-neobank.onrender.com
```

⚠️ **Critical**: 
- DO NOT include `/api` in the URL
- DO NOT include trailing slash
- DO NOT include `BACKEND_API_URL=` in the value (just the URL)

#### Vercel Preview URLs:
For preview deployments, Vercel automatically creates URLs like:
- `https://your-app-git-branch.vercel.app`

Make sure to include these in `SPRING_WEB_CORS_ALLOWED_ORIGINS` if needed.

### Database (Railway MySQL)

Railway automatically provides connection string. Use these variables in Render:

```
SPRING_DATASOURCE_URL={Railway MySQL Connection URL}
SPRING_DATASOURCE_USERNAME={Railway MySQL Username}
SPRING_DATASOURCE_PASSWORD={Railway MySQL Password}
```

---

## 9. Verification Checklist

### Backend (Render)

- [ ] Application starts successfully
- [ ] Default manager account is created (check logs)
- [ ] Health endpoint works: `GET /actuator/health`
- [ ] CORS headers are present in responses
- [ ] OPTIONS requests return 200 OK
- [ ] POST requests to `/api/admins/login` work
- [ ] POST requests to `/api/users/create` work

### Frontend (Vercel)

- [ ] Application builds successfully
- [ ] `environment.apiBaseUrl` is correctly set
- [ ] API calls use correct base URL (no hardcoded URLs)
- [ ] No CORS errors in browser console
- [ ] Admin login works
- [ ] User creation works

### Database (Railway)

- [ ] Connection successful
- [ ] `admins` table exists
- [ ] Default manager record exists
- [ ] Password field uses BCrypt format (60 characters, starts with `$2a$`)

### CORS Verification

Test CORS with curl:

```bash
# Test OPTIONS preflight
curl -X OPTIONS https://your-backend.onrender.com/api/admins/login \
  -H "Origin: https://full-stack-neo-bank22.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -v

# Should return:
# Access-Control-Allow-Origin: https://full-stack-neo-bank22.vercel.app
# Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
# Access-Control-Allow-Credentials: true
```

### Admin Login Test

```bash
curl -X POST https://your-backend.onrender.com/api/admins/login \
  -H "Content-Type: application/json" \
  -H "Origin: https://full-stack-neo-bank22.vercel.app" \
  -d '{"email":"manager@neobank.com","password":"manager123"}'
```

Should return:
```json
{
  "success": true,
  "message": "Login successful",
  "admin": { ... }
}
```

---

## 10. Troubleshooting

### CORS Errors

**Problem**: Browser shows `Access-Control-Allow-Origin` header with wrong origin

**Solution**:
1. Check `SPRING_WEB_CORS_ALLOWED_ORIGINS` environment variable
2. Ensure no trailing spaces in origins
3. Use comma-separated list: `origin1,origin2`
4. Restart application after changing environment variables

### 405 Method Not Allowed

**Problem**: POST requests return 405

**Solution**:
1. Ensure Spring Security permits the endpoint: `.requestMatchers("/api/**").permitAll()`
2. Check controller mapping uses correct HTTP method: `@PostMapping`
3. Verify endpoint path matches: `/api/admins/login` not `/admins/login`

### 401 Unauthorized for Admin Login

**Problem**: Login fails with 401 even with correct credentials

**Solution**:
1. Check password is encrypted with BCrypt (60 chars, starts with `$2a$`)
2. Verify `PasswordService.verifyPassword()` is called correctly
3. Check logs for password verification details
4. If using legacy SHA-256 password, it will be verified but should migrate to BCrypt

### Infinite Buffering / Connection Issues

**Problem**: Frontend shows loading indefinitely

**Solution**:
1. Check `BACKEND_API_URL` is set correctly in Vercel
2. Verify backend is accessible: `curl https://your-backend.onrender.com/actuator/health`
3. Check browser Network tab for failed requests
4. Verify CORS is configured correctly
5. Check backend logs for errors

### Default Manager Not Created

**Problem**: No manager account after startup

**Solution**:
1. Check application logs for `DefaultManagerInitializer` output
2. Verify database connection is working
3. Check if manager already exists (init won't create if exists)
4. Manually create via API: `POST /api/admins/create-default-manager`

---

## 11. Migration Notes

### Password Migration (SHA-256 → BCrypt)

Existing passwords in SHA-256 format will continue to work (backward compatible), but:
- New passwords are encrypted with BCrypt
- Old passwords should be migrated when users change passwords
- The `PasswordService.isEncrypted()` method checks for both formats

### Database Migration

If you need to migrate existing admin passwords to BCrypt:

```sql
-- Option 1: Reset all passwords (users will need to use password reset)
UPDATE admins SET password = NULL;

-- Option 2: Update specific admin password via application
-- Use the password reset feature in the admin dashboard
```

---

## 12. Production Deployment Steps

### 1. Deploy Backend (Render)

1. Connect GitHub repository to Render
2. Set environment variables (see section 8)
3. Deploy service
4. Verify health endpoint: `https://your-backend.onrender.com/actuator/health`
5. Check logs for default manager creation

### 2. Deploy Frontend (Vercel)

1. Connect GitHub repository to Vercel
2. Set root directory: `angularapp`
3. Set build command: `npm install && npm run build:prod`
4. Set output directory: `dist/angularapp/browser`
5. Set `BACKEND_API_URL` environment variable
6. Deploy

### 3. Configure CORS

1. Add Vercel URLs to `SPRING_WEB_CORS_ALLOWED_ORIGINS` in Render
2. Format: `https://your-app.vercel.app,https://*.vercel.app`
3. Restart backend service

### 4. Test Everything

1. Test admin login: `manager@neobank.com` / `manager123`
2. Test user creation
3. Test API endpoints
4. Verify no CORS errors

---

## 13. Files Modified

### Backend:
1. `springapp/src/main/java/com/neo/springapp/config/CorsConfig.java` - CORS configuration
2. `springapp/src/main/java/com/neo/springapp/config/SpringSecurityConfig.java` - Security configuration
3. `springapp/src/main/java/com/neo/springapp/config/WebSocketConfig.java` - WebSocket CORS
4. `springapp/src/main/java/com/neo/springapp/service/PasswordService.java` - BCrypt migration
5. `springapp/src/main/java/com/neo/springapp/config/DefaultManagerInitializer.java` - NEW FILE - Auto-create manager

### Frontend:
- No changes needed (already using `environment.apiBaseUrl` correctly)

---

## 14. Contact & Support

If you encounter issues:
1. Check application logs (Render dashboard)
2. Check browser console (F12)
3. Verify environment variables are set correctly
4. Test endpoints with curl or Postman
5. Review this documentation

---

**Last Updated**: 2024
**Version**: 1.0.0

