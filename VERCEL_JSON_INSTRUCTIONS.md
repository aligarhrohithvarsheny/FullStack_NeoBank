# 📝 vercel.json Configuration Instructions

## ⚠️ IMPORTANT: Update Backend URL

Your `vercel.json` file currently has a placeholder that **MUST** be replaced:

### Current Configuration (Line 10):
```json
"destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
```

### What You Need to Do:

1. **Get your backend URL** from Railway or Render dashboard
2. **Replace** `YOUR_BACKEND_URL_HERE` with your actual backend URL
3. **Do NOT** include `/api` in the base URL (it's added automatically)
4. **Do NOT** add a trailing slash

### Examples:

**If using Railway:**
```json
"destination": "https://neobank-production.up.railway.app/api/$1"
```

**If using Render:**
```json
"destination": "https://neobank-backend.onrender.com/api/$1"
```

### After Updating:

1. Save the file
2. Commit: `git add vercel.json && git commit -m "Fix: Update backend URL"`
3. Push: `git push`
4. Vercel will auto-redeploy

---

## How to Find Your Backend URL

### Railway:
1. Go to [railway.app](https://railway.app) → Your Project
2. Click on your backend service
3. Go to **Settings** → **Networking**
4. Copy the **Public Domain** URL

### Render:
1. Go to [render.com](https://render.com) → Your Dashboard
2. Click on your backend service
3. Copy the URL shown at the top of the service page

### Test Your Backend:
After getting the URL, test it:
```
https://your-backend-url/actuator/health
```

Should return: `{"status":"UP"}`

---

## Still Need Help?

See `COMPREHENSIVE_502_FIX.md` for detailed troubleshooting.
