# HOD Login Deployment Issue & Resolution

## Problem Summary

**Current Status**: `405 Method Not Allowed` on HOD Login/Create endpoints  
**Root Cause**: Render backend has not redeployed with the HOD POST endpoints  
**Live Evidence**:
```
STATUS 405
{
  "supportedMethods": ["GET"],
  "message": "HTTP method POST is not supported for this endpoint.",
  "error": "METHOD_NOT_ALLOWED"
}
```

## Code Status ✅

The source code contains the required HOD endpoints:

```java
@PostMapping("/hod-create")
public ResponseEntity<?> createHodAccount(@RequestBody Admin admin) { ... }

@PostMapping("/hod-login")
public ResponseEntity<Map<String, Object>> loginHod(@RequestBody Map<String, String> credentials) { ... }

@GetMapping("/hod-availability")
public ResponseEntity<Map<String, Object>> hodAvailability() { ... }

@GetMapping("/hod-overview")
public ResponseEntity<Map<String, Object>> getHodOverview() { ... }
```

**Committed Branch**: `origin/main` commit `a11d436`  
**Push Status**: ✅ Successfully pushed `mobile-security-v2 → main`

## Render Deployment Steps

### Step 1: Verify Branch Configuration
1. Go to: https://dashboard.render.com/
2. Select the NeoBank **backend service** (Spring application)
3. Click **Settings**
4. Under **Repository**, confirm:
   - **Branch**: `main` ✅
   - **Repository**: FullStack_NeoBank ✅

### Step 2: Trigger Manual Deploy
1. From the service dashboard, click **Manual Deploy**
2. Select **Deploy latest commit**
3. Wait for deployment log to show:
   ```
   Build started...
   Building application...
   Build complete
   Starting service...
   Service started successfully
   ```

### Step 3: Verify Deployment (3-5 minutes)
Test each endpoint:

**Test 1: HOD Availability (should return 200)**
```bash
curl -X GET https://fullstack-neobank.onrender.com/api/admins/hod-availability
```
Expected:
```json
{"available": true, "repairRequired": false}
```

**Test 2: HOD Create (should return 200 or 400 with proper error)**
```bash
curl -X POST https://fullstack-neobank.onrender.com/api/admins/hod-create \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","name":"HOD","role":"HOD"}'
```
Expected: **200** (success) or **400/409** (validation error) — NOT 405

**Test 3: HOD Login (should return 200 or 401 with proper error)**
```bash
curl -X POST https://fullstack-neobank.onrender.com/api/admins/hod-login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```
Expected: **200** (success) or **401/400** (auth error) — NOT 405

## If Deployment Still Shows 405

### Option A: Check Render Logs
1. Open the Render backend service
2. Click **Logs**
3. Look for build errors in the deploy log
4. If deployment failed, the log will show the error

### Option B: Force Redeploy
1. Click **Settings** → **Repository**
2. Temporarily change branch to `mobile-security-v2`
3. Click **Manual Deploy** → **Deploy latest commit**
4. Wait for successful deployment
5. Change branch back to `main`
6. Click **Manual Deploy** → **Deploy latest commit**

### Option C: Check Java Compilation
Render builds with Maven. If the build succeeded but 405 persists, check:
- Whether Spring Boot is running the correct `AdminController` class
- Whether there are multiple conflicting route mappings

---

## Frontend Status ✅

**Angular Frontend** (Firebase Hosting):
- ✅ Deployed to `neo-bank-669.web.app`
- ✅ Calls only:
  - `POST /api/admins/hod-create`
  - `POST /api/admins/hod-login`
  - `GET /api/admins/hod-availability`
- ✅ Does NOT call `/api/admins/login` or `/api/admins/create` for HOD

**Key Files**:
- `angularapp/src/app/component/website/hod-login/hod-login.ts` — HOD login form
- `angularapp/src/app/component/admin/login/login.ts` — Admin/Manager login (separate)
- `angularapp/src/app/component/website/landing/landing.ts` — Landing page with role buttons

---

## Next Steps

1. **Go to Render dashboard**: https://dashboard.render.com/
2. **Find the NeoBank backend service** (Spring Boot)
3. **Click Manual Deploy**
4. **Wait 3-5 minutes for deployment to complete**
5. **Test HOD Create Account**: https://neo-bank-669.web.app/hod/login
6. **Try creating an HOD account with email and password**

The error message should change from `405 Method Not Allowed` to a proper application response.

---

## Verification Checklist

After Render deploys:

- [ ] `GET /api/admins/hod-availability` returns `200` (not `400` or `405`)
- [ ] `POST /api/admins/hod-create` returns `200` or `400/409` (not `405`)
- [ ] `POST /api/admins/hod-login` returns `200` or `401/400` (not `405`)
- [ ] HOD Create Account form submits successfully
- [ ] HOD account is created and persisted in the database
- [ ] HOD can login with the created credentials
- [ ] No 405 errors in browser Network tab

---

## Database State

After HOD account creation via the repair endpoint:
- Old broken record with `null` password is **updated** (not recreated)
- Password is encrypted with BCrypt
- `accountLocked = false`
- `failedLoginAttempts = 0`
- Ready for login

