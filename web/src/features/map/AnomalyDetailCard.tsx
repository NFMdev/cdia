import { format } from 'date-fns';
import type { Anomaly } from '../../types/anomaly';

interface AnomalyDetailCardProps {
  anomaly: Anomaly;
}

export function AnomalyDetailCard({ anomaly }: AnomalyDetailCardProps) {
  return (
    <section className="rounded-xl border border-slate-200 bg-panel p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-lg font-semibold">{anomaly.title || anomaly.type}</h3>
          <p className="text-sm text-slate-600">{anomaly.description || 'No description provided.'}</p>
        </div>
        <span className="rounded bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">{anomaly.status}</span>
      </div>

      <dl className="mt-3 grid grid-cols-2 gap-2 text-sm">
        <div>
          <dt className="text-xs uppercase tracking-wide text-slate-500">Type</dt>
          <dd>{anomaly.type}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-wide text-slate-500">Severity</dt>
          <dd>{anomaly.severity}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-wide text-slate-500">Timestamp</dt>
          <dd>{format(new Date(anomaly.timestamp), 'PPpp')}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-wide text-slate-500">Score</dt>
          <dd>{anomaly.score ?? '-'}</dd>
        </div>
      </dl>
    </section>
  );
}
