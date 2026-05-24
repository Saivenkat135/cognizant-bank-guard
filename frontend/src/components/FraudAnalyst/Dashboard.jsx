import { useEffect, useMemo, useState } from 'react';
import { getAllTransactions } from '../../services/transaction.service';
import { formatMoney, dayKey } from '../../utils/formatters.jsx';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';
import StatCard from './StatCard';
import StatusPie from './StatusPie';
import BarChart from './BarChart';

/**
 * Fraud analyst dashboard. Pulls every transaction once, then aggregates
 * the data in-memory for KPI cards and the two charts.
 *
 * Aggregations:
 *   - count by status     -> pie chart
 *   - count by day (last 14 days) -> bar chart
 *   - count by customerId (top 5)  -> bar chart
 */
const Dashboard = () => {
  const [txns, setTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      const r = await getAllTransactions();
      if (r.success) setTxns(Array.isArray(r.data) ? r.data : []);
      else setError(r.error);
      setLoading(false);
    })();
  }, []);

  const stats = useMemo(() => {
    const total = txns.length;
    const flagged = txns.filter((t) => t.status === 'flagged').length;
    const genuine = txns.filter((t) => t.status === 'genuine').length;
    const pending = txns.filter((t) => t.status === 'pending').length;
    const sum = txns.reduce((acc, t) => acc + (t.amount || 0), 0);
    return { total, flagged, genuine, pending, sum };
  }, [txns]);

  // status -> count
  const statusBreakdown = useMemo(() => {
    const m = {};
    txns.forEach((t) => {
      const k = (t.status || 'unknown').toLowerCase();
      m[k] = (m[k] || 0) + 1;
    });
    return Object.entries(m).map(([label, value]) => ({ label, value }));
  }, [txns]);

  // last 14 distinct days, oldest -> newest
  const byDay = useMemo(() => {
    const m = {};
    txns.forEach((t) => {
      const k = dayKey(t.time);
      if (!k) return;
      m[k] = (m[k] || 0) + 1;
    });
    return Object.entries(m)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-14)
      .map(([label, value]) => ({ label: label.slice(5), value })); // MM-DD only
  }, [txns]);

  // top 5 customers by txn count
  const topCustomers = useMemo(() => {
    const m = {};
    txns.forEach((t) => {
      if (!t.customerId) return;
      m[t.customerId] = (m[t.customerId] || 0) + 1;
    });
    return Object.entries(m)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5)
      .map(([label, value]) => ({ label: `#${label}`, value }));
  }, [txns]);

  if (loading) return <Loader label="Loading dashboard…" />;

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Fraud analyst overview" />

      {error && (
        <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2 mb-3">
          {error}
        </div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard label="Total Txns" value={stats.total}             tone="blue" />
        <StatCard label="Flagged"    value={stats.flagged}           tone="red" />
        <StatCard label="Pending"    value={stats.pending}           tone="amber" />
        <StatCard label="Volume"     value={formatMoney(stats.sum)}  tone="slate" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        <div className="card">
          <h3 className="text-sm text-muted mb-3 uppercase tracking-wide">By status</h3>
          <StatusPie data={statusBreakdown} />
        </div>
        <div className="card">
          <h3 className="text-sm text-muted mb-3 uppercase tracking-wide">Last 14 days</h3>
          <BarChart data={byDay} />
        </div>
      </div>

      <div className="card">
        <h3 className="text-sm text-muted mb-3 uppercase tracking-wide">Top customers (by txn count)</h3>
        <BarChart data={topCustomers} />
      </div>
    </>
  );
};

export default Dashboard;
