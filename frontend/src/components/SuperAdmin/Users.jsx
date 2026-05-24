import { useEffect, useMemo, useState } from 'react';
import { listUsers, approveUser, declineUser } from '../../services/auth.service';
import { ROLES, ROLE_LABEL } from '../../utils/constants.jsx';
import PageHeader from '../Shared/PageHeader';
import Loader from '../Shared/Loader';

/**
 * SuperAdmin sees every account in one table. Pending FraudAnalyst / RiskManager
 * signups get Approve / Decline buttons; already-approved ones are read-only.
 */
const Users = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState(''); // '', 'pending', 'approved'
  const [roleFilter, setRoleFilter] = useState('');

  const reload = async () => {
    setLoading(true);
    const result = await listUsers();
    if (result.success) setUsers(Array.isArray(result.data) ? result.data : []);
    else setError(result.error);
    setLoading(false);
  };

  useEffect(() => { reload(); }, []);

  const handleApprove = async (id) => {
    const r = await approveUser(id);
    if (r.success) reload();
    else setError(r.error);
  };

  const handleDecline = async (id) => {
    if (!confirm('Decline and remove this signup?')) return;
    const r = await declineUser(id);
    if (r.success) reload();
    else setError(r.error);
  };

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return users.filter((u) => {
      if (statusFilter === 'pending'  && u.isApproved) return false;
      if (statusFilter === 'approved' && !u.isApproved) return false;
      if (roleFilter && u.role !== roleFilter) return false;
      if (q) {
        const hay = `${u.id} ${u.username || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [users, search, statusFilter, roleFilter]);

  if (loading) return <Loader label="Loading users…" />;

  return (
    <>
      <PageHeader title="Users" subtitle="Approve or decline new analyst / manager signups" />

      <div className="card mb-4 grid grid-cols-1 md:grid-cols-3 gap-3">
        <input
          className="input"
          placeholder="Search id or username…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          <option value="pending">Pending</option>
          <option value="approved">Approved</option>
        </select>
        <select className="input" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
          <option value="">All roles</option>
          <option value={ROLES.SUPER_ADMIN}>Super Admin</option>
          <option value={ROLES.FRAUD_ANALYST}>Fraud Analyst</option>
          <option value={ROLES.RISK_MANAGER}>Risk Manager</option>
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
              <th>Username</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={5} className="text-center text-muted py-8">No users match.</td></tr>
            ) : filtered.map((u) => (
              <tr key={u.id}>
                <td className="font-mono text-xs">{u.id}</td>
                <td>{u.username}</td>
                <td>{ROLE_LABEL[u.role] || u.role}</td>
                <td>
                  {u.isApproved
                    ? <span className="badge bg-emerald-50 text-emerald-700 border-emerald-200">approved</span>
                    : <span className="badge bg-amber-50 text-amber-700 border-amber-200">pending</span>
                  }
                </td>
                <td>
                  {u.role === ROLES.SUPER_ADMIN ? (
                    <span className="text-xs text-muted">—</span>
                  ) : u.isApproved ? (
                    <button onClick={() => handleDecline(u.id)} className="btn-ghost text-xs text-red-700">Remove</button>
                  ) : (
                    <div className="flex gap-2">
                      <button onClick={() => handleApprove(u.id)} className="btn-primary text-xs py-1 px-3">Approve</button>
                      <button onClick={() => handleDecline(u.id)} className="btn-ghost text-xs text-red-700">Decline</button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};

export default Users;
