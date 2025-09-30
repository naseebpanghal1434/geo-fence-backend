#!/bin/bash

echo "==================================="
echo "Spring Boot Application Setup Script"
echo "==================================="

# Check Java installation
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install JDK 11 first."
    echo "   Ubuntu/Debian: sudo apt-get install openjdk-11-jdk"
    echo "   CentOS/RHEL: sudo yum install java-11-openjdk-devel"
    echo "   macOS: brew install openjdk@11"
    exit 1
else
    echo "✅ Java is installed"
    java -version
fi

# Check Maven installation
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6+ first."
    echo "   Ubuntu/Debian: sudo apt-get install maven"
    echo "   CentOS/RHEL: sudo yum install maven"
    echo "   macOS: brew install maven"
    exit 1
else
    echo "✅ Maven is installed"
    mvn -version
fi

# Check Docker installation
if ! command -v docker &> /dev/null; then
    echo "⚠️  Docker is not installed. You'll need it to run PostgreSQL and Redis."
    echo "   Install from: https://docs.docker.com/get-docker/"
else
    echo "✅ Docker is installed"
    docker --version
fi

# Check Docker Compose installation
if ! command -v docker-compose &> /dev/null; then
    echo "⚠️  Docker Compose is not installed."
    echo "   Install from: https://docs.docker.com/compose/install/"
else
    echo "✅ Docker Compose is installed"
    docker-compose --version
fi

echo ""
echo "==================================="
echo "Starting Services"
echo "==================================="

# Start Docker services if Docker is available
if command -v docker-compose &> /dev/null; then
    echo "Starting PostgreSQL and Redis..."
    docker-compose up -d
    echo "Waiting for services to be ready..."
    sleep 10
    echo "✅ Services started"
else
    echo "⚠️  Skipping Docker services. Make sure PostgreSQL and Redis are running manually."
fi

echo ""
echo "==================================="
echo "Building Application"
echo "==================================="

# Build the application
echo "Building Spring Boot application..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "==================================="
    echo "Setup Complete!"
    echo "==================================="
    echo ""
    echo "You can now run the application with:"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Or run the JAR file:"
    echo "  java -jar target/tse.jar"
    echo ""
    echo "Application will be available at: http://localhost:8080"
    echo "Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "Health Check: http://localhost:8080/api/health"
else
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi