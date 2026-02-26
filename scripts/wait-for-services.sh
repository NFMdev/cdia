#!/usr/bin/env bash
set -euo pipefail

ELASTIC_PASSWORD="${ELASTIC_PASSWORD:-test}"

echo "⏳ Waiting for PostgreSQL..."
until docker exec postgres pg_isready -U admin > /dev/null 2>&1; do
  sleep 2
done
echo "✅ PostgreSQL ready."

echo "⏳ Waiting for Elasticsearch..."
until curl -fsS -u "elastic:${ELASTIC_PASSWORD}" "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=5s" > /dev/null; do
  sleep 2
done
echo "✅ Elasticsearch ready."

echo "⏳ Waiting for ingestion-service..."
until curl -fsS http://localhost:8080/events > /dev/null; do
  sleep 2
done
echo "✅ ingestion-service ready."

echo "⏳ Waiting for Flink JobManager..."
until curl -s http://localhost:8081/jobs/overview > /dev/null; do
  sleep 2
done
echo "✅ Flink JobManager ready."
