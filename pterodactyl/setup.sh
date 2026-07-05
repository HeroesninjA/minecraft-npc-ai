#!/bin/bash
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# Generate APP_KEY if empty
if grep -q 'APP_KEY=$' .env || grep -q 'APP_KEY=.*YourKey' .env; then
    echo "Generating APP_KEY..."
    APP_KEY=$(docker run --rm php:8.3-cli php -r 'echo "base64:" . base64_encode(random_bytes(32));')
    sed -i "s|^APP_KEY=.*|APP_KEY=$APP_KEY|" .env
    echo "APP_KEY generated: $APP_KEY"
fi

# Set passwords if default
if grep -q 'change_me' .env; then
    echo "Generating secure passwords..."
    DB_ROOT_PASS=$(openssl rand -base64 24)
    DB_PASS=$(openssl rand -base64 24)
    sed -i "s|MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$DB_ROOT_PASS|" .env
    sed -i "s|MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$DB_PASS|" .env
    echo "Passwords generated"
fi

# Start services
echo "=== Starting Pterodactyl stack ==="
docker compose up -d --build

echo "=== Waiting for panel to be ready ==="
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w '%{http_code}' http://localhost:8081 2>/dev/null | grep -q 200; then
        echo "Panel ready after ${i}s"
        break
    fi
    sleep 2
done

echo "=== Panel URL: http://141.147.48.170:8081 ==="
echo "=== Admin: admin / f9ddcb53eaae889b09cfba0a ==="
