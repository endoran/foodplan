#!/bin/bash
set -euo pipefail
export PATH=/opt/homebrew/bin:$PATH

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$SCRIPT_DIR/.env.local" ]; then
  set -a
  source "$SCRIPT_DIR/.env.local"
  set +a
fi

NAMESPACE="${K8S_NAMESPACE:-home-staging}"
TENANT="${K8S_TENANT:-test10}"
LOCAL_PORT="${LOCAL_PORT:-9090}"

echo "Starting port-forward to $TENANT in $NAMESPACE on :$LOCAL_PORT..."
kubectl -n "$NAMESPACE" port-forward "svc/foodplan-${TENANT}-frontend" "${LOCAL_PORT}:80" &
PF_PID=$!
trap "kill $PF_PID 2>/dev/null || true" EXIT

for i in $(seq 1 15); do
  if curl -s -o /dev/null "http://localhost:${LOCAL_PORT}/" 2>/dev/null; then
    echo "Port-forward ready."
    break
  fi
  if [ "$i" -eq 15 ]; then
    echo "ERROR: Port-forward failed to become ready after 15s"
    exit 1
  fi
  sleep 1
done

echo "Running Playwright tests..."
cd "$SCRIPT_DIR/.."
BASE_URL="http://localhost:${LOCAL_PORT}" npx playwright test --config e2e/playwright.config.ts "$@"
