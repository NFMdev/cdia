import type { ReactNode } from 'react';

interface PagePlaceholderProps {
  title: string;
  description: string;
  children?: ReactNode;
}

export function PagePlaceholder({ title, description, children }: PagePlaceholderProps) {
  return (
    <section className="p-4 md:p-6">
      <header className="mb-4 rounded-xl bg-panel p-5 shadow-sm">
        <h2 className="text-2xl font-semibold">{title}</h2>
        <p className="mt-1 text-sm text-slate-600">{description}</p>
      </header>
      <div className="rounded-xl bg-panel p-5 shadow-sm">{children}</div>
    </section>
  );
}
