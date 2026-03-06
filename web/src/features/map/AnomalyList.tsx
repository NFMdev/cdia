import { formatDistanceToNow } from 'date-fns';
import type { Anomaly } from '../../types/anomaly';

interface AnomalyListProps {
  items: Anomaly[];
  selectedId?: string;
  onSelect: (anomaly: Anomaly) => void;
}

function severityClass(value: Anomaly['severity']): string {
  switch (value) {
    case 'CRITICAL':
      return 'bg-red-100 text-red-800';
    case 'HIGH':
      return 'bg-orange-100 text-orange-800';
    case 'MEDIUM':
      return 'bg-amber-100 text-amber-800';
    default:
      return 'bg-emerald-100 text-emerald-800';
  }
}

export function AnomalyList({ items, selectedId, onSelect }: AnomalyListProps) {
  return (
    <section className="rounded-xl border border-slate-200 bg-panel p-4">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-600">Recent anomalies</h3>

      <div className="max-h-[340px] space-y-2 overflow-y-auto pr-1">
        {items.slice(0, 20).map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect(item)}
            className={`w-full rounded-lg border p-3 text-left transition ${
              item.id === selectedId ? 'border-brand bg-brand/5' : 'border-slate-200 hover:bg-slate-50'
            }`}
          >
            <div className="flex items-start justify-between gap-2">
              <p className="text-sm font-medium text-slate-800">{item.title || item.type}</p>
              <span className={`rounded px-2 py-0.5 text-[10px] font-semibold ${severityClass(item.severity)}`}>
                {item.severity}
              </span>
            </div>
            <p className="mt-1 max-h-8 overflow-hidden text-xs text-slate-600">{item.description || 'No description'}</p>
            <p className="mt-1 text-[11px] text-slate-500">
              {formatDistanceToNow(new Date(item.timestamp), { addSuffix: true })}
            </p>
          </button>
        ))}

        {items.length === 0 && <p className="text-sm text-slate-500">No anomalies match the current filters.</p>}
      </div>
    </section>
  );
}
