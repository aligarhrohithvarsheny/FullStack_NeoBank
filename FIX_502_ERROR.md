# 🔧 Fix 502 Error on Vercel Deployment

## Problem
You're seeing **502 Bad Gateway** errors because your `vercel.json` file has a placeholder backend URL that hasn't been replaced with your actual backend URL.

## Root Cause
The `vercel.json` file contains:
```json
"destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
```

This placeholder URL doesn't exist, so Vercel can't proxy your API requests to the backend, resulting in 502 errors.

## Solution

### Step 1: Deploy Your Backend (If Not Already Deployed)

You need to deploy your Spring Boot backend to a hosting platform. Recommended options:

#### Option A: Railway (Easiest)
1. Go to [railway.app](https://railway.app) and sign up
2. Create a new project from your GitHub repo
3. Add a MySQL database
4. Deploy the backend service:
   - Root Directory: `springapp`
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/*.jar`
5. Copy your backend URL (e.g., `https://your-app.up.railway.app`)

#### Option B: Render
1. Go to [render.com](https://render.com)
2. Create a new Web Service from your GitHub repo
3. Configure similar to Railway
4. Copy your backend URL

### Step 2: Update vercel.json

1. Open `vercel.json` in your project root
2. Find this line:
   ```json
   "destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
   ```
3. Replace `YOUR_BACKEND_URL_HERE` with your actual backend URL (without `/api` at the end)
   
   **Example:**
   ```json
   "destination": "https://your-app.up.railway.app/api/$1"
   ```
   
   Or if using Render:
   ```json
   "destination": "https://your-app.onrender.com/api/$1"
   ```

### Step 3: Configure CORS on Backend

Make sure your backend allows requests from your Vercel domain:

1. In your backend environment variables, add:
   ```
   SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank.vercel.app,https://full-stack-neo-bank-*.vercel.app
   ```

2. Or if you have a custom domain:
   ```
   SPRING_WEB_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://full-stack-neo-bank.vercel.app
   ```

### Step 4: Redeploy on Vercel

1. Commit your updated `vercel.json`:
   ```bash
   git add vercel.json
   git commit -m "Update backend URL in vercel.json"
   git push
   ```

2. Vercel will automatically redeploy, or you can trigger a redeploy from the Vercel dashboard

### Step 5: Verify

1. Open your Vercel app: `https://full-stack-neo-bank.vercel.app`
2. Open browser DevTools (F12) → Network tab
3. Try to login
4. Check that API requests to `/api/*` are now successful (status 200) instead of 502

## Quick Checklist

- [ ] Backend is deployed and accessible (test in browser: `https://your-backend-url/actuator/health`)
- [ ] `vercel.json` has the correct backend URL (not the placeholder)
- [ ] Backend CORS is configured to allow your Vercel domain
- [ ] Changes are committed and pushed
- [ ] Vercel has redeployed with the new configuration

## Still Getting 502 Errors?

1. **Verify backend is running**: Visit `https://your-backend-url/actuator/health` in your browser
2. **Check backend logs**: Look at Railway/Render logs for errors
3. **Verify URL format**: Make sure there's no trailing slash in the backend URL in `vercel.json`
4. **Check CORS**: Ensure backend allows your Vercel domain
5. **Wait a few minutes**: Sometimes DNS/proxy changes take a moment to propagate

## Example vercel.json (After Fix)

```json
{
  "version": 2,
  "buildCommand": "cd angularapp && npm install && npm run build:prod && cp dist/angularapp/browser/index.csr.html dist/angularapp/browser/index.html",
  "outputDirectory": "angularapp/dist/angularapp/browser",
  "installCommand": "cd angularapp && npm install",
  "framework": null,
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://your-backend.up.railway.app/api/$1"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ],
  ...
}
```

---

**Need help?** Check your backend deployment platform's documentation or logs for more details.

