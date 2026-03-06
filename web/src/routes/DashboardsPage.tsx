import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { PagePlaceholder } from '../components/PagePlaceholder';

const data = [
  { time: '08:00', anomalies: 4, resolved: 1 },
  { time: '10:00', anomalies: 7, resolved: 2 },
  { time: '12:00', anomalies: 12, resolved: 6 },
  { time: '14:00', anomalies: 10, resolved: 8 },
  { time: '16:00', anomalies: 14, resolved: 10 },
  { time: '18:00', anomalies: 9, resolved: 9 }
];

export function DashboardsPage() {
  return (
    <PagePlaceholder
      title="Dashboards"
      description="Placeholder dashboard route with sample chart widgets."
    >
      <div className="h-[320px] w-full">
        <ResponsiveContainer>
          <AreaChart data={data}>
            <defs>
              <linearGradient id="colorAnomaly" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#1d4ed8" stopOpacity={0.8} />
                <stop offset="95%" stopColor="#1d4ed8" stopOpacity={0.1} />
              </linearGradient>
              <linearGradient id="colorResolved" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#0f766e" stopOpacity={0.8} />
                <stop offset="95%" stopColor="#0f766e" stopOpacity={0.1} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Area type="monotone" dataKey="anomalies" stroke="#1d4ed8" fillOpacity={1} fill="url(#colorAnomaly)" />
            <Area type="monotone" dataKey="resolved" stroke="#0f766e" fillOpacity={1} fill="url(#colorResolved)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </PagePlaceholder>
  );
}
