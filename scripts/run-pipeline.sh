#!/usr/bin/env bash
set -euo pipefail

POSTGRES_DB="${POSTGRES_DB:-cdia}"
CDIA_MIGRATIONS_DB_USER="${CDIA_MIGRATIONS_DB_USER:-cdia_migrations}"

echo "🚀 Starting containers (fast mode, no build)..."
docker compose --profile core --profile apps --profile streaming up -d

echo "🌐 Waiting for services (Postgres + Elasticsearch) to be ready..."
bash ./scripts/wait-for-services.sh

echo "📦 Submitting Flink job..."
docker exec flink-jobmanager flink run -m flink-jobmanager:8081 /opt/flink/usrlib/processing-service.jar \
  --job-class com.github.NFMdev.cdia.processing_service.flink.job.AnomalyJob

echo ""
echo "🎉 Pipeline started successfully!"
echo "🗄️  Postgres:          localhost:5432  (${CDIA_MIGRATIONS_DB_USER}/******, db=${POSTGRES_DB})"
echo "🔍 Elasticsearch:     http://localhost:9200  (elastic/test)"
echo "🌊 Flink Dashboard:   http://localhost:8081"
echo "📈 Postgres Exporter: http://localhost:9187/metrics"
echo "💡 To stop everything: docker compose down -v"
echo ""
