export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Anomaly {
  id: string;
  timestamp: string;
  lat: number;
  lon: number;
  type: string;
  severity: Severity;
  status: 'OPEN' | 'ONGOING' | 'CLOSED';
  title?: string;
  description?: string;
  score?: number;
}

export interface Filters {
  severity: Severity[];
  type: string[];
  from: string;
  to: string;
  onlyActive: boolean;
  bbox?: [number, number, number, number];
}
