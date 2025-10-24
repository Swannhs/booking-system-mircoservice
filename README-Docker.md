# Docker Development Setup

This document explains how to set up and run the booking-system-microservice project using Docker for local development.

## Prerequisites

- Docker
- Docker Compose

## Quick Start

### Development Environment (with hot reload)

```bash
# Start all services in development mode
docker-compose -f docker-compose.dev.yml up --build

# Or run in detached mode
docker-compose -f docker-compose.dev.yml up -d --build
```

### Local Environment (production-like)

```bash
# Start all services in local production-like mode
docker-compose up --build

# Or detached mode
docker-compose up -d --build
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| booking-api | 8080 | Spring Boot application with PostgreSQL |
| payment-api | 8000 | Laravel application with MySQL |
| user-api | 3000 | NestJS application |
| booking-db | 5432 | PostgreSQL database |
| payment-db | 3306 | MySQL database |

## Development Features

### Hot Reload
- **booking-api**: Spring DevTools enabled with volume mounts for source code
- **payment-api**: Laravel with volume mounts for all application files
- **user-api**: NestJS with watch mode and volume mounts for source code

### Volume Mounts
- Source code is mounted as volumes, so changes are reflected immediately
- Database data is persisted in named volumes

## Useful Commands

```bash
# View logs
docker-compose -f docker-compose.dev.yml logs -f [service-name]

# Stop services
docker-compose -f docker-compose.dev.yml down

# Rebuild specific service
docker-compose -f docker-compose.dev.yml up --build [service-name]

# Clean up
docker-compose -f docker-compose.dev.yml down -v --remove-orphans
```

## Environment Variables

### Laravel (payment-api)
Make sure to set a proper `APP_KEY` in the docker-compose.dev.yml:

```bash
# Generate a new key
docker-compose -f docker-compose.dev.yml exec payment-api php artisan key:generate
```

### Database Connections
- **booking-api**: Connects to PostgreSQL at `booking-db:5432`
- **payment-api**: Connects to MySQL at `payment-db:3306`

## Troubleshooting

### Port Conflicts
If ports are already in use, modify the port mappings in docker-compose.dev.yml:

```yaml
ports:
  - "8081:8080"  # Change host port from 8080 to 8081
```

### Permission Issues
For Laravel file permissions:

```bash
docker-compose -f docker-compose.dev.yml exec payment-api chown -R www-data:www-data /var/www/html
```

### Database Connection Issues
Wait for databases to be fully initialized before starting the APIs:

```bash
docker-compose -f docker-compose.dev.yml up booking-db payment-db -d
# Wait a few seconds, then start the APIs
docker-compose -f docker-compose.dev.yml up booking-api payment-api user-api