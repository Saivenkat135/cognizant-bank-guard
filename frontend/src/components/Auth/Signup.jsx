import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../../services/auth.service';
import { ROLES } from '../../utils/constants.jsx';

// SuperAdmin cannot self-signup — only FraudAnalyst & RiskManager appear here.
const Signup = () => {
  const [role, setRole] = useState(ROLES.FRAUD_ANALYST);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    const result = await register({ username, password, role });

    setLoading(false);
    if (!result.success) {
      setError(result.error);
      return;
    }
    setSuccess('Signup submitted. SuperAdmin must approve your account before you can log in.');
    setTimeout(() => navigate('/login'), 2200);
  };

  return (
    <div className="min-h-screen grid md:grid-cols-2 bg-app">
      <div className="hidden md:flex flex-col justify-between p-12 text-white"
           style={{ background: 'linear-gradient(135deg, #0b2a5b 0%, #1e40af 100%)' }}>
        <div>
          <div className="font-display text-3xl tracking-tight">BankGuard</div>
          <div className="text-sm mt-1 text-white/70">Fraud &amp; Risk Operations Console</div>
        </div>
        <div className="space-y-4 max-w-sm">
          <p className="text-lg leading-snug font-medium">
            Request access for a Fraud Analyst or Risk Manager account.
          </p>
          <p className="text-sm text-white/70">
            New accounts are reviewed and approved by your Super Administrator before sign-in is enabled.
          </p>
        </div>
        <div className="text-xs text-white/50">© BankGuard · Internal use only</div>
      </div>

      <div className="flex items-center justify-center p-6 md:p-10">
      <div className="card w-full max-w-md">
        <div className="mb-6">
          <div className="md:hidden text-navy font-display text-2xl mb-1">BankGuard</div>
          <h1 className="text-xl font-semibold text-slate-900">Create account</h1>
          <p className="text-sm text-muted mt-1">For Fraud Analysts and Risk Managers.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-semibold text-slate-700 mb-1 block uppercase tracking-wide">Role</label>
            <select className="input" value={role} onChange={(e) => setRole(e.target.value)}>
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
              minLength={6}
              required
            />
          </div>

          {error && (
            <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2">
              {error}
            </div>
          )}
          {success && (
            <div className="text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-md px-3 py-2">
              {success}
            </div>
          )}

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Submitting…' : 'Sign up'}
          </button>
        </form>

        <p className="text-sm text-muted mt-5 text-center">
          Already have an account?{' '}
          <Link to="/login" className="text-primary hover:underline font-medium">Sign in</Link>
        </p>
      </div>
      </div>
    </div>
  );
};

export default Signup;
