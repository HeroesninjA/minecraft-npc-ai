#!/bin/bash
set -e

cd /var/www/pterodactyl

# Configure .env from runtime environment
sed -i "s|APP_KEY=.*|APP_KEY=${APP_KEY}|" .env
sed -i "s|APP_URL=.*|APP_URL=${APP_URL}|" .env
sed -i "s|DB_HOST=.*|DB_HOST=${DB_HOST}|" .env
sed -i "s|DB_PORT=.*|DB_PORT=${DB_PORT}|" .env
sed -i "s|DB_DATABASE=.*|DB_DATABASE=${DB_DATABASE}|" .env
sed -i "s|DB_USERNAME=.*|DB_USERNAME=${DB_USERNAME}|" .env
sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=${DB_PASSWORD}|" .env
sed -i "s|REDIS_HOST=.*|REDIS_HOST=${REDIS_HOST}|" .env
sed -i "s|REDIS_PORT=.*|REDIS_PORT=${REDIS_PORT}|" .env
sed -i "s|QUEUE_CONNECTION=sync|QUEUE_CONNECTION=redis|" .env

echo "=== Waiting for database ==="
for i in $(seq 1 30); do
    if php artisan migrate --seed --force 2>/dev/null; then
        echo "Migration complete"
        break
    fi
    echo "  Attempt $i/30..."
    sleep 2
done

php artisan p:user:make \
    --email="${ADMIN_EMAIL}" \
    --username="${ADMIN_USER}" \
    --name-first="Admin" \
    --name-last="User" \
    --password="${ADMIN_PASSWORD}" \
    --admin=1 \
    --no-interaction 2>/dev/null || true

echo "=== Panel ready on http://0.0.0.0:8080 ==="
php artisan serve --host=0.0.0.0 --port=8080
