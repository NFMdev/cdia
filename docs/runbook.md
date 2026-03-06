# CDIA Runbook

## Standard Startup
1. Ensure `.env` is present with non-default credentials.
2. Start stack:
   - `bash ./scripts/compose-up.sh`
3. Wait for dependencies:
   - `bash ./scripts/wait-for-services.sh`
4. Submit Flink job:
   - `docker exec flink-jobmanager flink run -m flink-jobmanager:8081 /opt/flink/usrlib/processing-service.jar -c com.github.NFMdev.cdia.processing_service.flink.job.AnomalyJob`

## Unsafe Demo Mode
Use only for local demo speed, never for shared environments:
- `UNSAFE_DEMO_MODE=true bash ./scripts/compose-up.sh`

This mode relaxes secret validation and exposes ports broadly.

## Failure Modes
### Ingestion returns `400` for create event
- Confirm request includes required fields: `type`, `description`, `location`, `createdAt`, `sourceSystem`.
- Check response `details` object for failing field.

### Search indexing lag
- Check ingestion logs for retry/dead-letter messages.
- Verify metric `cdia_search_indexing_dead_letter_total`.
- Validate search endpoint availability at `/search/internal/index/events`.

### Flink anomaly stream empty
- Confirm Flink job is running in JobManager UI (`:8081`).
- Check CDC connectivity (`POSTGRES_SLOT_NAME`, `POSTGRES_STARTUP_MODE`).
- Check Elasticsearch sink credentials (`ES_HOSTS`, `ES_USERNAME`, `ES_PASSWORD`).

### SSE reconnect storms
- Ensure clients send `Last-Event-ID` (EventSource does this automatically after server event IDs).
- Inspect search-service logs for repeated emitter send failures.

## Operational Checks
- Ingestion health: `GET /actuator/health`
- Search health: `GET /search/actuator/health`
- Reports health: `GET /reports/actuator/health`
- Prometheus targets: `http://localhost/prometheus/targets`

## Recovery
- Restart only Flink:
  - `docker compose restart flink-jobmanager flink-taskmanager`
- Full reset:
  - `docker compose down -v`
  - `bash ./scripts/compose-up.sh`
