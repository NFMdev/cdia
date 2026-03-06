import { useMemo } from 'react';
import { useAnomalies, useLiveAnomalies } from '../hooks/useAnomalies';
import { useUiStore } from '../store/uiStore';
import { AnomalyMap } from '../features/map/AnomalyMap';
import { FilterPanel } from '../features/map/FilterPanel';
import { AnomalyList } from '../features/map/AnomalyList';
import { AnomalyDetailCard } from '../features/map/AnomalyDetailCard';
import { SimpleStat } from '../components/SimpleStat';

export function MapPage() {
  const { data = [], isLoading, isError } = useAnomalies();
  useLiveAnomalies();

  const selectedAnomaly = useUiStore((state) => state.selectedAnomaly);
  const setSelectedAnomaly = useUiStore((state) => state.setSelectedAnomaly);
  const sidePanelOpen = useUiStore((state) => state.sidePanelOpen);
  const setSidePanelOpen = useUiStore((state) => state.setSidePanelOpen);

  const stats = useMemo(() => {
    const total = data.length;
    const critical = data.filter((item) => item.severity === 'CRITICAL').length;
    const high = data.filter((item) => item.severity === 'HIGH').length;
    return { total, critical, high };
  }, [data]);

  return (
    <div className="grid h-full gap-4 p-4 md:grid-cols-[340px_minmax(0,1fr)] md:p-6">
      <div className={`${sidePanelOpen ? 'block' : 'hidden'} space-y-4 md:block`}>
        <FilterPanel />
        <AnomalyList items={data} selectedId={selectedAnomaly?.id} onSelect={setSelectedAnomaly} />
      </div>

      <div className="space-y-4">
        <button
          type="button"
          className="rounded-md border border-slate-200 bg-white px-3 py-1 text-sm md:hidden"
          onClick={() => setSidePanelOpen(!sidePanelOpen)}
        >
          {sidePanelOpen ? 'Hide filters' : 'Show filters'}
        </button>

        <section className="grid gap-3 sm:grid-cols-3">
          <SimpleStat label="Visible anomalies" value={String(stats.total)} />
          <SimpleStat label="Critical" value={String(stats.critical)} />
          <SimpleStat label="High" value={String(stats.high)} />
        </section>

        {isLoading && <p className="text-sm text-slate-600">Loading anomalies...</p>}
        {isError && <p className="text-sm text-red-700">Could not load anomalies.</p>}

        <AnomalyMap anomalies={data} selectedAnomaly={selectedAnomaly} onSelect={setSelectedAnomaly} />

        {selectedAnomaly && <AnomalyDetailCard anomaly={selectedAnomaly} />}
      </div>
    </div>
  );
}
