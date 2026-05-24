import { useEffect, useMemo, useState } from 'react';
import { getAllTransactions } from '../../services/transaction.service';
import { formatMoney, formatDate } from '../../utils/formatters.jsx';
import { TRANSACTION_STATUSES } from '../../utils/constants.jsx';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';
import StatusBadge from '../Shared/StatusBadge';

/**
 * Shared transactions view. Shows every transaction in a table with:
 *  - free-text search (id, customer id, receiver account)
 *  - status filter (pending/genuine/flagged/terminated)
 *  - min/max amount filters
 *
 * Used by SuperAdmin, RiskManager, FraudAnalyst — they share the same backing endpoint.
 */
const TransactionsTable = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filter state
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');

  useEffect(() => {
    (async () => {
      const result = await getAllTransactions();
      if (result.success) setRows(Array.isArray(result.data) ? result.data : []);
      else setError(result.error);
      setLoading(false);
    })();
  }, []);

  // Apply filters locally — the dataset is small enough to filter client-side.
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const min = minAmount === '' ? -Infinity : Number(minAmount);
    const max = maxAmount === '' ?  Infinity : Number(maxAmount);

    return rows.filter((t) => {
      if (status && (t.status || '').toLowerCase() !== status) return false;
      if (t.amount < min || t.amount > max) return false;
      if (q) {
        const hay = `${t.transactionId} ${t.customerId} ${t.receiverAccountNumber || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [rows, search, status, minAmount, maxAmount]);

  if (loading) return <Loader label="Loading transactions…" />;

  return (
    <>
      <PageHeader title="Transactions" subtitle={`${filtered.length} of ${rows.length} shown`} />

      <div className="card mb-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <input
          className="input"
          placeholder="Search id, customer, receiver…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {TRANSACTION_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <input
          className="input"
          placeholder="Min amount"
          type="number"
          value={minAmount}
          onChange={(e) => setMinAmount(e.target.value)}
        />
        <input
          className="input"
          placeholder="Max amount"
          type="number"
          value={maxAmount}
          onChange={(e) => setMaxAmount(e.target.value)}
        />
      </div>

      {error && (
        <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2 mb-3">
          {error}
        </div>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Receiver Acct</th>
              <th>Amount</th>
              <th>City</th>
              <th>Time</th>
              <th>Risk</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={8} className="text-center text-muted py-8">No transactions match.</td></tr>
            ) : filtered.map((t) => (
              <tr key={t.transactionId}>
                <td className="font-mono text-xs">{t.transactionId}</td>
                <td>{t.customerId}</td>
                <td className="font-mono text-xs">{t.receiverAccountNumber}</td>
                <td>{formatMoney(t.amount)}</td>
                <td>{t.city || '—'}</td>
                <td className="text-xs">{formatDate(t.time)}</td>
                <td>{t.riskScore?.toFixed?.(1) ?? '—'}</td>
                <td><StatusBadge status={t.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default TransactionsTable;
