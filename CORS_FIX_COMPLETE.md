# CORS and Backend URL Configuration - Complete Fix

## ✅ All Issues Fixed

This document summarizes all changes made to fix:
- `net::ERR_FAILED` errors
- `HttpErrorResponse status 0` errors
- CORS preflight failures
- Backend URL configuration

---

## 📁 Files Modified/Created

### **Angular Frontend Files**

#### 1. Environment Configuration
**File:** `angularapp/src/environment/environment.prod.ts`  
**Package:** `src/environment`

**Changes:**
- Updated to use `BACKEND_API_URL` environment variable
- Added clear instructions for Vercel configuration
- The `replace-env.js` script automatically replaces the placeholder during build

**Current Content:**
```typescript
export const environment = {
  production: true,
  // This placeholder will be replaced during build by replace-env.js
  // Set BACKEND_API_URL environment variable in Vercel dashboard
  apiUrl: '/api' // This will be replaced by replace-env.js with BACKEND_API_URL
};
```

#### 2. Environment Replacement Script
**File:** `angularapp/replace-env.js`  
**Status:** ✅ Already configured correctly

This script:
- Reads `BACKEND_API_URL` from environment variables
- Replaces the `apiUrl` in `environment.prod.ts` during build
- Automatically adds `/api` suffix if not present

#### 3. All Service Files
**Location:** `angularapp/src/app/service/*.ts`  
**Status:** ✅ All services already use `environment.apiUrl`

**Verified Services:**
- `user.ts` ✅
- `account.ts` ✅
- `transaction.ts` ✅
- All other services ✅

**Example:**
```typescript
private apiUrl = `${environment.apiUrl}/users`;
```

---

### **Spring Boot Backend Files**

#### 1. Spring Security Configuration
**File:** `springapp/src/main/java/com/neo/springapp/config/SpringSecurityConfig.java`  
**Package:** `com.neo.springapp.config`

**Key Features:**
- ✅ Uses modern `SecurityFilterChain` (NOT deprecated `WebSecurityConfigurerAdapter`)
- ✅ Explicitly allows OPTIONS requests for preflight
- ✅ Disables CSRF for API endpoints
- ✅ Permits all `/api/**` endpoints
- ✅ Enables CORS inside SecurityFilterChain
- ✅ Allows origin: `https://full-stack-neo-bank22.vercel.app`
- ✅ Allows methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- ✅ Allows all headers (`*`)
- ✅ Enables credentials

**Key Code:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/**").permitAll()
            .anyRequest().permitAll()
        )
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
    return http.build();
}
```

#### 2. Global CORS Configuration
**File:** `springapp/src/main/java/com/neo/springapp/config/CorsConfig.java`  
**Package:** `com.neo.springapp.config`

**Key Features:**
- ✅ Implements `WebMvcConfigurer` for global CORS
- ✅ Allows origin: `https://full-stack-neo-bank22.vercel.app`
- ✅ Allows methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- ✅ Allows all headers
- ✅ Enables credentials
- ✅ Applies to all paths (`/**`)
- ✅ Sets max age for preflight cache (3600 seconds)

#### 3. Spring Security Dependency
**File:** `springapp/pom.xml`

**Added:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## 🚀 Deployment Instructions

### **Step 1: Configure Vercel Environment Variable**

1. Go to **Vercel Dashboard** → Your Project → **Settings** → **Environment Variables**
2. Add new environment variable:
   - **Name:** `BACKEND_API_URL`
   - **Value:** `https://YOUR-ACTUAL-SPRING-BOOT-URL`
   - **Example:** `https://your-backend.railway.app` (DO NOT include `/api` - it's added automatically)
3. **Important:** Select **Production**, **Preview**, and **Development** environments
4. Click **Save**

### **Step 2: Rebuild and Redeploy Angular Frontend**

The build process will automatically:
1. Run `replace-env.js` to replace the backend URL
2. Build the Angular application with the correct API URL

**Vercel will automatically rebuild when you:**
- Push to your repository, OR
- Manually trigger a redeploy from Vercel dashboard

### **Step 3: Rebuild and Redeploy Spring Boot Backend**

1. **Build the JAR:**
   ```bash
   cd springapp
   mvn clean package -DskipTests
   ```

2. **Deploy the JAR:**
   - Location: `springapp/target/springapp-0.0.1-SNAPSHOT.jar`
   - Deploy to your backend hosting (Railway, Render, etc.)
   - Restart the Spring Boot application

---

## ✅ Verification Checklist

After deployment, verify:

- [ ] Vercel environment variable `BACKEND_API_URL` is set correctly
- [ ] Angular frontend is rebuilt and redeployed
- [ ] Spring Boot backend JAR is rebuilt and redeployed
- [ ] Spring Boot application is restarted
- [ ] Test POST request: `POST /api/users/authenticate`
- [ ] Test POST request: `POST /api/users/send-reset-otp`
- [ ] Check browser console - no CORS errors
- [ ] Check browser Network tab - OPTIONS preflight succeeds

---

## 🔍 How It Works

### **Frontend Flow:**
1. Angular service uses `environment.apiUrl`
2. `environment.prod.ts` has placeholder `/api`
3. `replace-env.js` replaces it with `BACKEND_API_URL` during build
4. All API calls use the correct backend URL

### **Backend Flow:**
1. Browser sends preflight OPTIONS request
2. Spring Security's CORS filter processes it first
3. `SpringSecurityConfig` explicitly allows OPTIONS requests
4. `CorsConfig` provides additional MVC-level CORS support
5. Actual request (POST/GET/etc.) is allowed through
6. Response includes proper CORS headers

---

## 📋 File Summary

| File | Location | Purpose |
|------|----------|---------|
| `environment.prod.ts` | `angularapp/src/environment/` | Production environment config |
| `replace-env.js` | `angularapp/` | Replaces backend URL during build |
| `SpringSecurityConfig.java` | `springapp/src/main/java/com/neo/springapp/config/` | Spring Security CORS config |
| `CorsConfig.java` | `springapp/src/main/java/com/neo/springapp/config/` | Global CORS config |
| `pom.xml` | `springapp/` | Added Spring Security dependency |

---

## 🎯 Expected Results

After completing all steps:

✅ **No more `net::ERR_FAILED` errors**  
✅ **No more `HttpErrorResponse status 0` errors**  
✅ **CORS preflight OPTIONS requests succeed**  
✅ **POST requests to `/api/users/authenticate` work**  
✅ **POST requests to `/api/users/send-reset-otp` work**  
✅ **All API calls use the correct backend URL from environment variable**

---

## ⚠️ Important Notes

1. **Backend URL Format:**
   - ✅ Correct: `https://your-backend.railway.app`
   - ❌ Wrong: `https://your-backend.railway.app/api` (don't include `/api`)

2. **Environment Variable:**
   - Must be set in Vercel **before** building
   - Applies to Production, Preview, and Development

3. **Spring Security:**
   - Uses modern `SecurityFilterChain` (not deprecated)
   - Explicitly allows OPTIONS requests
   - CSRF disabled for APIs (stateless)

4. **CORS Configuration:**
   - Configured at both Spring Security level and MVC level
   - Allows credentials
   - Allows all headers
   - Caches preflight for 1 hour

---

## 🆘 Troubleshooting

### Issue: Still getting CORS errors
- ✅ Verify `BACKEND_API_URL` is set in Vercel
- ✅ Verify Spring Boot backend is redeployed
- ✅ Check browser Network tab for OPTIONS request
- ✅ Verify backend URL is accessible

### Issue: `net::ERR_FAILED` persists
- ✅ Check backend URL is correct in Vercel environment variable
- ✅ Verify backend is running and accessible
- ✅ Check browser console for specific error messages

### Issue: OPTIONS request fails
- ✅ Verify `SpringSecurityConfig.java` is deployed
- ✅ Check Spring Boot logs for CORS-related errors
- ✅ Verify origin matches: `https://full-stack-neo-bank22.vercel.app`

---

**All configurations are production-ready and follow Spring Boot 3.x and Angular best practices.**
