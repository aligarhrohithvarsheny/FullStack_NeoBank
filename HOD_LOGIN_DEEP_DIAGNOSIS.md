# HOD Login Deep Diagnosis

## Issue Confirmation

**Error on Frontend**:
```
HTTP method POST is not supported for this endpoint.
```

**Root Cause**: The live Render backend does not have the HOD endpoints compiled and running.

---

## Code Integration Verification ✅

### Frontend → Backend Route Mapping

| Frontend Call | Backend Route | Status |
|---|---|---|
| `POST /api/admins/hod-login` | `@PostMapping("/hod-login")` in `@RequestMapping("/api/admins")` | ✅ Correct |
| `POST /api/admins/hod-create` | `@PostMapping("/hod-create")` in `@RequestMapping("/api/admins")` | ✅ Correct |
| `GET /api/admins/hod-availability` | `@GetMapping("/hod-availability")` in `@RequestMapping("/api/admins")` | ✅ Correct |

### Frontend Code (`hod-login.ts`)

**Login Form**:
```typescript
// Line 58: HOD login endpoint
this.http.post<any>(`${environment.apiBaseUrl}/api/admins/hod-login`, credentials)
  .subscribe({
    next: response => {
      if (response?.success && response?.role === 'HOD') {
        // Success: store session and navigate to HOD dashboard
      }
    },
    error: err => {
      this.errorMessage = err.error?.message 
        || 'HOD login service is unavailable. Please deploy the HOD backend service.';
    }
  });
```

**Create Account Form**:
```typescript
// Line 99: HOD account creation endpoint
this.http.post<any>(`${environment.apiBaseUrl}/api/admins/hod-create`, accountPayload)
  .subscribe({
    next: () => {
      // Success: account created, show login form
    },
    error: err => {
      this.errorMessage = err.error?.message 
        || 'HOD account service is unavailable. Please deploy the HOD backend service.';
    }
  });
```

**Availability Check (on page load)**:
```typescript
// Line 36: Check if HOD account creation is allowed
this.http.get<any>(`${environment.apiBaseUrl}/api/admins/hod-availability`)
  .subscribe({
    next: response => {
      if (response?.available === false) this.showCreateAccountButton = false;
    },
    error: () => this.showCreateAccountButton = true
  });
```

### Backend Code (`AdminController.java`)

**Controller Declaration**:
```java
// Line 35
@RequestMapping("/api/admins")
// Line 37
public class AdminController {
```

**HOD Login Endpoint**:
```java
// Line 176
@PostMapping("/hod-login")  // Full route: POST /api/admins/hod-login
public ResponseEntity<Map<String, Object>> loginHod(@RequestBody Map<String, String> credentials) {
  // Email validation
  // Role validation (must be HOD)
  // Account lock check
  // Password verification
  // Return success with admin details or 401 Unauthorized
}
```

**HOD Create Account Endpoint**:
```java
// Line 143
@PostMapping("/hod-create")  // Full route: POST /api/admins/hod-create
public ResponseEntity<?> createHodAccount(@RequestBody Admin admin) {
  // Validation: email and password required
  // Check if HOD exists
  // If exists with null password: repair (update password, unlock account)
  // If exists with valid password: return 409 Conflict
  // If new: create HOD account
}
```

**HOD Availability Endpoint**:
```java
// Line 129
@GetMapping("/hod-availability")  // Full route: GET /api/admins/hod-availability
public ResponseEntity<Map<String, Object>> hodAvailability() {
  // Check if HOD account exists
  // Return: available (true if no HOD or HOD needs password repair)
  // Return: repairRequired (true if HOD exists but password is null/empty)
}
```

---

## Why the 405 Error Occurs

The live Render backend is **still running the old version** without these endpoints.

**Current Live Response**:
```json
{
  "supportedMethods": ["GET"],
  "message": "HTTP method POST is not supported for this endpoint.",
  "error": "METHOD_NOT_ALLOWED",
  "status": 405
}
```

This means:
1. ✅ The HTTP request reached the Render server
2. ✅ The request was correctly formatted (Content-Type, JSON body)
3. ❌ The server has no `@PostMapping` handler for `/hod-login` or `/hod-create`
4. ❌ The server is serving the `main` branch BEFORE the push on 2026-09-24

---

## Deployment Status

### Local Repository
```
Commit a11d436 (HEAD -> mobile-security-v2, origin/mobile-security-v2, origin/main)
├─ Contains: @PostMapping("/hod-login")
├─ Contains: @PostMapping("/hod-create")
├─ Contains: @GetMapping("/hod-availability")
├─ Password fix: @JsonProperty(access = WRITE_ONLY)
└─ Status: ✅ Pushed to origin/main successfully
```

### Render Backend Service
```
Last Deployed: Before 2026-09-24
├─ Branch: main (correct)
├─ Contains: ❌ NO @PostMapping("/hod-login")
├─ Contains: ❌ NO @PostMapping("/hod-create")
├─ Contains: ❌ NO @GetMapping("/hod-availability")
└─ Status: ⏳ WAITING FOR MANUAL DEPLOY TRIGGER
```

---

## Required Action

### Render Dashboard Steps

**You must manually trigger deploy on Render dashboard:**

1. **Go to**: https://dashboard.render.com/
2. **Select**: NeoBank backend service (Spring Boot application)
3. **Verify**: Branch is `main` in Settings
4. **Click**: **Manual Deploy**
5. **Select**: **Deploy latest commit**
6. **Wait**: 3-5 minutes for build and startup

**Render will then**:
- Pull the latest `main` branch (commit `a11d436` with HOD endpoints)
- Run: `./mvnw clean package -DskipTests`
- Compile: All Java classes including HOD POST methods
- Start: Spring Boot server with HOD endpoints active
- Result: `POST /api/admins/hod-login` and `POST /api/admins/hod-create` will be available

### What Happens After Deploy

Once Render finishes:

**Frontend will work**:
```
https://neo-bank-669.web.app/hod/login
├─ Load page
├─ GET /api/admins/hod-availability → returns JSON (not 400/405)
├─ Show "Create HOD Account" button
├─ Submit form
└─ POST /api/admins/hod-create → returns 200 with account details
```

**Backend will respond**:
```
POST /api/admins/hod-create
Status: 200 OK (or 409 if account exists with password)
Response: {"success": true, "message": "HOD account created"}

POST /api/admins/hod-login  
Status: 200 OK (or 401 if credentials invalid)
Response: {"success": true, "role": "HOD", "admin": {...}}
```

---

## Common Issues After Deploy

| Issue | Solution |
|---|---|
| Still getting 405 | Render deploy may not have finished. Wait 5 more minutes, then refresh. |
| Build failed | Check Render logs for Java compilation errors. Most likely: missing dependency or syntax error. |
| 400 Bad Request | Frontend JSON is malformed. Check browser Network tab, look at request body. |
| 401 Unauthorized | HOD account doesn't exist or password is wrong. Create account first. |
| 409 Conflict | HOD account already exists with a password. Use existing password to login. |

---

## Verification After Deploy

Test manually in browser or terminal:

```bash
# Test 1: Check availability (should be 200, not 400)
curl -X GET https://fullstack-neobank.onrender.com/api/admins/hod-availability

# Test 2: Create account (should be 200 or 400/409, not 405)
curl -X POST https://fullstack-neobank.onrender.com/api/admins/hod-create \
  -H "Content-Type: application/json" \
  -d '{"email":"neohod@example.com","password":"password123","name":"NeoBank Head of Department","role":"HOD"}'

# Test 3: Login (should be 200 or 401, not 405)
curl -X POST https://fullstack-neobank.onrender.com/api/admins/hod-login \
  -H "Content-Type: application/json" \
  -d '{"email":"neohod@example.com","password":"password123"}'
```

All three should return **200**, **400**, or **401** — **never 405** after deploy.

