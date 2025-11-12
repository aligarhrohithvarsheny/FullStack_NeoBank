# NeoBank Docker Management Makefile
# Usage: make <command>

.PHONY: help build up down logs clean restart status health

# Default target
help: ## Show this help message
	@echo "NeoBank Docker Management Commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Development commands
build: ## Build all Docker images
	docker-compose build

up: ## Start all services in development mode
	docker-compose up -d

up-build: ## Build and start all services
	docker-compose up --build -d

down: ## Stop all services
	docker-compose down

down-volumes: ## Stop all services and remove volumes (⚠️ deletes data)
	docker-compose down -v

restart: ## Restart all services
	docker-compose restart

# Logging commands
logs: ## Show logs for all services
	docker-compose logs -f

logs-backend: ## Show backend logs
	docker-compose logs -f backend

logs-frontend: ## Show frontend logs
	docker-compose logs -f frontend

logs-db: ## Show database logs
	docker-compose logs -f db

# Status and health commands
status: ## Show status of all services
	docker-compose ps

health: ## Check health of all services
	@echo "Checking service health..."
	@echo "Backend: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health || echo 'DOWN')"
	@echo "Frontend: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/health || echo 'DOWN')"
	@echo "Database: $$(docker-compose exec -T db mysqladmin ping -h localhost 2>/dev/null && echo 'UP' || echo 'DOWN')"

# Database commands
db-shell: ## Access MySQL shell
	docker-compose exec db mysql -u neo -p springapp

db-backup: ## Backup database to backup.sql
	docker-compose exec db mysqldump -u neo -p springapp > backup-$$(date +%Y%m%d-%H%M%S).sql

db-restore: ## Restore database from backup.sql (usage: make db-restore FILE=backup.sql)
	docker-compose exec -T db mysql -u neo -p springapp < $(FILE)

# Cleanup commands
clean: ## Remove all containers, networks, and images
	docker-compose down --rmi all --volumes --remove-orphans

clean-images: ## Remove all Docker images
	docker image prune -a -f

clean-volumes: ## Remove all Docker volumes
	docker volume prune -f

# Production commands
prod-build: ## Build for production
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

prod-up: ## Start in production mode
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

prod-down: ## Stop production services
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

# Development utilities
shell-backend: ## Access backend container shell
	docker-compose exec backend bash

shell-frontend: ## Access frontend container shell
	docker-compose exec frontend sh

shell-db: ## Access database container shell
	docker-compose exec db bash

# Quick setup
setup: ## Initial setup (copy env file and start services)
	@if [ ! -f .env ]; then cp env.example .env; echo "Created .env file from template"; fi
	@echo "Starting NeoBank services..."
	@make up-build

# Monitoring
monitor: ## Monitor resource usage
	docker stats

# Update commands
update: ## Pull latest images and rebuild
	docker-compose pull
	docker-compose build --no-cache
	docker-compose up -d

