#!/bin/bash
set -e
cd "$(dirname "$0")"

# Generate secure passwords
MYSQL_ROOT_PASS=$(openssl rand -base64 24 | tr '+/' '-_')
MYSQL_PASS=$(openssl rand -base64 24 | tr '+/' '-_')
APP_KEY=$(docker run --rm php:8.3-fpm-bullseye php -r 'echo "base64:" . base64_encode(random_bytes(32));')

cat > .env << EOF
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASS
MYSQL_PASSWORD=$MYSQL_PASS
APP_KEY=$APP_KEY
APP_URL=http://141.147.48.170:8081
ADMIN_USER=admin
ADMIN_PASSWORD=f9ddcb53eaae889b09cfba0a
ADMIN_EMAIL=admin@localhost
EOF

echo "Starting Pterodactyl stack..."
docker compose up -d --build

echo "Waiting for panel..."
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w '%{http_code}' http://localhost:8081 2>/dev/null | grep -q 200; then
        echo "Panel ready after ${i}s"
        break
    fi
    sleep 2
done

echo ""
echo "=== PTERODACTYL PANEL READY ==="
echo "URL: http://141.147.48.170:8081"
echo "Admin: admin / f9ddcb53eaae889b09cfba0a"
echo ""
echo "Credentials saved in: $(pwd)/.env"
echo "Backup: docker compose down && tar czf pterodactyl-backup.tar.gz panel-data/ .env"
