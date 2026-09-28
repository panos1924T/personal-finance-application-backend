#!/bin/bash

set -e

ENV_FILE=".env.docker"

if [ ! -f "$ENV_FILE" ]; then
    echo "Missing $ENV_FILE"
    echo "Create it from env.example first."
    exit 1
fi

set -a
. "./$ENV_FILE"
set +a

APP_PORT="${APP_PORT:-8081}"

echo "Building and starting Personal Finance..."
docker compose --env-file "$ENV_FILE" up -d --build

echo ""
echo "Containers:"
docker compose --env-file "$ENV_FILE" ps

echo ""
echo "Waiting for application on port $APP_PORT..."

MAX_RETRIES=60
RETRY=0

while [ "$RETRY" -lt "$MAX_RETRIES" ]; do

    if curl -fsS "http://localhost:$APP_PORT" > /dev/null 2>&1; then
        echo ""
        echo "Personal Finance is ready:"
        echo "http://localhost:$APP_PORT"
        exit 0
    fi

    RETRY=$((RETRY + 1))
    echo -n "."
    sleep 2
done

echo ""
echo "Application did not become ready in time."
echo "Check logs with:"
echo "docker compose --env-file $ENV_FILE logs"
exit 1
