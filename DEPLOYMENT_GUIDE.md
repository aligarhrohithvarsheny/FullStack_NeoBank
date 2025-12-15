# 🚀 NeoBank Live Deployment Guide

This guide provides step-by-step instructions to deploy your NeoBank application to production.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Deployment Options](#deployment-options)
3. [Option 1: Railway (Recommended)](#option-1-railway-recommended)
4. [Option 2: Render](#option-2-render)
5. [Option 3: Vercel (Frontend) + Railway/Render (Backend)](#option-3-vercel-frontend--railwayrender-backend)
6. [Post-Deployment Checklist](#post-deployment-checklist)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- GitHub account (for version control)
- Account on your chosen deployment platform
- Domain name (optional, but recommended)
- Environment variables ready (see `env.example`)

---

## Deployment Options

### Quick Comparison

| Platform | Best For | Database | Cost | Difficulty |
|----------|----------|----------|------|------------|
| **Railway** | Full-stack apps | ✅ Built-in MySQL | Free tier available | ⭐ Easy |
| **Render** | Full-stack apps | ✅ Built-in PostgreSQL | Free tier available | ⭐ Easy |
| **Vercel + Railway** | Separate frontend/backend | Requires separate DB | Free tier available | ⭐⭐ Medium |

---

## Option 1: Railway (Recommended)

Railway is the easiest option for full-stack deployments with built-in database support.

### Step 1: Prepare Your Repository

1. Push your code to GitHub:
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/neobank.git
git push -u origin main
```

### Step 2: Deploy Backend on Railway

1. Go to [railway.app](https://railway.app) and sign up/login
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your repository
4. Click **"Add Service"** → **"Database"** → **"MySQL"**
5. Click **"Add Service"** → **"GitHub Repo"** → Select your repo
6. In the backend service settings:
   - **Root Directory**: `springapp`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/*.jar`
   - **Port**: `8080`

### Step 3: Configure Environment Variables

In Railway backend service, add these environment variables:

```env
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=${{MySQL.DATABASE_URL}}
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false

# Email Configuration
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# WhatsApp Configuration
WHATSAPP_ENABLED=true

# CORS - Update with your frontend URL
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app,https://yourdomain.com
```

**Note**: Railway automatically provides MySQL connection variables. Use `${{MySQL.DATABASE_URL}}` format.

### Step 4: Deploy Frontend on Railway

1. In the same Railway project, click **"Add Service"** → **"GitHub Repo"**
2. Select your repository again
3. In frontend service settings:
   - **Root Directory**: `angularapp`
   - **Build Command**: `npm install && npm run build:prod`
   - **Start Command**: `npx serve -s dist/angularapp/browser -l 3000`
   - **Port**: `3000`

### Step 5: Configure Frontend Environment

1. Get your backend URL from Railway (e.g., `https://backend-production.up.railway.app`)
2. In frontend service, add environment variable:
   ```env
   BACKEND_API_URL=https://your-backend-url.railway.app/api
   ```

### Step 6: Update CORS in Backend

Update `SecurityConfig.java` to include your Railway frontend URL, or use environment variable (see production config).

### Step 7: Deploy

1. Railway will automatically deploy on every push to main branch
2. Check deployment logs in Railway dashboard
3. Your app will be live at the provided Railway URLs

---

## Option 2: Render

### Step 1: Deploy Database

1. Go to [render.com](https://render.com) and sign up
2. Click **"New +"** → **"PostgreSQL"** (or MySQL if available)
3. Configure:
   - **Name**: `neobank-db`
   - **Database**: `springapp`
   - **User**: `neo`
   - **Region**: Choose closest to you
4. Note the **Internal Database URL**

### Step 2: Deploy Backend

1. Click **"New +"** → **"Web Service"**
2. Connect your GitHub repository
3. Configure:
   - **Name**: `neobank-backend`
   - **Root Directory**: `springapp`
   - **Environment**: `Java`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/*.jar`
   - **Plan**: Free or Starter

4. Add Environment Variables:
```env
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=${DATABASE_URL}
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
```

### Step 3: Deploy Frontend

1. Click **"New +"** → **"Static Site"**
2. Connect your GitHub repository
3. Configure:
   - **Root Directory**: `angularapp`
   - **Build Command**: `npm install && npm run build:prod`
   - **Publish Directory**: `dist/angularapp/browser`

4. Add Environment Variable:
```env
BACKEND_API_URL=https://your-backend.onrender.com/api
```

---

## Option 3: Vercel (Frontend) + Railway/Render (Backend)

This option separates frontend and backend deployments.

### Step 1: Deploy Backend

Follow **Option 1** (Railway) or **Option 2** (Render) for backend deployment.

### Step 2: Deploy Frontend on Vercel

1. Go to [vercel.com](https://vercel.com) and sign up
2. Click **"Add New Project"**
3. Import your GitHub repository
4. Configure:
   - **Framework Preset**: Other
   - **Root Directory**: `angularapp`
   - **Build Command**: `npm install && npm run build:prod`
   - **Output Directory**: `dist/angularapp/browser`

5. Add Environment Variable:
```env
BACKEND_API_URL=https://your-backend-url.railway.app/api
```

6. Update `vercel.json` (already exists) with your backend URL:
```json
{
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://your-backend-url.railway.app/api/$1"
    }
  ]
}
```

### Step 3: Update CORS

Update backend CORS to allow your Vercel domain:
```env
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app,https://your-app-git-main.vercel.app
```

---

## Post-Deployment Checklist

- [ ] Backend is accessible and health check passes
- [ ] Frontend loads correctly
- [ ] Database connection is working
- [ ] CORS is configured correctly
- [ ] Environment variables are set
- [ ] Email service is configured (if using)
- [ ] WhatsApp service is configured (if using)
- [ ] SSL/HTTPS is enabled
- [ ] Custom domain is configured (optional)
- [ ] Monitoring is set up
- [ ] Backups are configured

---

## Troubleshooting

### Backend won't start

1. Check logs in deployment platform
2. Verify database connection string
3. Ensure all environment variables are set
4. Check Java version (should be 21)

### CORS errors

1. Update `SecurityConfig.java` with your frontend URL
2. Or set `SPRING_WEB_CORS_ALLOWED_ORIGINS` environment variable
3. Ensure credentials are allowed if using cookies

### Database connection issues

1. Verify database URL format
2. Check database credentials
3. Ensure database is accessible from backend service
4. For Railway: Use `${{MySQL.DATABASE_URL}}` format

### Frontend can't reach backend

1. Verify `BACKEND_API_URL` environment variable
2. Check `vercel.json` proxy configuration (if using Vercel)
3. Ensure backend is publicly accessible
4. Check network/firewall settings

### Build failures

1. Check Node.js version (should be 20+)
2. Check Java version (should be 21)
3. Verify all dependencies are in `package.json` and `pom.xml`
4. Check build logs for specific errors

---

## Environment Variables Reference

See `env.example` for all available environment variables.

### Required for Production:

- Database connection variables
- CORS allowed origins
- Email configuration (if using email)
- JWT secret (if using authentication)
- Encryption keys (if using encryption)

---

## Support

For issues specific to:
- **Railway**: Check [Railway Docs](https://docs.railway.app)
- **Render**: Check [Render Docs](https://render.com/docs)
- **Vercel**: Check [Vercel Docs](https://vercel.com/docs)

---

## Next Steps

1. Set up custom domain
2. Configure SSL certificates
3. Set up monitoring (e.g., Sentry, LogRocket)
4. Configure automated backups
5. Set up CI/CD pipeline
6. Configure staging environment

---

**Happy Deploying! 🎉**

