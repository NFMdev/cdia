import type { Anomaly, Filters } from '../types/anomaly';

export const DEFAULT_FILTERS: Filters = {
  severity: [],
  type: [],
  from: '',
  to: '',
  onlyActive: false
};

export function applyFilters(items: Anomaly[], filters: Filters): Anomaly[] {
  const fromTs = Date.parse(filters.from);
  const toTs = Date.parse(filters.to);
  const hasFrom = Number.isFinite(fromTs);
  const hasTo = Number.isFinite(toTs);

  return items.filter((item) => {
    if (filters.onlyActive && item.status === 'CLOSED') {
      return false;
    }

    if (filters.severity.length > 0 && !filters.severity.includes(item.severity)) {
      return false;
    }

    if (filters.type.length > 0 && !filters.type.includes(item.type)) {
      return false;
    }

    const itemTs = Date.parse(item.timestamp);
    if (!Number.isFinite(itemTs)) {
      return false;
    }

    if (hasFrom && itemTs < fromTs) {
      return false;
    }

    if (hasTo && itemTs > toTs) {
      return false;
    }

    if (filters.bbox) {
      const [minLon, minLat, maxLon, maxLat] = filters.bbox;
      if (item.lon < minLon || item.lon > maxLon || item.lat < minLat || item.lat > maxLat) {
        return false;
      }
    }

    return true;
  });
}
