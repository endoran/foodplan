#!/bin/bash
# Deploy FoodPlan to Methuselah
set -e

REMOTE="ptgomez@methuselah.local"
DEPLOY_DIR="~/foodplan"

echo "Deploying FoodPlan to Methuselah..."
ssh "$REMOTE" "export PATH=/usr/local/bin:\$PATH && cd $DEPLOY_DIR && git pull origin v0.1 && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build"
echo "Deploy complete. App available at http://methuselah.local:3001/"
