# 🚀 Quick Deployment Guide

## Fastest Way: Railway (Recommended)

### Step 1: Push to GitHub
```bash
git add .
git commit -m "Ready for deployment"
git push origin main
```

### Step 2: Deploy on Railway

1. Go to [railway.app](https://railway.app) → Sign up/Login
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your repository

### Step 3: Add MySQL Database

1. In Railway project, click **"New"** → **"Database"** → **"MySQL"**
2. Railway will automatically create connection variables

### Step 4: Configure Backend Service

1. Click **"New"** → **"GitHub Repo"** → Select your repo
2. In service settings:
   - **Root Directory**: `springapp`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/*.jar`

3. Add Environment Variables:
   ```
   SPRING_PROFILES_ACTIVE=production
   SPRING_DATASOURCE_URL=${{MySQL.DATABASE_URL}}
   SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
   SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   SPRING_JPA_SHOW_SQL=false
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_USERNAME=your-email@gmail.com
   SPRING_MAIL_PASSWORD=your-app-password
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
   WHATSAPP_ENABLED=true
   ```

4. **Important**: After backend deploys, copy the backend URL (e.g., `https://backend-production.up.railway.app`)

5. Add CORS environment variable:
   ```
   SPRING_WEB_CORS_ALLOWED_ORIGINS=https://your-frontend-url.railway.app
   ```
   (Update this after frontend is deployed)

### Step 5: Configure Frontend Service

1. In same Railway project, click **"New"** → **"GitHub Repo"** → Select your repo again
2. In service settings:
   - **Root Directory**: `angularapp`
   - **Build Command**: `npm install && npm run build:prod`
   - **Start Command**: `npx serve -s dist/angularapp/browser -l 3000`

3. Add Environment Variable:
   ```
   BACKEND_API_URL=https://your-backend-url.railway.app/api
   ```
   (Use the backend URL from Step 4)

4. Copy the frontend URL (e.g., `https://frontend-production.up.railway.app`)

### Step 6: Update CORS

1. Go back to backend service
2. Update `SPRING_WEB_CORS_ALLOWED_ORIGINS` with your frontend URL:
   ```
   SPRING_WEB_CORS_ALLOWED_ORIGINS=https://your-frontend-url.railway.app
   ```
3. Redeploy backend (Railway auto-redeploys on env var changes)

### Step 7: Test

1. Visit your frontend URL
2. Test login/registration
3. Check browser console for errors
4. Check Railway logs if issues occur

---

## Alternative: Render

### Step 1: Deploy Database
1. Go to [render.com](https://render.com) → Sign up
2. **New +** → **PostgreSQL** (or MySQL if available)
3. Name: `neobank-db`
4. Copy connection details

### Step 2: Deploy Backend
1. **New +** → **Web Service**
2. Connect GitHub repo
3. Settings:
   - **Root Directory**: `springapp`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/*.jar`
4. Add environment variables (see Railway Step 4)
5. Copy backend URL

### Step 3: Deploy Frontend
1. **New +** → **Static Site**
2. Connect GitHub repo
3. Settings:
   - **Root Directory**: `angularapp`
   - **Build Command**: `npm install && npm run build:prod`
   - **Publish Directory**: `dist/angularapp/browser`
4. Add `BACKEND_API_URL` environment variable
5. Copy frontend URL

### Step 4: Update CORS
Update backend `SPRING_WEB_CORS_ALLOWED_ORIGINS` with frontend URL

---

## Troubleshooting

### Backend won't start
- Check Railway/Render logs
- Verify database connection string
- Ensure Java 21 is available

### CORS errors
- Update `SPRING_WEB_CORS_ALLOWED_ORIGINS` in backend
- Include both `https://` and `http://` if needed
- Check browser console for specific error

### Frontend can't reach backend
- Verify `BACKEND_API_URL` is set correctly
- Check backend is publicly accessible
- Test backend URL directly in browser: `https://your-backend-url/actuator/health`

### Database connection fails
- Verify connection string format
- Check database credentials
- Ensure database is running

---

## Environment Variables Checklist

### Backend Required:
- ✅ `SPRING_PROFILES_ACTIVE=production`
- ✅ Database connection variables
- ✅ `SPRING_WEB_CORS_ALLOWED_ORIGINS` (your frontend URL)

### Frontend Required:
- ✅ `BACKEND_API_URL` (your backend URL + `/api`)

### Optional but Recommended:
- Email configuration
- WhatsApp configuration
- JWT secrets (if using authentication)

---

## Next Steps After Deployment

1. ✅ Test all features
2. ✅ Set up custom domain (optional)
3. ✅ Configure SSL (usually automatic)
4. ✅ Set up monitoring
5. ✅ Configure backups

---

**Your app should now be live! 🎉**

Visit your frontend URL to see it in action.


