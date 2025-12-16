# 🔍 502 Error Diagnosis & Fix Guide

## Current Error Analysis

You're experiencing **502 Bad Gateway** errors on these endpoints:
- `/api/users/authenticate` 
- `/api/admins/login`
- `/api/webauthn/credentials/rohithsai186@gmail.com`

### Root Cause

Your `vercel.json` file contains a **placeholder backend URL**:
```json
"destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
```

When Vercel tries to proxy API requests to this non-existent URL, it returns a 502 error.

---

## Solution Steps

### Step 1: Check if Backend is Deployed

**Option A: If you already have a backend deployed**

1. Check your Railway/Render dashboard for the backend URL
2. Test if it's accessible: Open `https://your-backend-url/actuator/health` in your browser
3. If it works, proceed to Step 2

**Option B: If backend is NOT deployed yet**

You need to deploy your Spring Boot backend first. Choose one:

#### Deploy to Railway (Easiest)
1. Go to [railway.app](https://railway.app) and sign up/login
2. Create new project → Deploy from GitHub repo
3. Add MySQL database service
4. Add backend service:
   - Root Directory: `springapp`
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/*.jar`
5. Copy your backend URL (e.g., `https://your-app.up.railway.app`)

#### Deploy to Render
1. Go to [render.com](https://render.com) and sign up
2. Create new Web Service from GitHub repo
3. Configure similar to Railway
4. Copy your backend URL

---

### Step 2: Update vercel.json

1. Open `vercel.json` in your project root
2. Find this line:
   ```json
   "destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
   ```
3. Replace `YOUR_BACKEND_URL_HERE` with your actual backend URL (without `/api` at the end)

   **Example for Railway:**
   ```json
   "destination": "https://your-app.up.railway.app/api/$1"
   ```

   **Example for Render:**
   ```json
   "destination": "https://your-app.onrender.com/api/$1"
   ```

4. Save the file

---

### Step 3: Configure CORS on Backend

Your backend must allow requests from your Vercel domain.

**In your backend environment variables (Railway/Render dashboard), add:**

```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank2.vercel.app,https://full-stack-neo-bank2-*.vercel.app,https://*.vercel.app
```

**Or if you have a custom domain:**
```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://full-stack-neo-bank2.vercel.app,https://*.vercel.app
```

---

### Step 4: Commit and Redeploy

1. Commit your changes:
   ```bash
   git add vercel.json
   git commit -m "Fix: Update backend URL in vercel.json"
   git push
   ```

2. Vercel will automatically redeploy, or trigger a manual redeploy from Vercel dashboard

3. Wait 1-2 minutes for deployment to complete

---

### Step 5: Verify the Fix

1. Open your Vercel app: `https://full-stack-neo-bank2.vercel.app`
2. Open browser DevTools (F12) → Network tab
3. Try to login again
4. Check that API requests now return **200 OK** instead of **502**

---

## Quick Checklist

Before redeploying, verify:

- [ ] Backend is deployed and accessible (test: `https://your-backend-url/actuator/health`)
- [ ] `vercel.json` has the correct backend URL (not the placeholder)
- [ ] Backend CORS is configured to allow your Vercel domain
- [ ] Changes are committed and pushed
- [ ] Vercel has redeployed with the new configuration

---

## Still Getting 502 Errors?

### 1. Verify Backend is Running
- Visit `https://your-backend-url/actuator/health` in your browser
- Should return JSON with status "UP"
- If it doesn't work, your backend is down or not accessible

### 2. Check Backend Logs
- Railway: Go to your backend service → Logs tab
- Render: Go to your backend service → Logs tab
- Look for errors, connection issues, or startup failures

### 3. Verify URL Format
- Make sure there's **NO trailing slash** in the backend URL in `vercel.json`
- Correct: `https://your-app.railway.app/api/$1`
- Wrong: `https://your-app.railway.app//api/$1`

### 4. Check CORS Configuration
- Ensure backend allows your Vercel domain
- Check backend logs for CORS-related errors
- Verify `SPRING_WEB_CORS_ALLOWED_ORIGINS` environment variable

### 5. Wait for Propagation
- DNS/proxy changes can take 1-5 minutes to propagate
- Clear browser cache and try again
- Try in incognito mode

### 6. Check Vercel Function Logs
- Go to Vercel Dashboard → Your Project → Functions tab
- Check for any errors in the proxy function

---

## Common Backend URLs Format

- **Railway**: `https://your-app.up.railway.app` or `https://your-app.railway.app`
- **Render**: `https://your-app.onrender.com`
- **Heroku**: `https://your-app.herokuapp.com`
- **Custom Domain**: `https://api.yourdomain.com`

---

## Need More Help?

1. **Backend not starting?** Check Railway/Render logs for Java errors, database connection issues
2. **Database connection errors?** Verify database credentials and connection string
3. **CORS still failing?** Check that your backend SecurityConfig allows your Vercel domain
4. **Build failures?** Check Vercel build logs for npm/Node.js errors

---

**Last Updated:** Based on your current error logs from `full-stack-neo-bank2.vercel.app`
