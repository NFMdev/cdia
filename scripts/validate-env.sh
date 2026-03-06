#!/usr/bin/env bash
set -euo pipefail

UNSAFE_DEMO_MODE="${UNSAFE_DEMO_MODE:-false}"

if [[ "${UNSAFE_DEMO_MODE}" == "true" ]]; then
  echo "⚠️  UNSAFE_DEMO_MODE=true: skipping strict secret validation."
  exit 0
fi

declare -A defaults=(
  [POSTGRES_SUPERPASS]="change_me_superpass"
  [CDIA_APP_DB_PASSWORD]="change_me_app"
  [CDIA_MIGRATIONS_DB_PASSWORD]="change_me_migrations"
  [CDIA_METRICS_DB_PASSWORD]="change_me_metrics"
  [ELASTIC_PASSWORD]="test"
  [GRAFANA_ADMIN_PASSWORD]="admin"
)

invalid=0

for key in "${!defaults[@]}"; do
  current="${!key:-${defaults[$key]}}"
  if [[ "${current}" == "${defaults[$key]}" ]]; then
    echo "❌ ${key} is using insecure default value '${defaults[$key]}'."
    invalid=1
  fi
done

if [[ "${invalid}" -eq 1 ]]; then
  echo "Set non-default secrets or run with UNSAFE_DEMO_MODE=true for local demo mode."
  exit 1
fi

echo "✅ Environment validation passed."
