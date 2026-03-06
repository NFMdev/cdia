interface SimpleStatProps {
  label: string;
  value: string;
}

export function SimpleStat({ label, value }: SimpleStatProps) {
  return (
    <div className="rounded-lg border border-slate-200 p-4">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold">{value}</p>
    </div>
  );
}
