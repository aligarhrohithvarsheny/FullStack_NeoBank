#!/bin/bash

echo "🔍 NeoBank Docker Debug Script"
echo "================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

echo "✅ Docker is running"

# Check if MySQL is running on host
echo "🔍 Checking MySQL connection..."
if nc -z localhost 3306 2>/dev/null; then
    echo "✅ MySQL is running on localhost:3306"
else
    echo "❌ MySQL is not running on localhost:3306"
    echo "Please start MySQL on your host machine with:"
    echo "  - Host: localhost"
    echo "  - Port: 3306"
    echo "  - Database: springapp"
    echo "  - Username: root"
    echo "  - Password: 1234"
fi

# Build and start services
echo "🏗️ Building and starting services..."

# Stop existing containers
docker-compose -f docker-compose.simple.yml down

# Build and start
docker-compose -f docker-compose.simple.yml up --build -d

echo "⏳ Waiting for services to start..."
sleep 10

# Check backend health
echo "🔍 Checking backend health..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Backend is healthy"
else
    echo "❌ Backend is not responding"
    echo "Backend logs:"
    docker logs neobank-backend --tail 20
fi

# Check frontend health
echo "🔍 Checking frontend health..."
if curl -f http://localhost:3000/health > /dev/null 2>&1; then
    echo "✅ Frontend is healthy"
else
    echo "❌ Frontend is not responding"
    echo "Frontend logs:"
    docker logs neobank-frontend --tail 20
fi

echo ""
echo "🌐 Access URLs:"
echo "  Frontend: http://localhost:3000"
echo "  Backend:  http://localhost:8080"
echo "  API Docs: http://localhost:8080/swagger-ui.html"
echo ""
echo "📋 Useful commands:"
echo "  View logs: docker-compose -f docker-compose.simple.yml logs -f"
echo "  Stop all:  docker-compose -f docker-compose.simple.yml down"
echo "  Restart:   docker-compose -f docker-compose.simple.yml restart"
