# 🐳 NeoBank Docker Setup Guide

This guide will help you containerize and run your NeoBank application using Docker and Docker Compose.

## 📋 Prerequisites

### Required Software
- **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop)
- **Docker Compose** (included with Docker Desktop)
- **Visual Studio Code** with Docker extension (recommended)

### Verify Installation
```bash
docker --version
docker-compose --version
docker run hello-world
```

## 🏗️ Project Structure

```
FULL STACKNEOBANK/
├── angularapp/                 # Angular Frontend
│   ├── Dockerfile
│   ├── nginx.conf
│   └── .dockerignore
├── springapp/                  # Spring Boot Backend
│   ├── Dockerfile
│   └── .dockerignore
├── init-scripts/               # Database initialization
│   └── 01-init.sql
├── docker-compose.yml          # Main orchestration file
├── env.example                 # Environment variables template
└── .dockerignore
```

## 🚀 Quick Start

### 1. Environment Setup
```bash
# Copy the environment template
cp env.example .env

# Edit the .env file with your preferred settings
# Default values are already configured for development
```

### 2. Build and Run
```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode (background)
docker-compose up --build -d
```

### 3. Access Your Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Database**: localhost:3306

## 🔧 Service Details

### Frontend (Angular)
- **Port**: 3000
- **Technology**: Angular 20 with SSR
- **Web Server**: Nginx
- **Features**: 
  - Production build optimization
  - Gzip compression
  - Security headers
  - API proxy to backend

### Backend (Spring Boot)
- **Port**: 8080
- **Technology**: Spring Boot 3.5.5 with Java 21
- **Database**: MySQL 8.0
- **Features**:
  - Multi-stage Docker build
  - Health checks
  - Actuator endpoints
  - Swagger documentation

### Database (MySQL)
- **Port**: 3306
- **Version**: MySQL 8.0
- **Features**:
  - Persistent data storage
  - Initialization scripts
  - Health checks
  - Optimized for development

### Cache (Redis)
- **Port**: 6379
- **Version**: Redis 7
- **Features**: Optional caching layer

## 🛠️ Development Commands

### Basic Operations
```bash
# Start services
docker-compose up

# Start in background
docker-compose up -d

# Stop services
docker-compose down

# Stop and remove volumes (⚠️ deletes database data)
docker-compose down -v

# View logs
docker-compose logs

# View logs for specific service
docker-compose logs backend
docker-compose logs frontend
docker-compose logs db
```

### Building and Rebuilding
```bash
# Rebuild all services
docker-compose build

# Rebuild specific service
docker-compose build backend
docker-compose build frontend

# Force rebuild (no cache)
docker-compose build --no-cache
```

### Database Operations
```bash
# Access MySQL shell
docker-compose exec db mysql -u neo -p springapp

# Backup database
docker-compose exec db mysqldump -u neo -p springapp > backup.sql

# Restore database
docker-compose exec -T db mysql -u neo -p springapp < backup.sql
```

### Debugging
```bash
# Check service status
docker-compose ps

# View service health
docker-compose exec backend curl http://localhost:8080/actuator/health
docker-compose exec frontend curl http://localhost:80/health

# Access container shell
docker-compose exec backend bash
docker-compose exec frontend sh
```

## 🔒 Environment Variables

Key environment variables in `.env`:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_ROOT_PASSWORD` | `root123` | MySQL root password |
| `DB_NAME` | `springapp` | Database name |
| `DB_USER` | `neo` | Database user |
| `DB_PASSWORD` | `neo123` | Database password |
| `BACKEND_PORT` | `8080` | Backend service port |
| `FRONTEND_PORT` | `3000` | Frontend service port |
| `REDIS_PORT` | `6379` | Redis service port |

## 🏥 Health Checks

All services include health checks:

- **Backend**: `http://localhost:8080/actuator/health`
- **Frontend**: `http://localhost:3000/health`
- **Database**: MySQL ping check
- **Redis**: Redis ping check

## 🔧 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Check what's using the port
netstat -tulpn | grep :8080

# Change port in .env file
BACKEND_PORT=8081
```

#### Database Connection Issues
```bash
# Check database logs
docker-compose logs db

# Restart database
docker-compose restart db

# Check database connectivity
docker-compose exec backend ping db
```

#### Frontend Build Issues
```bash
# Clear npm cache
docker-compose exec frontend npm cache clean --force

# Rebuild frontend
docker-compose build --no-cache frontend
```

#### Backend Build Issues
```bash
# Clear Maven cache
docker-compose exec backend mvn clean

# Rebuild backend
docker-compose build --no-cache backend
```

### Performance Optimization

#### For Development
```bash
# Use bind mounts for faster development
# Add to docker-compose.yml under backend service:
volumes:
  - ./springapp/src:/app/src
```

#### For Production
```bash
# Use production profiles
SPRING_PROFILES_ACTIVE=prod

# Optimize JVM settings
# Add to backend Dockerfile:
ENV JAVA_OPTS="-Xmx512m -Xms256m"
```

## 📦 Production Deployment

### Build Production Images
```bash
# Build for production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# Push to registry
docker tag neobank-backend your-registry/neobank-backend:latest
docker tag neobank-frontend your-registry/neobank-frontend:latest
docker push your-registry/neobank-backend:latest
docker push your-registry/neobank-frontend:latest
```

### Security Considerations
1. Change default passwords in production
2. Use secrets management for sensitive data
3. Enable SSL/TLS certificates
4. Configure firewall rules
5. Regular security updates

## 🎯 Next Steps

1. **Customize Configuration**: Modify `.env` file for your needs
2. **Add Monitoring**: Consider adding Prometheus/Grafana
3. **CI/CD Pipeline**: Set up automated builds and deployments
4. **Load Balancing**: Add multiple backend instances
5. **Backup Strategy**: Implement automated database backups

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Angular Docker Guide](https://angular.io/guide/deployment#docker)

## 🆘 Support

If you encounter issues:
1. Check the logs: `docker-compose logs`
2. Verify environment variables
3. Ensure all ports are available
4. Check Docker Desktop is running
5. Review this documentation

---

**Happy Containerizing! 🐳**

