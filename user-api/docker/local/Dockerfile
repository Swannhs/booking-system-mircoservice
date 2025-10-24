# Use Node.js 18 Alpine for development
FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install all dependencies (including dev dependencies for development)
RUN npm install

# Copy source code
COPY . .

# Expose port
EXPOSE 3000

# Default command for development (can be overridden)
CMD ["npm", "run", "start:dev"]