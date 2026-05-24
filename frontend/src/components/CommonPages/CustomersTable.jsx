import { useEffect, useMemo, useState } from 'react';
import { getAllCustomers } from '../../services/customer.service';
import { formatMoney } from '../../utils/formatters.jsx';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';

/**
 * Shared customers view. Table with:
 *  - free-text search (id, name, email, account number)
 *  - account-type filter
 *  - bank-name filter (populated from the data so it stays current)
 */
const CustomersTable = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [search, setSearch] = useState('');
  const [accountType, setAccountType] = useState('');
  const [bank, setBank] = useState('');

  useEffect(() => {
    (async () => {
      const result = await getAllCustomers();
      if (result.success) setRows(Array.isArray(result.data) ? result.data : []);
      else setError(result.error);
      setLoading(false);
    })();
  }, []);

  // Distinct values for the dropdown filters
  const banks = useMemo(() => [...new Set(rows.map((c) => c.bankName).filter(Boolean))], [rows]);
  const accountTypes = useMemo(() => [...new Set(rows.map((c) => c.accountType).filter(Boolean))], [rows]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return rows.filter((c) => {
      if (accountType && c.accountType !== accountType) return false;
      if (bank && c.bankName !== bank) return false;
      if (q) {
        const hay = `${c.customerId} ${c.name || ''} ${c.email || ''} ${c.accountNo || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [rows, search, accountType, bank]);

  if (loading) return <Loader label="Loading customers…" />;

  return (
    <>
      <PageHeader title="Customers" subtitle={`${filtered.length} of ${rows.length} shown`} />

      <div className="card mb-4 grid grid-cols-1 md:grid-cols-3 gap-3">
        <input
          className="input"
          placeholder="Search id, name, email, account…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select className="input" value={accountType} onChange={(e) => setAccountType(e.target.value)}>
          <option value="">All account types</option>
          {accountTypes.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <select className="input" value={bank} onChange={(e) => setBank(e.target.value)}>
          <option value="">All banks</option>
          {banks.map((b) => <option key={b} value={b}>{b}</option>)}
        </select>
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
              <th>Name</th>
              <th>Email</th>
              <th>Account #</th>
              <th>Bank</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Risk</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={8} className="text-center text-muted py-8">No customers match.</td></tr>
            ) : filtered.map((c) => (
              <tr key={c.customerId}>
                <td className="font-mono text-xs">{c.customerId}</td>
                <td>{c.name}</td>
                <td className="text-xs">{c.email}</td>
                <td className="font-mono text-xs">{c.accountNo}</td>
                <td>{c.bankName}</td>
                <td>{c.accountType}</td>
                <td>{formatMoney(c.balance)}</td>
                <td>{c.riskscore?.toFixed?.(1) ?? c.riskScore?.toFixed?.(1) ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default CustomersTable;
