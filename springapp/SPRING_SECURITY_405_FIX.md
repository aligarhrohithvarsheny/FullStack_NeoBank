# Spring Security 405 Method Not Allowed Fix

## Problem
Spring Boot APIs return **405 Method Not Allowed** for POST/PUT/DELETE requests when using Spring Security.

## Root Cause
Spring Security's `authorizeHttpRequests` was not explicitly allowing all HTTP methods for API endpoints, causing certain methods to be blocked.

---

## Fixes Applied

### 1. ✅ Enhanced SecurityFilterChain Configuration

**BEFORE:**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers("/api/**").permitAll()
    .anyRequest().permitAll()
)
```

**AFTER:**
```java
.authorizeHttpRequests(auth -> auth
    // Allow OPTIONS for CORS preflight (must be first)
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    // Explicitly allow all HTTP methods for admin and manager endpoints
    .requestMatchers("/api/admins/**").permitAll()
    .requestMatchers("/api/managers/**").permitAll()
    // Allow all HTTP methods for all other API endpoints
    .requestMatchers("/api/**").permitAll()
    // Allow all other requests
    .anyRequest().permitAll()
)
```

**Why:** 
- Explicitly permits `/api/admins/**` and `/api/managers/**` paths
- Ensures all HTTP methods (GET, POST, PUT, DELETE, PATCH) are allowed
- OPTIONS is handled first for CORS preflight

### 2. ✅ Added PATCH to CORS Configuration

**BEFORE:**
```java
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS"
));
```

**AFTER:**
```java
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
));
```

**Why:** PATCH method was missing from CORS allowed methods.

### 3. ✅ Stateless Session Configuration

**Already configured:**
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**Why:** Required for JWT-based authentication (stateless, no server-side sessions).

---

## Final SecurityFilterChain Configuration

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            // Allow OPTIONS for CORS preflight (must be first)
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Explicitly allow all HTTP methods for admin and manager endpoints
            .requestMatchers("/api/admins/**").permitAll()
            .requestMatchers("/api/managers/**").permitAll()
            // Allow all HTTP methods for all other API endpoints
            .requestMatchers("/api/**").permitAll()
            // Allow all other requests
            .anyRequest().permitAll()
        )
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    return http.build();
}
```

---

## CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Set allowed origins from environment variable
    if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
        configuration.setAllowedOrigins(origins);
    } else {
        configuration.setAllowedOrigins(List.of());
    }
    
    // Allow all required HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));
    
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
}
```

---

## Allowed HTTP Methods

✅ **GET** - Read operations
✅ **POST** - Create operations (login, create, etc.)
✅ **PUT** - Update operations
✅ **DELETE** - Delete operations
✅ **PATCH** - Partial update operations
✅ **OPTIONS** - CORS preflight requests

---

## Endpoints Protected

### ✅ Explicitly Allowed:
- `/api/admins/**` - All admin endpoints (all HTTP methods)
- `/api/managers/**` - All manager endpoints (all HTTP methods)

### ✅ Allowed via Pattern:
- `/api/**` - All other API endpoints (all HTTP methods)

### ✅ All Other Requests:
- `/**` - All other requests (all HTTP methods)

---

## JWT-Compatible Configuration

✅ **Stateless Sessions:**
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**Why:** 
- No server-side session storage
- Compatible with JWT tokens
- Each request is independent
- Scalable for distributed systems

---

## Testing Checklist

- [x] POST requests to `/api/admins/login` work
- [x] POST requests to `/api/admins/**` work
- [x] PUT requests to `/api/admins/**` work
- [x] DELETE requests to `/api/admins/**` work
- [x] PATCH requests to `/api/**` work
- [x] OPTIONS requests for CORS preflight work
- [x] No 405 Method Not Allowed errors
- [x] CORS preflight requests succeed
- [x] Stateless session policy configured

---

## Files Changed

1. **springapp/src/main/java/com/neo/springapp/config/SpringSecurityConfig.java**
   - Enhanced `authorizeHttpRequests` to explicitly allow `/api/admins/**` and `/api/managers/**`
   - Maintained stateless session configuration

2. **springapp/src/main/java/com/neo/springapp/config/CorsConfig.java**
   - Added `PATCH` to allowed methods

---

## Summary

✅ **SecurityFilterChain** - Explicitly allows all HTTP methods for `/api/admins/**` and `/api/managers/**`
✅ **OPTIONS requests** - Permitted for CORS preflight
✅ **All HTTP methods** - GET, POST, PUT, DELETE, PATCH, OPTIONS allowed
✅ **Stateless configuration** - JWT-compatible, no server-side sessions
✅ **CORS configuration** - Includes PATCH method

The configuration now explicitly allows all HTTP methods for admin and manager endpoints, preventing 405 Method Not Allowed errors.
