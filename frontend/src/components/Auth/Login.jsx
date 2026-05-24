import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ROLES, ROLE_LABEL } from '../../utils/constants.jsx';

// Where each role lands after login
const HOME_FOR_ROLE = {
  [ROLES.SUPER_ADMIN]:   '/admin/users',
  [ROLES.RISK_MANAGER]:  '/risk/transactions',
  [ROLES.FRAUD_ANALYST]: '/fraud/dashboard'
};

const Login = () => {
  const [role, setRole] = useState(ROLES.SUPER_ADMIN);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const result = await login({ username, password });

    setLoading(false);
    if (!result.success) {
      setError(result.error);
      return;
    }

    // Server tells us the actual role; verify it matches what the user picked
    if (result.data.role !== role) {
      setError(`This account is a ${ROLE_LABEL[result.data.role]}, not ${ROLE_LABEL[role]}.`);
      return;
    }

    navigate(HOME_FOR_ROLE[result.data.role] || '/');
  };

  return (
    <div className="min-h-screen grid md:grid-cols-2 bg-app">
      {/* Marketing panel — hidden on mobile to keep the form front and centre */}
      <div className="hidden md:flex flex-col justify-between p-12 text-white"
           style={{ background: 'linear-gradient(135deg, #0b2a5b 0%, #1e40af 100%)' }}>
        <div>
          <div className="font-display text-3xl tracking-tight">BankGuard</div>
          <div className="text-sm mt-1 text-white/70">Fraud &amp; Risk Operations Console</div>
        </div>
        <div className="space-y-4 max-w-sm">
          <p className="text-lg leading-snug font-medium">
            Real-time monitoring, AI-assisted decisioning, and a unified operations view.
          </p>
          <p className="text-sm text-white/70">
            Sign in with the role assigned to your account by your administrator.
          </p>
        </div>
        <div className="text-xs text-white/50">© BankGuard · Internal use only</div>
      </div>

      <div className="flex items-center justify-center p-6 md:p-10">
      <div className="card w-full max-w-md">
        <div className="mb-6">
          <div className="md:hidden text-navy font-display text-2xl mb-1">BankGuard</div>
          <h1 className="text-xl font-semibold text-slate-900">Sign in</h1>
          <p className="text-sm text-muted mt-1">Enter your credentials to continue.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">Role</label>
            <select className="input" value={role} onChange={(e) => setRole(e.target.value)}>
              <option value={ROLES.SUPER_ADMIN}>Super Admin</option>
              <option value={ROLES.FRAUD_ANALYST}>Fraud Analyst</option>
              <option value={ROLES.RISK_MANAGER}>Risk Manager</option>
            </select>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">Username</label>
            <input
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">Password</label>
            <input
              type="password"
              className="input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && (
            <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2">
              {error}
            </div>
          )}

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="text-sm text-muted mt-5 text-center">
          New Fraud Analyst or Risk Manager?{' '}
          <Link to="/signup" className="text-primary hover:underline font-medium">Sign up</Link>
        </p>
      </div>
      </div>
    </div>
  );
};

export default Login;
