#!/usr/bin/env bash
# Builds ONE deployable jar with the React app inside it.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "→ Testing and building React app"
(cd frontend && npm ci && npm test && npm run build)

echo "→ Copying it into the backend"
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/. backend/src/main/resources/static/

echo "→ Building Spring Boot jar (runs all backend tests)"
(cd backend && mvn -q package)

echo "✓ backend/target/kindred-1.0.0.jar"
