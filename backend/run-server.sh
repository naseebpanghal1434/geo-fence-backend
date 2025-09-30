#!/bin/bash

echo "==================================="
echo "Starting Spring Boot Application"
echo "==================================="

# Get the machine's IP address
IP=$(hostname -I | awk '{print $1}')
if [ -z "$IP" ]; then
    # Fallback to alternative method
    IP=$(ip addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -1)
fi

if [ -z "$IP" ]; then
    echo "⚠️  Could not detect IP address. Using 0.0.0.0"
    IP="0.0.0.0"
fi

echo "Server IP Address: $IP"
echo ""

# Display access URLs
echo "==================================="
echo "Access URLs:"
echo "==================================="
echo "Local:            http://localhost:8080"
echo "Network:          http://$IP:8080"
echo "Health Check:     http://$IP:8080/api/health"
echo "Swagger UI:       http://$IP:8080/swagger-ui.html"
echo "==================================="
echo ""

# Check if Docker services are running
echo "Checking services..."
if command -v docker-compose &> /dev/null; then
    if ! docker-compose ps | grep -q "geofence_postgres.*Up"; then
        echo "⚠️  PostgreSQL is not running. Starting Docker services..."
        docker-compose up -d
        echo "Waiting for services to be ready..."
        sleep 10
    else
        echo "✅ PostgreSQL is running"
    fi

    if ! docker-compose ps | grep -q "geofence_redis.*Up"; then
        echo "⚠️  Redis is not running. Starting Redis..."
        docker-compose up -d redis
        sleep 5
    else
        echo "✅ Redis is running"
    fi
fi

echo ""
echo "Starting Spring Boot application..."
echo "==================================="

# Run with Maven
if command -v mvn &> /dev/null; then
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.address=0.0.0.0"
elif [ -f "target/tse.jar" ]; then
    # Run JAR if Maven not available but JAR exists
    java -jar target/tse.jar --server.address=0.0.0.0
else
    echo "❌ Maven not found and JAR file not built."
    echo "   Please install Maven or build the JAR first with: mvn clean package"
    exit 1
fi