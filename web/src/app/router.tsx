import { createBrowserRouter } from 'react-router-dom';
import { AppShell } from '../components/AppShell';
import { AlertsPage } from '../routes/AlertsPage';
import { DashboardsPage } from '../routes/DashboardsPage';
import { DocsPage } from '../routes/DocsPage';
import { MapPage } from '../routes/MapPage';
import { NotFoundPage } from '../routes/NotFoundPage';
import { ToolsPage } from '../routes/ToolsPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <MapPage /> },
      { path: '/dashboards', element: <DashboardsPage /> },
      { path: '/alerts', element: <AlertsPage /> },
      { path: '/tools', element: <ToolsPage /> },
      { path: '/docs', element: <DocsPage /> }
    ],
    errorElement: <NotFoundPage />
  }
]);
