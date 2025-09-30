# Core Application Backend

## Technology Stack
- **Spring Boot**: 2.4.5
- **Java**: JDK 11
- **PostgreSQL**: 13.18
- **Redis**: 7
- **Maven**: Build tool

## Prerequisites
- JDK 11 installed
- Maven 3.6+ installed
- Docker and Docker Compose (for running PostgreSQL and Redis)

## Quick Start

### 1. Start Database and Redis
```bash
docker-compose up -d
```

### 2. Build the Application
```bash
mvn clean install
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

Or run the generated JAR:
```bash
java -jar target/tse.jar
```

## Configuration

### Ports
- **Application**: 8080
- **PostgreSQL**: 5432
- **Redis**: 6379

### Database
- **Database Name**: geofence_db
- **Username**: postgres
- **Password**: postgres

### API Documentation
Once the application is running, access Swagger UI at:
- http://localhost:8080/swagger-ui.html

### Health Check
- http://localhost:8080/api/health

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/tse/core_application/
│   │       ├── config/        # Configuration classes
│   │       ├── controller/    # REST controllers
│   │       ├── service/       # Business logic
│   │       ├── repository/    # Data access layer
│   │       ├── entity/        # JPA entities
│   │       ├── dto/           # Data transfer objects
│   │       ├── exception/     # Custom exceptions
│   │       ├── security/      # Security configurations
│   │       ├── util/          # Utility classes
│   │       └── constants/     # Application constants
│   └── resources/
│       └── application.properties
└── test/
    └── java/
```

## Development Commands

### Run Tests
```bash
mvn test
```

### Generate Code Coverage Report
```bash
mvn jacoco:report
```

### Run Mutation Tests
```bash
mvn pitest:mutationCoverage
```

### Stop Docker Services
```bash
docker-compose down
```

### Stop and Remove Volumes
```bash
docker-compose down -v
```

## Security
- JWT based authentication
- BCrypt password encoding
- CORS configured for localhost:3000 and localhost:4200

## Monitoring
- Actuator endpoints available at `/actuator/health`

## Notes
- The application uses Jasypt for property encryption
- Firebase Admin SDK is included for push notifications
- OAuth2 client support is configured
- WebSocket support is enabled