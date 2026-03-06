import { NavLink, Outlet } from 'react-router-dom';
import clsx from 'clsx';

const navItems = [
  { to: '/', label: 'Map' },
  { to: '/dashboards', label: 'Dashboards' },
  { to: '/alerts', label: 'Alerts' },
  { to: '/tools', label: 'Tools' },
  { to: '/docs', label: 'Docs' }
];

export function AppShell() {
  return (
    <div className="flex min-h-screen flex-col bg-canvas text-ink md:flex-row">
      <aside className="border-b border-slate-200 bg-panel md:w-56 md:border-b-0 md:border-r">
        <div className="px-5 py-4">
          <h1 className="text-xl font-semibold tracking-tight">CDIA</h1>
          <p className="text-xs text-slate-500">Crime Data Intelligence</p>
        </div>
        <nav className="grid grid-cols-2 gap-1 px-2 pb-3 md:grid-cols-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                clsx(
                  'rounded-md px-3 py-2 text-sm font-medium transition',
                  isActive ? 'bg-brand text-white' : 'text-slate-700 hover:bg-slate-100'
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
