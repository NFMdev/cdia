import type { ChangeEvent } from 'react';
import { useUiStore } from '../../store/uiStore';
import type { Severity } from '../../types/anomaly';

const severityOptions: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const typeOptions = ['INCIDENT', 'ALERT', 'CRIME'];

function toggleListValue<T extends string>(value: T, list: T[]): T[] {
  if (list.includes(value)) {
    return list.filter((item) => item !== value);
  }
  return [...list, value];
}

export function FilterPanel() {
  const filters = useUiStore((state) => state.filters);
  const liveEnabled = useUiStore((state) => state.liveEnabled);
  const setFilters = useUiStore((state) => state.setFilters);
  const setLiveEnabled = useUiStore((state) => state.setLiveEnabled);

  const onTimeChange = (event: ChangeEvent<HTMLInputElement>, key: 'from' | 'to') => {
    const value = event.target.value;
    if (!value) {
      setFilters({ [key]: '' });
      return;
    }

    const parsed = new Date(value);
    if (!Number.isNaN(parsed.getTime())) {
      setFilters({ [key]: parsed.toISOString() });
    }
  };

  const toDateTimeLocal = (input: string): string => {
    const parsed = new Date(input);
    if (Number.isNaN(parsed.getTime())) {
      return '';
    }
    const offset = parsed.getTimezoneOffset() * 60_000;
    const local = new Date(parsed.getTime() - offset);
    return local.toISOString().slice(0, 16);
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-panel p-4">
      <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Filters</h3>

      <div className="mt-3 space-y-3">
        <div>
          <p className="mb-1 text-xs font-semibold text-slate-500">Severity</p>
          <div className="flex flex-wrap gap-2">
            {severityOptions.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setFilters({ severity: toggleListValue(item, filters.severity) })}
                className={`rounded-full border px-2 py-1 text-xs ${
                  filters.severity.includes(item)
                    ? 'border-brand bg-brand text-white'
                    : 'border-slate-200 text-slate-700 hover:bg-slate-100'
                }`}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div>
          <p className="mb-1 text-xs font-semibold text-slate-500">Type</p>
          <div className="flex flex-wrap gap-2">
            {typeOptions.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setFilters({ type: toggleListValue(item, filters.type) })}
                className={`rounded-full border px-2 py-1 text-xs ${
                  filters.type.includes(item)
                    ? 'border-accent bg-accent text-white'
                    : 'border-slate-200 text-slate-700 hover:bg-slate-100'
                }`}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-2">
          <label className="text-xs font-semibold text-slate-500" htmlFor="fromTime">
            From
          </label>
          <input
            id="fromTime"
            className="rounded-md border border-slate-200 px-2 py-1 text-sm"
            type="datetime-local"
            value={toDateTimeLocal(filters.from)}
            onChange={(event) => onTimeChange(event, 'from')}
          />

          <label className="text-xs font-semibold text-slate-500" htmlFor="toTime">
            To
          </label>
          <input
            id="toTime"
            className="rounded-md border border-slate-200 px-2 py-1 text-sm"
            type="datetime-local"
            value={toDateTimeLocal(filters.to)}
            onChange={(event) => onTimeChange(event, 'to')}
          />
        </div>

        <div className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-2">
          <label className="text-sm font-medium text-slate-700" htmlFor="liveToggle">
            Live
          </label>
          <input
            id="liveToggle"
            type="checkbox"
            className="h-4 w-4"
            checked={liveEnabled}
            onChange={(event) => setLiveEnabled(event.target.checked)}
          />
        </div>

        <div className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-2">
          <label className="text-sm font-medium text-slate-700" htmlFor="activeToggle">
            Only active
          </label>
          <input
            id="activeToggle"
            type="checkbox"
            className="h-4 w-4"
            checked={filters.onlyActive}
            onChange={(event) => setFilters({ onlyActive: event.target.checked })}
          />
        </div>
      </div>
    </section>
  );
}
