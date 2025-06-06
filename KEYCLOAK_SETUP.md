# Spring Boot Gateway with Keycloak Authentication

This project is a Spring Boot application that integrates with Keycloak for authentication and authorization.

## Features

- **Keycloak Integration**: Complete authentication and authorization using Keycloak
- **JWT Token Support**: Stateless authentication using JWT tokens
- **User Registration**: Register new users through Keycloak
- **Role-based Access Control**: Different endpoints for different user roles
- **Swagger Documentation**: Interactive API documentation with JWT authentication support
- **PostgreSQL Database**: Data persistence with JPA and Flyway migrations

## Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL 12+
- Keycloak 22+
- Docker (optional, for running Keycloak)

## Keycloak Setup

### Option 1: Using Docker

1. Run Keycloak using Docker:
```bash
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:22.0.1 start-dev
```

2. Access Keycloak Admin Console at `http://localhost:8080/admin`
   - Username: `admin`
   - Password: `admin`

### Option 2: Download and Install

1. Download Keycloak from [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads)
2. Extract and run: `./bin/kc.sh start-dev`

### Keycloak Configuration

1. **Create a Realm** (optional, or use the default `master` realm)

2. **Create a Client**:
   - Client ID: `spring-gateway`
   - Client authentication: ON
   - Authentication flow: 
     - Standard flow: ON
     - Direct access grants: ON
     - Service accounts roles: ON
   - Valid redirect URIs: `http://localhost:8080/*`
   - Web origins: `http://localhost:8080`

3. **Get Client Secret**:
   - Go to Clients → spring-gateway → Credentials
   - Copy the Client Secret

4. **Create Roles**:
   - Go to Realm roles
   - Create roles: `USER`, `ADMIN`

5. **Create Users**:
   - Go to Users → Add user
   - Set username, email, first name, last name
   - Go to Credentials tab → Set password (temporary: OFF)
   - Go to Role mapping → Assign roles

## Application Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd spring-sentiment
```

2. **Configure environment variables**
```bash
cp .env.example .env
```

Edit `.env` file with your configuration:
```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/gateway_db
DB_USERNAME=postgres
DB_PASSWORD=your_db_password

# FastAPI Service Configuration
FASTAPI_URL=http://localhost:8000

# Keycloak Configuration
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=master
KEYCLOAK_CLIENT_ID=spring-gateway
KEYCLOAK_CLIENT_SECRET=your-client-secret-from-keycloak
```

3. **Create PostgreSQL Database**
```sql
CREATE DATABASE gateway_db;
```

4. **Build and run the application**
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on port 8080 (or another port if 8080 is taken by Keycloak).

## API Endpoints

### Public Endpoints (No Authentication Required)

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/health` - Authentication service health check

### Protected Endpoints (JWT Token Required)

- `GET /api/protected/user-info` - Get current user information
- `GET /api/protected/test` - Test authentication
- `GET /api/protected/health` - Protected service health check

### Role-based Endpoints

- `GET /api/protected/user` - Requires USER or ADMIN role
- `GET /api/protected/admin` - Requires ADMIN role

## Usage Examples

### 1. Register a new user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Login and get JWT token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

Response:
```json
{
  "status": "success",
  "message": "Authentication successful",
  "auth": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 300,
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "scope": "openid profile email"
  },
  "timestamp": "2025-06-06T10:30:00"
}
```

### 3. Access protected endpoint

```bash
curl -X GET http://localhost:8080/api/protected/test \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 4. Get user information

```bash
curl -X GET http://localhost:8080/api/protected/user-info \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Swagger Documentation

Access the interactive API documentation at:
`http://localhost:8080/swagger-ui.html`

The Swagger UI includes:
- All API endpoints with descriptions
- Request/response examples
- JWT authentication support (click "Authorize" button and enter "Bearer <your-jwt-token>")

## Troubleshooting

### Common Issues

1. **Port Conflicts**: If both Keycloak and Spring Boot try to use port 8080:
   - Change Spring Boot port in `application.yml`: `server.port: 8081`
   - Or run Keycloak on different port: `docker run -p 8180:8080 ...`

2. **Database Connection Issues**:
   - Verify PostgreSQL is running
   - Check database credentials in `.env` file
   - Ensure database exists

3. **Keycloak Connection Issues**:
   - Verify Keycloak is running and accessible
   - Check Keycloak URL in configuration
   - Verify client configuration in Keycloak

4. **JWT Token Issues**:
   - Check token expiration
   - Verify issuer URI matches Keycloak realm
   - Ensure client secret is correct

### Logs

Check application logs for detailed error information:
```bash
tail -f logs/application.log
```

## Development

### Adding New Protected Endpoints

1. Create controller method
2. Add `@PreAuthorize` annotation for role-based access
3. Use `Authentication` parameter to get user info

Example:
```java
@GetMapping("/admin-only")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint(Authentication auth) {
    // Your code here
}
```

### Custom JWT Claims

To access custom claims from JWT token:
```java
Jwt jwt = (Jwt) authentication.getPrincipal();
String customClaim = jwt.getClaimAsString("custom_claim");
```

## Security Notes

- Always use HTTPS in production
- Keep client secrets secure
- Regularly rotate JWT signing keys
- Implement proper token expiration policies
- Use strong passwords for Keycloak admin accounts

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request
