# 🐳 NeoBank Docker Troubleshooting Guide

## Quick Fix for 404 Error

### Option 1: Standalone Setup (Recommended)
```bash
# Use the standalone Docker Compose with built-in MySQL
docker-compose -f docker-compose.standalone.yml up --build -d
```

### Option 2: Fix Existing Setup
```bash
# Make sure MySQL is running on your host
# Then use the original compose file
docker-compose -f docker-compose.simple.yml up --build -d
```

## Common Issues & Solutions

### 1. 404 Error - "This application has no explicit mapping for /error"

**Cause**: Backend not starting properly or database connection issues

**Solutions**:
```bash
# Check backend logs
docker logs neobank-backend

# Check if backend is healthy
curl http://localhost:8080/actuator/health

# Restart backend
docker-compose restart backend
```

### 2. Database Connection Issues

**For docker-compose.simple.yml** (uses host MySQL):
```bash
# Make sure MySQL is running on host with these settings:
# - Host: localhost:3306
# - Database: springapp
# - Username: root
# - Password: 1234
```

**For docker-compose.standalone.yml** (uses container MySQL):
```bash
# MySQL runs in container, no host setup needed
# Database will be created automatically
```

### 3. Frontend Not Loading

**Check**:
```bash
# Check frontend logs
docker logs neobank-frontend

# Check if nginx is running
docker exec neobank-frontend nginx -t

# Check frontend health
curl http://localhost:3000/health
```

### 4. API Calls Failing

**Check nginx proxy**:
```bash
# Test API directly
curl http://localhost:8080/api/users

# Test through frontend proxy
curl http://localhost:3000/api/users
```

## Debug Commands

### Check All Services
```bash
# View all container status
docker-compose ps

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Health Checks
```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost:3000/health

# Database connection (if using standalone)
docker exec neobank-db mysql -u neo -pneo123 -e "SELECT 1"
```

### Reset Everything
```bash
# Stop and remove all containers
docker-compose down -v

# Remove all images
docker-compose down --rmi all

# Start fresh
docker-compose up --build -d
```

## Access URLs

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Database** (if standalone): localhost:3306

## Environment Variables

Create a `.env` file for custom configuration:
```env
# Database
DB_NAME=springapp
DB_USER=neo
DB_PASSWORD=neo123

# Ports
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

## Performance Tips

1. **Use standalone setup** for development (includes MySQL)
2. **Use simple setup** for production (external MySQL)
3. **Enable Docker BuildKit** for faster builds:
   ```bash
   export DOCKER_BUILDKIT=1
   ```

## Still Having Issues?

1. **Check Docker Desktop is running**
2. **Check ports are not in use**:
   ```bash
   netstat -an | grep :3000
   netstat -an | grep :8080
   netstat -an | grep :3306
   ```
3. **Check firewall settings**
4. **Try different ports** in docker-compose files
5. **Check system resources** (RAM, disk space)
