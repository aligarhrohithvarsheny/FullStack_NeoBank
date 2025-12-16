# 🔧 Comprehensive 502 Error Fix Guide

## 📋 Table of Contents
1. [Understanding the Error](#understanding-the-error)
2. [Step-by-Step Fix](#step-by-step-fix)
3. [Backend Deployment Guide](#backend-deployment-guide)
4. [CORS Configuration](#cors-configuration)
5. [Verification Steps](#verification-steps)
6. [Troubleshooting](#troubleshooting)

---

## Understanding the Error

### What is a 502 Error?
A **502 Bad Gateway** error means that Vercel (acting as a proxy) cannot reach your backend server. This happens when:

1. **Backend URL is incorrect** - The placeholder `YOUR_BACKEND_URL_HERE` in `vercel.json` doesn't point to a real server
2. **Backend is not deployed** - Your Spring Boot backend hasn't been deployed yet
3. **Backend is down** - The backend server is not running
4. **CORS issues** - Backend is blocking requests from Vercel domain

### Your Current Error
```
Failed to load resource: the server responded with a status of 502
/api/users/authenticate:1
/api/admins/login:1
/api/webauthn/credentials/rohithsai186@gmail.com:1
```

This confirms that:
- ✅ Frontend is working (Vercel deployment is successful)
- ❌ Backend proxy is failing (vercel.json has placeholder URL)
- ❌ API requests cannot reach the backend

---

## Step-by-Step Fix

### Step 1: Check if Backend is Deployed

#### Option A: Check Railway Dashboard
1. Go to [railway.app](https://railway.app) and login
2. Open your project
3. Find your backend service
4. Check the **Settings** tab → **Networking** section
5. Copy the **Public Domain** URL (e.g., `https://your-app.up.railway.app`)

#### Option B: Check Render Dashboard
1. Go to [render.com](https://render.com) and login
2. Open your backend service
3. Check the service URL at the top (e.g., `https://your-app.onrender.com`)

#### Option C: Test Backend Manually
1. If you have a backend URL, test it in your browser:
   ```
   https://your-backend-url/actuator/health
   ```
2. If it returns JSON with `"status":"UP"`, your backend is running ✅
3. If it doesn't load, your backend is not deployed or not accessible ❌

#### Option D: Use the Check Script
Run the provided script to test your backend:
```bash
node check-backend.js https://your-backend-url.com
```

---

### Step 2: Deploy Backend (If Not Deployed)

If you don't have a backend deployed yet, choose one:

#### 🚂 Deploy to Railway (Recommended - Easiest)

1. **Sign up/Login**: Go to [railway.app](https://railway.app)

2. **Create Project**:
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository

3. **Add MySQL Database**:
   - Click "New" → "Database" → "MySQL"
   - Railway will auto-generate connection variables

4. **Deploy Backend Service**:
   - Click "New" → "GitHub Repo" → Select your repo
   - In service settings:
     - **Root Directory**: `springapp`
     - **Build Command**: `mvn clean package -DskipTests`
     - **Start Command**: `java -jar target/*.jar`
     - **Port**: `8080` (auto-detected)

5. **Configure Environment Variables**:
   In Railway backend service → Variables tab, add:
   ```env
   SPRING_PROFILES_ACTIVE=production
   SPRING_DATASOURCE_URL=${{MySQL.DATABASE_URL}}
   SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
   SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   SPRING_JPA_SHOW_SQL=false
   
   # CORS - Add your Vercel domain
   SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank2.vercel.app,https://full-stack-neo-bank2-*.vercel.app,https://*.vercel.app
   
   # Email Configuration (use your existing settings)
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=sairohith669@gmail.com
   SPRING_MAIL_PASSWORD=lgmrduvlopknzvge
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
   ```

6. **Get Backend URL**:
   - Wait for deployment to complete
   - Go to Settings → Networking
   - Copy the **Public Domain** URL
   - Example: `https://neobank-production.up.railway.app`

#### 🎨 Deploy to Render

1. **Sign up/Login**: Go to [render.com](https://render.com)

2. **Create Database**:
   - Click "New +" → "PostgreSQL" (or MySQL if available)
   - Name: `neobank-db`
   - Note the connection details

3. **Deploy Backend**:
   - Click "New +" → "Web Service"
   - Connect GitHub repo
   - Configure:
     - **Name**: `neobank-backend`
     - **Root Directory**: `springapp`
     - **Environment**: `Java`
     - **Build Command**: `mvn clean package -DskipTests`
     - **Start Command**: `java -jar target/*.jar`

4. **Add Environment Variables** (similar to Railway)

5. **Get Backend URL**: Copy from the service dashboard

---

### Step 3: Update vercel.json

1. **Open** `vercel.json` in your project root

2. **Find** this line:
   ```json
   "destination": "https://YOUR_BACKEND_URL_HERE/api/$1"
   ```

3. **Replace** `YOUR_BACKEND_URL_HERE` with your actual backend URL (without `/api`)

   **Example for Railway:**
   ```json
   "destination": "https://neobank-production.up.railway.app/api/$1"
   ```

   **Example for Render:**
   ```json
   "destination": "https://neobank-backend.onrender.com/api/$1"
   ```

4. **Important**: 
   - ✅ Do NOT include `/api` in the base URL (it's added by `$1`)
   - ✅ Do NOT add trailing slash
   - ✅ Use HTTPS (not HTTP)

5. **Save** the file

---

### Step 4: Configure CORS on Backend

Your backend's `SecurityConfig.java` already includes wildcard patterns for Vercel:
```java
"https://*.vercel.app"
```

However, for better security, you should also add your specific Vercel domain.

#### In Railway/Render Backend Environment Variables:

Add or update:
```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank2.vercel.app,https://full-stack-neo-bank2-*.vercel.app,https://*.vercel.app
```

**Note**: The wildcard `https://*.vercel.app` should already work, but adding the specific domain is recommended.

---

### Step 5: Commit and Redeploy

1. **Commit your changes**:
   ```bash
   git add vercel.json
   git commit -m "Fix: Update backend URL in vercel.json"
   git push
   ```

2. **Vercel will auto-redeploy**, or manually trigger:
   - Go to Vercel Dashboard → Your Project → Deployments
   - Click "Redeploy"

3. **Wait 1-2 minutes** for deployment to complete

---

## Verification Steps

### 1. Test Backend Directly
Open in browser:
```
https://your-backend-url/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 2. Test API Endpoint
Open in browser:
```
https://your-backend-url/api/users/test
```

Should return a response (even if it's an error, it means backend is reachable).

### 3. Test Through Vercel Proxy
1. Open your Vercel app: `https://full-stack-neo-bank2.vercel.app`
2. Open DevTools (F12) → Network tab
3. Try to login
4. Check API requests:
   - ✅ Should return **200 OK** (not 502)
   - ✅ Response should contain data (not error)

### 4. Check Vercel Function Logs
1. Go to Vercel Dashboard → Your Project → Functions
2. Check for any errors in the proxy function

---

## Troubleshooting

### ❌ Still Getting 502 Errors?

#### 1. Backend Not Accessible
**Symptoms**: Can't access `https://your-backend-url/actuator/health`

**Solutions**:
- Check Railway/Render logs for backend errors
- Verify backend service is running (not stopped)
- Check if backend crashed during startup
- Verify database connection is working

#### 2. Wrong URL Format in vercel.json
**Symptoms**: 502 errors persist after updating

**Check**:
- ✅ URL has no trailing slash: `https://app.railway.app` (not `https://app.railway.app/`)
- ✅ URL doesn't include `/api`: `https://app.railway.app/api/$1` (not `https://app.railway.app/api/api/$1`)
- ✅ Using HTTPS (not HTTP)
- ✅ URL is correct (copy-paste from Railway/Render dashboard)

#### 3. CORS Errors
**Symptoms**: 502 or CORS errors in browser console

**Solutions**:
- Verify `SPRING_WEB_CORS_ALLOWED_ORIGINS` includes your Vercel domain
- Check backend logs for CORS-related errors
- Ensure backend allows credentials: `allowCredentials(true)` (already configured)

#### 4. Backend Timeout
**Symptoms**: 502 errors after a few seconds

**Solutions**:
- Check backend logs for slow queries
- Verify database connection is stable
- Check if backend is under heavy load
- Consider increasing timeout in Vercel (if using serverless functions)

#### 5. Database Connection Issues
**Symptoms**: Backend starts but API calls fail

**Solutions**:
- Verify database credentials in Railway/Render
- Check database is running and accessible
- Verify connection string format
- Check database logs for connection errors

---

## Quick Reference

### Backend URL Formats
- **Railway**: `https://your-app.up.railway.app` or `https://your-app.railway.app`
- **Render**: `https://your-app.onrender.com`
- **Heroku**: `https://your-app.herokuapp.com`

### vercel.json Format
```json
{
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://YOUR_BACKEND_URL/api/$1"
    }
  ]
}
```

### CORS Environment Variable
```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank2.vercel.app,https://*.vercel.app
```

---

## Next Steps After Fix

1. ✅ Test login functionality
2. ✅ Test all API endpoints
3. ✅ Monitor Vercel and backend logs
4. ✅ Set up error monitoring (Sentry, etc.)
5. ✅ Configure custom domain (optional)

---

## Need More Help?

- **Railway Docs**: https://docs.railway.app
- **Render Docs**: https://render.com/docs
- **Vercel Docs**: https://vercel.com/docs
- **Spring Boot CORS**: https://spring.io/guides/gs/rest-service-cors/

---

**Last Updated**: Based on error from `full-stack-neo-bank2.vercel.app`
