import { useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.heat';
import type { Anomaly } from '../../types/anomaly';

interface HeatLayerProps {
  anomalies: Anomaly[];
  enabled: boolean;
}

export function HeatLayer({ anomalies, enabled }: HeatLayerProps) {
  const map = useMap();

  useEffect(() => {
    if (!enabled || anomalies.length === 0) {
      return undefined;
    }

    const points = anomalies.map((item) => [item.lat, item.lon, Math.max(0.1, (item.score ?? 50) / 100)]);
    const layer = (L as typeof L & { heatLayer: (pts: number[][], options: Record<string, unknown>) => L.Layer })
      .heatLayer(points, {
        radius: 20,
        blur: 16,
        maxZoom: 15
      })
      .addTo(map);

    return () => {
      map.removeLayer(layer);
    };
  }, [anomalies, enabled, map]);

  return null;
}
