import { apiGet, USE_MOCK_API } from './client';
import { mockFetchAnomalies, mockFetchAnomalyById } from '../mocks/anomalies';
import { applyFilters } from '../utils/filters';
import { resolveLatLon } from '../utils/location';
import type { Anomaly, Filters, Severity } from '../types/anomaly';

interface SearchServiceAnomaly {
  id: string;
  type?: string;
  location?: string;
  eventCount?: number;
  windowStart?: string;
  windowEnd?: string;
  detectedAt?: string;
  rule?: string;
  severity?: string;
  alertState?: string;
  description?: string;
}

interface SearchServiceAnomalyPage {
  items?: SearchServiceAnomaly[];
  content?: SearchServiceAnomaly[];
}

const KNOWN_TYPES = new Set(['INCIDENT', 'ALERT', 'CRIME']);
const BBOX_FILTER_ENABLED = (import.meta.env.VITE_FILTER_BY_BBOX || 'false').toLowerCase() === 'true';

function normalizeSeverity(input: string | undefined): Severity {
  const value = (input || '').toUpperCase();
  if (value === 'CRITICAL' || value === 'HIGH' || value === 'MEDIUM' || value === 'LOW') {
    return value;
  }
  return 'MEDIUM';
}

function normalizeType(type: string | undefined): string {
  const normalizedType = (type || '').trim().toUpperCase();
  if (KNOWN_TYPES.has(normalizedType)) {
    return normalizedType;
  }
  return 'INCIDENT';
}

function normalizeStatus(input: string | undefined): Anomaly['status'] {
  const value = (input || '').toUpperCase();
  if (value === 'OPEN' || value === 'ONGOING' || value === 'CLOSED') {
    return value;
  }

  return 'OPEN';
}

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

function extractAnomalyList(payload: SearchServiceAnomaly[] | SearchServiceAnomalyPage): SearchServiceAnomaly[] {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (payload && Array.isArray(payload.content)) {
    return payload.content;
  }

  if (payload && Array.isArray(payload.items)) {
    return payload.items;
  }

  return [];
}

function mapBackendAnomaly(item: SearchServiceAnomaly): Anomaly {
  const [lat, lon] = resolveLatLon(item.location, item.id);
  return {
    id: item.id,
    timestamp: normalizeTimestamp(item.detectedAt || item.windowEnd),
    lat,
    lon,
    type: normalizeType(item.type),
    severity: normalizeSeverity(item.severity),
    status: normalizeStatus(item.alertState),
    title: item.location || 'Anomaly',
    description: item.description,
    score: item.eventCount
  };
}

function resolveEffectiveFilters(filters: Filters): Filters {
  return BBOX_FILTER_ENABLED ? filters : { ...filters, bbox: undefined };
}

export async function fetchAnomalies(filters: Filters): Promise<Anomaly[]> {
  if (USE_MOCK_API) {
    return mockFetchAnomalies(filters);
  }

  const effectiveFilters = resolveEffectiveFilters(filters);
  const params = new URLSearchParams();
  if (effectiveFilters.from) params.set('from', effectiveFilters.from);
  if (effectiveFilters.to) params.set('to', effectiveFilters.to);
  if (effectiveFilters.severity.length) params.set('severity', effectiveFilters.severity.join(','));
  if (effectiveFilters.type.length) params.set('type', effectiveFilters.type.join(','));
  params.set('onlyActive', String(effectiveFilters.onlyActive));
  if (effectiveFilters.bbox) params.set('bbox', effectiveFilters.bbox.join(','));

  const query = params.toString();
  const data = await apiGet<SearchServiceAnomaly[] | SearchServiceAnomalyPage>(
    query ? `/anomalies?${query}` : '/anomalies'
  );
  return applyFilters(extractAnomalyList(data).map(mapBackendAnomaly), effectiveFilters);
}

export async function fetchAnomalyById(id: string): Promise<Anomaly | null> {
  if (USE_MOCK_API) {
    return mockFetchAnomalyById(id);
  }

  try {
    const item = await apiGet<SearchServiceAnomaly>(`/anomalies/${id}`);
    return mapBackendAnomaly(item);
  } catch {
    const all = await apiGet<SearchServiceAnomaly[] | SearchServiceAnomalyPage>('/anomalies');
    const candidate = extractAnomalyList(all).find((item) => item.id === id);
    return candidate ? mapBackendAnomaly(candidate) : null;
  }
}
