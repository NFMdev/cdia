#!/usr/bin/env bash
set -euo pipefail

# Minimal reproducible benchmark harness for local portfolio runs.
# It drives simulator load for a fixed duration and snapshots key APIs/metrics.

DURATION_SECONDS="${DURATION_SECONDS:-120}"
OUT_ROOT="${OUT_ROOT:-benchmarks}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="${OUT_ROOT}/${TIMESTAMP}"

SIMULATOR_BASE_URL="${SIMULATOR_BASE_URL:-http://localhost:8082/simulator}"
INGESTION_BASE_URL="${INGESTION_BASE_URL:-http://localhost:8080}"
SEARCH_BASE_URL="${SEARCH_BASE_URL:-http://localhost:8085}"
PROMETHEUS_BASE_URL="${PROMETHEUS_BASE_URL:-http://localhost:9090}"

mkdir -p "${OUT_DIR}"

echo "Benchmark output directory: ${OUT_DIR}"
echo "duration_seconds=${DURATION_SECONDS}" > "${OUT_DIR}/metadata.txt"
echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "${OUT_DIR}/metadata.txt"
echo "git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)" >> "${OUT_DIR}/metadata.txt"

echo "Starting simulator..."
curl -fsS -X POST "${SIMULATOR_BASE_URL}/start" > "${OUT_DIR}/simulator-start.json"

echo "Capturing baseline snapshots..."
curl -fsS "${INGESTION_BASE_URL}/actuator/metrics/http.server.requests" > "${OUT_DIR}/ingestion-http-baseline.json"
curl -fsS "${SEARCH_BASE_URL}/search/anomalies?page=0&size=1&onlyActive=false" > "${OUT_DIR}/anomalies-baseline.json"

echo "Running benchmark load for ${DURATION_SECONDS}s..."
sleep "${DURATION_SECONDS}"

echo "Stopping simulator..."
curl -fsS -X POST "${SIMULATOR_BASE_URL}/stop" > "${OUT_DIR}/simulator-stop.json"

echo "Capturing post-run snapshots..."
curl -fsS "${INGESTION_BASE_URL}/actuator/metrics/http.server.requests" > "${OUT_DIR}/ingestion-http-final.json"
curl -fsS "${SEARCH_BASE_URL}/search/anomalies?page=0&size=50&onlyActive=false" > "${OUT_DIR}/anomalies-final.json"

# Prometheus is optional; capture if reachable.
if curl -fsS "${PROMETHEUS_BASE_URL}/-/ready" > /dev/null 2>&1; then
  echo "Prometheus detected, collecting query snapshots..."
  curl -fsS --get \
    --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{app="ingestion-service"}[1m]))' \
    "${PROMETHEUS_BASE_URL}/api/v1/query" > "${OUT_DIR}/prom-ingestion-rps.json"
  curl -fsS --get \
    --data-urlencode 'query=histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{app="ingestion-service"}[5m])) by (le))' \
    "${PROMETHEUS_BASE_URL}/api/v1/query" > "${OUT_DIR}/prom-ingestion-p95.json"
else
  echo "Prometheus not reachable; skipping Prometheus snapshots." > "${OUT_DIR}/prometheus-skip.txt"
fi

echo "Benchmark smoke run complete."
echo "Artifacts written to ${OUT_DIR}"
