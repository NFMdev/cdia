# CDIA Web Module

React + Vite + TypeScript frontend for CDIA.

## Features

- Map-first anomaly UI (Leaflet + clustering + optional heatmap)
- Route shell: Map, Dashboards, Alerts, Tools, Docs
- TanStack Query + Zustand state management
- Real API mode and mock API mode (`VITE_USE_MOCK_API`)
- Live updates via SSE with reconnect logic

## Local run (web only)

```bash
cd web
npm install
npm run dev
```

Optional env vars:

- `VITE_API_BASE_URL` (default: `/api`)
- `VITE_USE_MOCK_API` (`true` or `false`, default: `false`)
- `VITE_ENABLE_HEATMAP` (`true` or `false`, default: `true`)
- `VITE_FILTER_BY_BBOX` (`true` or `false`, default: `false`)

## Anomalies API used by frontend

- `GET /api/anomalies?from=...&to=...&severity=...&type=...&bbox=...`
- `GET /api/anomalies/:id`
- `GET /api/anomalies/stream` (SSE)

When running against current backend, `/api/anomalies*` is mapped by Nginx to `search-service` endpoints.
