# Render Database Setup Guide for Spring Boot

## 1. WHY MySQL Workbench (localhost) CANNOT WORK WITH RENDER

**The Problem:**
- Your MySQL Workbench runs on `localhost:3306` (your local machine)
- Render's servers are in the cloud (different network)
- When your Spring Boot app on Render tries to connect to `localhost`, it looks for MySQL on Render's server, NOT your computer
- This causes: `java.net.UnknownHostException: <host>: Name does not resolve`

**The Solution:**
You MUST use a cloud-hosted MySQL database that Render can access over the internet.

---

## 2. CLOUD DATABASE OPTIONS THAT WORK WITH RENDER

### Option A: Railway MySQL (Recommended - Easiest)
- **Cost:** Free tier available, then ~$5/month
- **Setup Time:** 5 minutes
- **URL:** https://railway.app
- **Why:** Simple, fast, works perfectly with Render

### Option B: PlanetScale MySQL
- **Cost:** Free tier available
- **Setup Time:** 10 minutes
- **URL:** https://planetscale.com
- **Why:** Serverless MySQL, great for scaling

### Option C: Aiven MySQL
- **Cost:** Free trial, then pay-as-you-go
- **Setup Time:** 10 minutes
- **URL:** https://aiven.io
- **Why:** Professional, reliable, good for production

### Option D: Render PostgreSQL (Alternative)
- **Cost:** Free tier available
- **Setup Time:** 5 minutes
- **URL:** Built into Render dashboard
- **Why:** If you're open to switching from MySQL to PostgreSQL

### Option E: AWS RDS MySQL
- **Cost:** ~$15-20/month minimum
- **Setup Time:** 30 minutes
- **URL:** https://aws.amazon.com/rds
- **Why:** Enterprise-grade, most reliable

**RECOMMENDATION:** Start with **Railway MySQL** (easiest and fastest).

---

## 3. CORRECT JDBC URL FORMATS

### Railway MySQL Format:
```
jdbc:mysql://containers-us-west-XXX.railway.app:XXXXX/railway?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

### PlanetScale Format:
```
jdbc:mysql://aws.connect.psdb.cloud/your-database?sslMode=REQUIRED
```

### Aiven Format:
```
jdbc:mysql://your-instance.aivencloud.com:XXXXX/defaultdb?sslMode=REQUIRED
```

### Render PostgreSQL Format (if switching):
```
jdbc:postgresql://dpg-xxxxx-a.oregon-postgres.render.com:5432/yourdb?sslmode=require
```

**IMPORTANT:** Replace placeholders (XXX, xxxxx, your-database) with actual values from your cloud database provider.

---

## 4. EXACT ENVIRONMENT VARIABLES FOR RENDER

Go to Render Dashboard → Your Web Service → Environment → Add Environment Variable

### REQUIRED Variables (Copy-paste these names):

```
SPRING_PROFILES_ACTIVE=production
```

```
SPRING_DATASOURCE_URL=jdbc:mysql://YOUR_HOST:YOUR_PORT/YOUR_DATABASE?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```
**Replace:** YOUR_HOST, YOUR_PORT, YOUR_DATABASE with actual values

```
SPRING_DATASOURCE_USERNAME=your_database_username
```

```
SPRING_DATASOURCE_PASSWORD=your_database_password
```

### OPTIONAL Variables (Recommended):

```
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```
**Options:** `validate` (production - safest), `update` (auto-create tables), `none` (no changes)

```
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://full-stack-neo-bank22.vercel.app
```
**Replace with your actual Vercel frontend URL**

```
SWAGGER_ENABLED=false
```
**Set to `false` for production security**

---

## 5. STEP-BY-STEP: RAILWAY MYSQL SETUP (RECOMMENDED)

### Step 1: Create Railway Account
1. Go to https://railway.app
2. Sign up with GitHub
3. Click "New Project"

### Step 2: Add MySQL Database
1. Click "+ New"
2. Select "Database" → "Add MySQL"
3. Wait 2-3 minutes for database to provision

### Step 3: Get Connection Details
1. Click on your MySQL service
2. Go to "Variables" tab
3. Copy these values:
   - `MYSQLHOST` (this is your host)
   - `MYSQLPORT` (this is your port)
   - `MYSQLDATABASE` (this is your database name)
   - `MYSQLUSER` (this is your username)
   - `MYSQLPASSWORD` (this is your password)

### Step 4: Build JDBC URL
Format:
```
jdbc:mysql://MYSQLHOST:MYSQLPORT/MYSQLDATABASE?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

Example (replace with your actual values):
```
jdbc:mysql://containers-us-west-123.railway.app:6543/railway?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

### Step 5: Set Render Environment Variables
In Render Dashboard → Your Service → Environment:

| Variable Name | Value |
|--------------|-------|
| `SPRING_PROFILES_ACTIVE` | `production` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://containers-us-west-123.railway.app:6543/railway?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `SPRING_DATASOURCE_USERNAME` | `root` (or your MYSQLUSER value) |
| `SPRING_DATASOURCE_PASSWORD` | `your_password_here` (your MYSQLPASSWORD value) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` (first time) or `validate` (after tables exist) |
| `SPRING_WEB_CORS_ALLOWED_ORIGINS` | `https://full-stack-neo-bank22.vercel.app` |

### Step 6: Deploy
1. Save environment variables in Render
2. Render will automatically rebuild
3. Check logs for successful database connection

---

## 6. TESTING DATABASE CONNECTIVITY AFTER DEPLOYMENT

### Method 1: Check Render Logs
1. Go to Render Dashboard → Your Service → Logs
2. Look for: `HikariPool-1 - Starting...`
3. Look for: `HikariPool-1 - Start completed.`
4. **SUCCESS:** If you see "Start completed" without errors
5. **FAILURE:** If you see "Communications link failure" or "UnknownHostException"

### Method 2: Use Actuator Health Endpoint
1. After deployment, visit: `https://your-render-service.onrender.com/actuator/health`
2. Should return: `{"status":"UP","components":{"db":{"status":"UP"}}}`
3. If `db.status` is `DOWN`, check your connection string

### Method 3: Test API Endpoint
1. Try calling any API endpoint that requires database
2. If it works, database connection is successful
3. If you get 500 errors, check Render logs for database errors

### Method 4: Check Database Tables
1. Connect to your cloud database (Railway/PlanetScale/etc.)
2. Verify tables were created (if using `ddl-auto=update`)
3. Tables should match your JPA entities

---

## 7. TROUBLESHOOTING COMMON ERRORS

### Error: "UnknownHostException"
**Cause:** Invalid host in JDBC URL
**Fix:** Double-check `SPRING_DATASOURCE_URL` host value

### Error: "Access denied for user"
**Cause:** Wrong username or password
**Fix:** Verify `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`

### Error: "Communications link failure"
**Cause:** Firewall blocking connection or wrong port
**Fix:** 
- Verify port in JDBC URL
- Check if database allows connections from Render's IPs
- Some providers require IP whitelisting (Railway doesn't)

### Error: "SSL required"
**Cause:** Database requires SSL but URL doesn't specify it
**Fix:** Add `?useSSL=true` or `?sslMode=REQUIRED` to JDBC URL

### Error: "Table doesn't exist"
**Cause:** Tables not created yet
**Fix:** Set `SPRING_JPA_HIBERNATE_DDL_AUTO=update` temporarily, then switch to `validate`

---

## 8. SECURITY BEST PRACTICES

1. **Never commit database credentials to Git**
2. **Use environment variables only** (already configured)
3. **Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`** after initial setup
4. **Disable Swagger in production:** `SWAGGER_ENABLED=false`
5. **Use SSL for database connections** (already in JDBC URL)
6. **Rotate database passwords regularly**

---

## 9. MIGRATION FROM LOCAL TO CLOUD DATABASE

### Option A: Export/Import SQL
1. Export your local database:
   ```bash
   mysqldump -u root -p springapp > backup.sql
   ```
2. Import to cloud database (via Railway/PlanetScale console)
3. Update Render environment variables
4. Deploy

### Option B: Let Hibernate Create Tables
1. Set `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
2. Deploy to Render
3. Hibernate will create tables automatically
4. **Note:** You'll lose existing data (only structure is created)

---

## 10. QUICK REFERENCE CHECKLIST

- [ ] Created cloud MySQL database (Railway/PlanetScale/Aiven)
- [ ] Copied database connection details
- [ ] Built correct JDBC URL with SSL parameters
- [ ] Set `SPRING_PROFILES_ACTIVE=production` in Render
- [ ] Set `SPRING_DATASOURCE_URL` in Render
- [ ] Set `SPRING_DATASOURCE_USERNAME` in Render
- [ ] Set `SPRING_DATASOURCE_PASSWORD` in Render
- [ ] Set `SPRING_WEB_CORS_ALLOWED_ORIGINS` to your Vercel URL
- [ ] Deployed and checked Render logs
- [ ] Tested `/actuator/health` endpoint
- [ ] Verified database connection success

---

**After completing these steps, your Spring Boot app on Render will successfully connect to your cloud MySQL database!**
