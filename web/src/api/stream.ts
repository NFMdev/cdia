import { API_BASE_URL, USE_MOCK_API } from './client';
import { mockAnomalyStream } from '../mocks/anomalies';
import { resolveLatLon } from '../utils/location';
import type { Anomaly } from '../types/anomaly';

type MessageHandler = (anomaly: Anomaly) => void;

interface BackendStreamEvent {
  id: string;
  type?: string;
  location?: string;
  detectedAt?: string;
  rule?: string;
  severity?: string;
  alertState?: string;
  description?: string;
  eventCount?: number;
}

const KNOWN_TYPES = new Set(['INCIDENT', 'ALERT', 'CRIME']);

function normalizeTimestamp(input: string | undefined): string {
  if (!input) {
    return new Date().toISOString();
  }

  const parsed = Date.parse(input);
  if (Number.isFinite(parsed)) {
    return new Date(parsed).toISOString();
  }

  // Keep a safe fallback for malformed legacy payloads.
  return new Date().toISOString();
}

function normalizeSeverity(input: string | undefined): Anomaly['severity'] {
  const value = (input || '').toUpperCase();
  if (value === 'CRITICAL' || value === 'HIGH' || value === 'MEDIUM' || value === 'LOW') {
    return value;
  }

  return 'MEDIUM';
}

function normalizeStatus(input: string | undefined): Anomaly['status'] {
  const value = (input || '').toUpperCase();
  if (value === 'OPEN' || value === 'OPENED') {
    return 'OPEN';
  }
  if (value === 'ONGOING') {
    return 'ONGOING';
  }
  if (value === 'CLOSED') {
    return 'CLOSED';
  }

  return 'OPEN';
}

function normalizeType(type: string | undefined): string {
  const normalizedType = (type || '').trim().toUpperCase();
  if (KNOWN_TYPES.has(normalizedType)) {
    return normalizedType;
  }
  return 'INCIDENT';
}

function mapEvent(payload: BackendStreamEvent): Anomaly {
  const [lat, lon] = resolveLatLon(payload.location, payload.id);
  return {
    id: payload.id,
    timestamp: normalizeTimestamp(payload.detectedAt),
    lat,
    lon,
    type: normalizeType(payload.type),
    severity: normalizeSeverity(payload.severity),
    status: normalizeStatus(payload.alertState),
    title: payload.location || 'Live anomaly',
    description: payload.description,
    score: payload.eventCount
  };
}

export function startAnomalyStream(onMessage: MessageHandler): () => void {
  if (USE_MOCK_API) {
    return mockAnomalyStream(onMessage);
  }

  let closed = false;
  let source: EventSource | null = null;
  let reconnectTimer: number | null = null;

  const connect = () => {
    source = new EventSource(`${API_BASE_URL}/anomalies/stream`);

    source.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as BackendStreamEvent;
        onMessage(mapEvent(data));
      } catch {
        // Ignore malformed event payload.
      }
    };

    source.onerror = () => {
      source?.close();
      if (!closed) {
        reconnectTimer = window.setTimeout(connect, 3000);
      }
    };
  };

  connect();

  return () => {
    closed = true;
    source?.close();
    if (reconnectTimer !== null) {
      window.clearTimeout(reconnectTimer);
    }
  };
}
