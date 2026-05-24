import { useEffect, useMemo, useState } from 'react';
import { getAllTransactions } from '../../services/transaction.service';
import { formatMoney, formatDate, dayKey } from '../../utils/formatters.jsx';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';

/**
 * Reports view for the fraud analyst.
 *
 * Two pieces:
 *   1. "Daily summary" — one row per day with totals and flagged count.
 *   2. "Flagged transactions" — every flagged txn, optionally narrowed by date range.
 *
 * Everything is derived from the same /api/transactions feed; no extra endpoint.
 */
const Reports = () => {
  const [txns, setTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  useEffect(() => {
    (async () => {
      const r = await getAllTransactions();
      if (r.success) setTxns(Array.isArray(r.data) ? r.data : []);
      else setError(r.error);
      setLoading(false);
    })();
  }, []);

  // Apply date filter once for both reports below
  const inRange = useMemo(() => {
    return txns.filter((t) => {
      const k = dayKey(t.time);
      if (!k) return false;
      if (from && k < from) return false;
      if (to && k > to) return false;
      return true;
    });
  }, [txns, from, to]);

  const daily = useMemo(() => {
    const m = {};
    inRange.forEach((t) => {
      const k = dayKey(t.time);
      if (!m[k]) m[k] = { day: k, count: 0, flagged: 0, volume: 0 };
      m[k].count += 1;
      m[k].volume += t.amount || 0;
      if ((t.status || '').toLowerCase() === 'flagged') m[k].flagged += 1;
    });
    return Object.values(m).sort((a, b) => b.day.localeCompare(a.day));
  }, [inRange]);

  const flagged = useMemo(
    () => inRange.filter((t) => (t.status || '').toLowerCase() === 'flagged'),
    [inRange]
  );

  if (loading) return <Loader label="Loading reports…" />;

  return (
    <>
      <PageHeader title="Reports" subtitle="Daily summary and flagged transactions" />

      <div className="card mb-4 grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">From</label>
          <input type="date" className="input" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">To</label>
          <input type="date" className="input" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      {error && (
        <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2 mb-3">
          {error}
        </div>
      )}

      <h2 className="text-lg text-slate-900 font-semibold mb-2">Daily summary</h2>
      <div className="table-wrap mb-6">
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th>Transactions</th>
              <th>Flagged</th>
              <th>Volume</th>
            </tr>
          </thead>
          <tbody>
            {daily.length === 0 ? (
              <tr><td colSpan={4} className="text-center text-muted py-8">No data in range.</td></tr>
            ) : daily.map((d) => (
              <tr key={d.day}>
                <td>{d.day}</td>
                <td>{d.count}</td>
                <td className="text-red-700 font-medium">{d.flagged}</td>
                <td>{formatMoney(d.volume)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 className="text-lg text-slate-900 font-semibold mb-2">Flagged transactions</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Amount</th>
              <th>Risk</th>
              <th>Time</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {flagged.length === 0 ? (
              <tr><td colSpan={6} className="text-center text-muted py-8">No flagged transactions.</td></tr>
            ) : flagged.map((t) => (
              <tr key={t.transactionId}>
                <td className="font-mono text-xs">{t.transactionId}</td>
                <td>{t.customerId}</td>
                <td>{formatMoney(t.amount)}</td>
                <td>{t.riskScore?.toFixed?.(1) ?? '—'}</td>
                <td className="text-xs">{formatDate(t.time)}</td>
                <td className="text-xs">{t.reason || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default Reports;
