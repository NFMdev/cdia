import { addMinutes, subHours } from 'date-fns';
import { applyFilters } from '../utils/filters';
import type { Anomaly, Filters, Severity } from '../types/anomaly';

const types = ['INCIDENT', 'ALERT', 'CRIME'];
const severities: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const baseData: Anomaly[] = Array.from({ length: 80 }).map((_, idx) => {
  const lat = 55.5 + Math.random() * 1.3;
  const lon = 9.2 + Math.random() * 3.5;
  const severity = severities[idx % severities.length];
  const type = types[idx % types.length];

  return {
    id: `mock-${idx + 1}`,
    timestamp: addMinutes(subHours(new Date(), 12), idx * 8).toISOString(),
    lat,
    lon,
    type,
    severity,
    status: idx % 4 === 0 ? 'ONGOING' : 'OPEN',
    title: `${type} near zone ${idx % 10}`,
    description: `Synthetic anomaly #${idx + 1}`,
    score: Math.round(40 + Math.random() * 60)
  };
});

export async function mockFetchAnomalies(filters: Filters): Promise<Anomaly[]> {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return applyFilters(baseData, filters).sort((a, b) => b.timestamp.localeCompare(a.timestamp));
}

export async function mockFetchAnomalyById(id: string): Promise<Anomaly | null> {
  await new Promise((resolve) => setTimeout(resolve, 100));
  return baseData.find((item) => item.id === id) ?? null;
}

export function mockAnomalyStream(onMessage: (anomaly: Anomaly) => void): () => void {
  const timer = window.setInterval(() => {
    const id = Math.floor(Math.random() * baseData.length);
    const seed = baseData[id];
    const liveEvent: Anomaly = {
      ...seed,
      id: `live-${Date.now()}`,
      timestamp: new Date().toISOString(),
      status: 'OPEN',
      score: Math.min(100, (seed.score ?? 50) + Math.round(Math.random() * 15))
    };
    onMessage(liveEvent);
  }, 6000);

  return () => window.clearInterval(timer);
}
