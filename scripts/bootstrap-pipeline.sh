#!/usr/bin/env bash
set -euo pipefail

echo "📦 Building all services (slow bootstrap)..."

# Build all Java 21 services
docker run --rm -v "$(pwd):/app" -w /app maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests -pl '!processing-service'

# Build Flink processing-service with Java 17 from the root reactor
docker run --rm -v "$(pwd):/app" -w /app maven:3.9-eclipse-temurin-17 \
  mvn clean package -DskipTests -pl processing-service -am

echo "✅ Packages built."
echo "➡️  Launching pipeline..."
bash ./scripts/run-pipeline.sh
