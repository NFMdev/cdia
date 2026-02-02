#!/bin/bash
set -e

echo "📦 Building all services..."

# Build all services
docker run --rm -v "$(pwd):/app" -w /app maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests -pl '!processing-service'
docker run --rm -v "$(pwd)/processing-service:/app" -w /app maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests

echo "✅ Packages built!"

echo "🚀 Starting containers..."
docker compose up -d

echo "🌐 Waiting for services (Postgres + Elasticsearch) to be ready..."
./scripts/wait-for-services.sh

echo "📦 Submitting Flink job..."
docker exec -it flink-jobmanager flink run -m flink-jobmanager:8081 /opt/flink/usrlib/processing-service.jar \
  --job-class com.github.NFMdev.cdia.processing_service.flink.jobs.AnomalyJob

echo ""
echo "🎉 Pipeline started successfully!"
echo "🗄️  Postgres:          localhost:5432  (admin/admin)"
echo "🔍 Elasticsearch:     http://localhost:9200  (elastic/test)"
echo "🌊 Flink Dashboard:   http://localhost:8081"
echo "💡 To stop everything: docker compose down -v"
echo ""