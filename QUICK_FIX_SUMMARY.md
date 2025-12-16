# ⚡ Quick Fix Summary for 502 Errors

## 🎯 The Problem
Your `vercel.json` has a placeholder backend URL: `https://YOUR_BACKEND_URL_HERE/api/$1`

## ✅ The Solution (3 Steps)

### Step 1: Get Your Backend URL

**If backend is already deployed:**
- **Railway**: Dashboard → Backend Service → Settings → Networking → Copy Public Domain
- **Render**: Dashboard → Backend Service → Copy URL from top
- **Test it**: Open `https://your-backend-url/actuator/health` in browser (should return `{"status":"UP"}`)

**If backend is NOT deployed:**
- Deploy to Railway (easiest) or Render
- See `COMPREHENSIVE_502_FIX.md` for detailed deployment steps

### Step 2: Update vercel.json

Open `vercel.json` and replace line 10:

**From:**
```json
"destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
```

**To (example for Railway):**
```json
"destination": "https://your-app.up.railway.app/api/$1"
```

**Important:**
- ✅ Use your actual backend URL
- ✅ No trailing slash
- ✅ Don't include `/api` in base URL (it's added by `$1`)

### Step 3: Commit and Deploy

```bash
git add vercel.json
git commit -m "Fix: Update backend URL in vercel.json"
git push
```

Vercel will auto-redeploy. Wait 1-2 minutes, then test your login.

---

## ✅ CORS Configuration Status

**Good News!** Your backend's `SecurityConfig.java` already includes:
```java
"https://*.vercel.app"
```

This means CORS should work automatically for all Vercel domains, including:
- `https://full-stack-neo-bank2.vercel.app`
- `https://full-stack-neo-bank2-*.vercel.app`

**Optional (Recommended):** Add specific domain to backend environment variables:
```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank2.vercel.app,https://*.vercel.app
```

---

## 🧪 Verification

After updating and redeploying:

1. **Test backend directly:**
   ```
   https://your-backend-url/actuator/health
   ```
   Should return: `{"status":"UP"}`

2. **Test through Vercel:**
   - Open: `https://full-stack-neo-bank2.vercel.app`
   - Open DevTools (F12) → Network tab
   - Try to login
   - Check API requests: Should be **200 OK** (not 502)

---

## 📚 More Help

- **Detailed Guide**: See `COMPREHENSIVE_502_FIX.md`
- **Backend Deployment**: See `DEPLOYMENT_GUIDE.md`
- **Backend Health Check**: Run `node check-backend.js <your-backend-url>`

---

## 🔍 Still Getting 502?

1. ✅ Verify backend is running (test `/actuator/health`)
2. ✅ Check `vercel.json` URL is correct (no typos)
3. ✅ Ensure backend URL has no trailing slash
4. ✅ Check Vercel deployment logs
5. ✅ Check backend logs (Railway/Render dashboard)

---

**Need to deploy backend?** See `COMPREHENSIVE_502_FIX.md` → Step 2
