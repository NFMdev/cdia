#!/usr/bin/env bash
set -euo pipefail

UNSAFE_DEMO_MODE="${UNSAFE_DEMO_MODE:-false}"

if [[ "${UNSAFE_DEMO_MODE}" == "true" ]]; then
  export CDIA_BIND_HOST="0.0.0.0"
  echo "⚠️  UNSAFE_DEMO_MODE=true: exposing service ports on 0.0.0.0"
else
  export CDIA_BIND_HOST="${CDIA_BIND_HOST:-127.0.0.1}"
fi

bash ./scripts/validate-env.sh

docker compose --profile core --profile apps --profile streaming up -d "$@"
