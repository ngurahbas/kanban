# Kanban Board Application

A modern web-based kanban board application built with Spring Boot and Kotlin, featuring real-time collaboration, OAuth2 authentication, and a responsive user interface.

## 🚀 Quick Start

Get the application running in seconds with Docker and Gradle:

```bash
docker compose up -d && ./gradlew bootRun
```

That's it! The application will be available at `http://localhost:8080`. You can login via [keycloak oauth2 link](http://localhost:8080/oauth2/authorization/keycloak) with credentials: username: `testuser` password: `passsword`.

## 📋 Prerequisites

- **Java 21** - Required runtime environment
- **Docker & Docker Compose** - For running supporting services
- **Gradle** - Build tool (included with the project)

## 🏗️ Architecture Overview

This application uses a modern microservices-oriented architecture with the following components:

### Backend Services
- **Spring Boot** - Main application framework
- **Kotlin** - Programming language
- **PostgreSQL** - Primary database
- **Valkey (Redis-compatible)** - Session storage and caching


### Frontend Technologies
- **JTE Templates** - Server-side rendering with Kotlin
- **Tailwind CSS** - Utility-first CSS framework
- **Alpine.js** - Lightweight JavaScript framework
- **HTMX** - Dynamic HTML updates

## 🔧 Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd kanban
```

### 2. Start Supporting Services
```bash
docker compose up -d
```

This starts:
- PostgreSQL database on port 5432
- Valkey (Redis) on port 6379
- Keycloak on port 9080

### 3. Run the Application
```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

### 4. Access Keycloak Admin Console
- URL: `http://localhost:9080/admin`
- Username: `admin`
- Password: `admin`

## 🔐 Authentication

The application supports OAuth2 authentication with:

1. **Google OAuth2** - Configure with `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` environment variables
2. **Keycloak** - Pre-configured local identity provider

### Google OAuth2 Setup

1. Create a Google Cloud project
2. Enable Google+ API
3. Create OAuth2 credentials
4. Set environment variables:
   ```bash
   export GOOGLE_CLIENT_ID=your-client-id
   export GOOGLE_CLIENT_SECRET=your-client-secret
   ```

### Environment Variables
- `GOOGLE_CLIENT_ID` - Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth2 client secret

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 5432, 6379, 8080, and 9080 are available
2. **Docker issues**: Make sure Docker is running and you have sufficient permissions
3. **Java version**: Ensure Java 21 is installed and set as default
4. **Build failures**: Try `./gradlew clean build` to refresh dependencies

### Getting Help

- Check the application logs for detailed error messages
- Verify all services are running with `docker compose ps`
- Ensure database migrations have run successfully

---


