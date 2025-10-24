# Use OpenJDK 21 as base image for development
FROM openjdk:21-jdk-slim

# Install necessary tools and Maven
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Expose ports
EXPOSE 8080 5005

# Default command for development (can be overridden)
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"]