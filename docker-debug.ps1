# NeoBank Docker Debug Script for Windows
Write-Host "🔍 NeoBank Docker Debug Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Check if Docker is running
try {
    docker info | Out-Null
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if MySQL is running on host
Write-Host "🔍 Checking MySQL connection..." -ForegroundColor Yellow
try {
    $mysqlTest = Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue
    if ($mysqlTest.TcpTestSucceeded) {
        Write-Host "✅ MySQL is running on localhost:3306" -ForegroundColor Green
    } else {
        Write-Host "❌ MySQL is not running on localhost:3306" -ForegroundColor Red
        Write-Host "Please start MySQL on your host machine with:" -ForegroundColor Yellow
        Write-Host "  - Host: localhost" -ForegroundColor White
        Write-Host "  - Port: 3306" -ForegroundColor White
        Write-Host "  - Database: springapp" -ForegroundColor White
        Write-Host "  - Username: root" -ForegroundColor White
        Write-Host "  - Password: 1234" -ForegroundColor White
    }
} catch {
    Write-Host "❌ Could not check MySQL connection" -ForegroundColor Red
}

# Build and start services
Write-Host "🏗️ Building and starting services..." -ForegroundColor Yellow

# Stop existing containers
Write-Host "Stopping existing containers..." -ForegroundColor Yellow
docker-compose -f docker-compose.simple.yml down

# Build and start
Write-Host "Starting services..." -ForegroundColor Yellow
docker-compose -f docker-compose.simple.yml up --build -d

Write-Host "⏳ Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check backend health
Write-Host "🔍 Checking backend health..." -ForegroundColor Yellow
try {
    $backendResponse = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5
    if ($backendResponse.StatusCode -eq 200) {
        Write-Host "✅ Backend is healthy" -ForegroundColor Green
    } else {
        Write-Host "❌ Backend is not responding" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Backend is not responding" -ForegroundColor Red
    Write-Host "Backend logs:" -ForegroundColor Yellow
    docker logs neobank-backend --tail 20
}

# Check frontend health
Write-Host "🔍 Checking frontend health..." -ForegroundColor Yellow
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:3000/health" -TimeoutSec 5
    if ($frontendResponse.StatusCode -eq 200) {
        Write-Host "✅ Frontend is healthy" -ForegroundColor Green
    } else {
        Write-Host "❌ Frontend is not responding" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Frontend is not responding" -ForegroundColor Red
    Write-Host "Frontend logs:" -ForegroundColor Yellow
    docker logs neobank-frontend --tail 20
}

Write-Host ""
Write-Host "🌐 Access URLs:" -ForegroundColor Cyan
Write-Host "  Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "  Backend:  http://localhost:8080" -ForegroundColor White
Write-Host "  API Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "📋 Useful commands:" -ForegroundColor Cyan
Write-Host "  View logs: docker-compose -f docker-compose.simple.yml logs -f" -ForegroundColor White
Write-Host "  Stop all:  docker-compose -f docker-compose.simple.yml down" -ForegroundColor White
Write-Host "  Restart:   docker-compose -f docker-compose.simple.yml restart" -ForegroundColor White
