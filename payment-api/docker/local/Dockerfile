# Use PHP 8.2 CLI for development
FROM php:8.2-cli

# Install system dependencies
RUN apt-get update && apt-get install -y \
    git \
    curl \
    libpng-dev \
    libonig-dev \
    libxml2-dev \
    libzip-dev \
    zip \
    unzip \
    nodejs \
    npm \
    && docker-php-ext-install pdo_mysql mbstring exif pcntl bcmath gd zip

# Install Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Set working directory
WORKDIR /app

# Copy composer files
COPY composer.json composer.lock ./

# Copy application code (needed for artisan commands in post-install scripts)
COPY . .

# Install PHP dependencies (skip scripts that require artisan)
RUN composer install --no-scripts

# Run post-install scripts after application code is available
RUN composer run-script post-autoload-dump

# Copy package files
COPY package.json ./

# Install Node dependencies
RUN npm install

# Copy application code
COPY . .

# Set permissions
RUN chown -R www-data:www-data /app \
    && chmod -R 755 /app/storage \
    && chmod -R 755 /app/bootstrap/cache

# Expose port
EXPOSE 8000

# Default command for development
CMD ["php", "artisan", "serve", "--host=0.0.0.0", "--port=8000"]
