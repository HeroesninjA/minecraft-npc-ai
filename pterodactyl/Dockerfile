FROM php:8.3-cli

RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip curl git libzip-dev libxml2-dev libpng-dev default-mysql-client \
    && docker-php-ext-install bcmath pdo_mysql xml zip gd pcntl \
    && pecl install redis \
    && docker-php-ext-enable redis \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

RUN mkdir -p /var/www/pterodactyl && cd /var/www/pterodactyl \
    && curl -L -o panel.tar.gz https://github.com/pterodactyl/panel/releases/latest/download/panel.tar.gz \
    && tar -xzf panel.tar.gz && rm panel.tar.gz \
    && cp .env.example .env \
    && composer install --no-dev --optimize-autoloader --no-interaction \
    && chmod -R 777 storage bootstrap/cache \
    && rm -rf /root/.composer

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
