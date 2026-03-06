import { useEffect, useMemo } from 'react';
import { CircleMarker, MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import type { Anomaly } from '../../types/anomaly';
import { ensureLeafletIcons } from './leafletSetup';
import { useUiStore } from '../../store/uiStore';
import { HeatLayer } from './HeatLayer';

interface AnomalyMapProps {
  anomalies: Anomaly[];
  selectedAnomaly: Anomaly | null;
  onSelect: (anomaly: Anomaly) => void;
}

function severityColor(severity: Anomaly['severity']): string {
  switch (severity) {
    case 'CRITICAL':
      return '#b91c1c';
    case 'HIGH':
      return '#dc2626';
    case 'MEDIUM':
      return '#d97706';
    default:
      return '#0f766e';
  }
}

function SelectionSync({ selectedAnomaly }: { selectedAnomaly: Anomaly | null }) {
  const map = useMap();

  useEffect(() => {
    if (!selectedAnomaly) {
      return;
    }

    map.setView([selectedAnomaly.lat, selectedAnomaly.lon], Math.max(12, map.getZoom()), { animate: true });
  }, [map, selectedAnomaly]);

  return null;
}

function BboxSync() {
  const setFilters = useUiStore((state) => state.setFilters);

  useMapEvents({
    moveend: (event) => {
      const bounds = event.target.getBounds();
      setFilters({
        bbox: [bounds.getWest(), bounds.getSouth(), bounds.getEast(), bounds.getNorth()]
      });
    }
  });

  return null;
}

export function AnomalyMap({ anomalies, selectedAnomaly, onSelect }: AnomalyMapProps) {
  ensureLeafletIcons();
  const heatEnabled = (import.meta.env.VITE_ENABLE_HEATMAP || 'true').toLowerCase() === 'true';
  const bboxFilterEnabled = (import.meta.env.VITE_FILTER_BY_BBOX || 'false').toLowerCase() === 'true';
  const setFilters = useUiStore((state) => state.setFilters);

  useEffect(() => {
    if (!bboxFilterEnabled) {
      setFilters({ bbox: undefined });
    }
  }, [bboxFilterEnabled, setFilters]);

  const criticalCount = useMemo(
    () => anomalies.filter((item) => item.severity === 'HIGH' || item.severity === 'CRITICAL').length,
    [anomalies]
  );

  return (
    <div className="relative h-full min-h-[440px] overflow-hidden rounded-xl border border-slate-200">
      <MapContainer
        center={[56.2, 10.2]}
        zoom={7}
        minZoom={5}
        className="h-full w-full"
        scrollWheelZoom
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <HeatLayer anomalies={anomalies} enabled={heatEnabled} />
        {bboxFilterEnabled ? <BboxSync /> : null}
        <SelectionSync selectedAnomaly={selectedAnomaly} />

        <MarkerClusterGroup chunkedLoading removeOutsideVisibleBounds={false}>
          {anomalies.map((anomaly) => (
            <Marker
              key={anomaly.id}
              position={[anomaly.lat, anomaly.lon]}
              eventHandlers={{
                click: () => onSelect(anomaly)
              }}
            >
              <Popup>
                <div className="text-sm">
                  <p className="font-semibold">{anomaly.title || anomaly.type}</p>
                  <p className="text-slate-600">{anomaly.description || 'No description'}</p>
                </div>
              </Popup>
            </Marker>
          ))}
        </MarkerClusterGroup>

        {selectedAnomaly && (
          <CircleMarker
            center={[selectedAnomaly.lat, selectedAnomaly.lon]}
            radius={12}
            pathOptions={{
              color: severityColor(selectedAnomaly.severity),
              weight: 2,
              fillOpacity: 0.15
            }}
          />
        )}
      </MapContainer>

      <div className="pointer-events-none absolute left-3 top-3 rounded-md bg-white/95 px-3 py-2 text-xs shadow">
        <p className="font-semibold text-slate-700">Live overview</p>
        <p className="text-slate-600">Critical + High: {criticalCount}</p>
      </div>
    </div>
  );
}
