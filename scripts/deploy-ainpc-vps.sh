#!/bin/bash
# Run on VPS after JARs are deployed: copies to Pterodactyl server dir and restarts
set -e

SERVER_UUID="cd8c1a26-8639-40fd-9e52-aa95bfc11f3c"
PLUGIN_DIR="/var/lib/pterodactyl/$SERVER_UUID/plugins"
SOURCE_DIR="/home/ubuntu/testserver/paper-data/plugins"

# Copy new JARs from source if provided
if [ -d "$SOURCE_DIR" ]; then
    echo "Copying JARs from $SOURCE_DIR..."
    sudo cp "$SOURCE_DIR"/ainpc-core-plugin*.jar "$SOURCE_DIR"/ainpc-scenario-medieval*.jar "$PLUGIN_DIR/" 2>/dev/null || true
fi

# Fix permissions
sudo chown pterodactyl:pterodactyl "$PLUGIN_DIR"/*.jar 2>/dev/null
sudo chmod 644 "$PLUGIN_DIR"/*.jar 2>/dev/null

# Restart server via Wings API
echo "Restarting server..."
curl -s -X POST "http://localhost:8080/api/servers/$SERVER_UUID/power" \
    -H "Authorization: Bearer 0c0oUWMqhpqTzujB0HC29es3mEhYoJpHAdzf2ZrW7WfSerOAg3VyhxjAlLYdeNvn" \
    -H "Content-Type: application/json" \
    -d '{"action":"restart}'

echo "Server restart command sent. Check status in Panel."
