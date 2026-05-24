import { useAuth } from '../../context/AuthContext';
import { ROLE_LABEL } from '../../utils/constants.jsx';

// Inline SVG so we don't depend on an icon lib that might not be installed.
const MenuIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <line x1="3" y1="6"  x2="21" y2="6" />
    <line x1="3" y1="12" x2="21" y2="12" />
    <line x1="3" y1="18" x2="21" y2="18" />
  </svg>
);

const ShieldIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
);

/**
 * Navy banking-style header. On mobile the burger button on the left opens
 * the sidebar drawer (the parent owns that state).
 */
const Topbar = ({ onMenuClick }) => {
  const { username, role, logout } = useAuth();

  return (
    <header className="navbar h-16 px-4 md:px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="md:hidden p-2 rounded-md hover:bg-white/10 transition"
          aria-label="Open menu"
        >
          <MenuIcon />
        </button>

        <div className="flex items-center gap-2.5 select-none">
          <span className="flex items-center justify-center w-9 h-9 rounded-md bg-white/10 ring-1 ring-white/15">
            <ShieldIcon />
          </span>
          <div className="leading-tight">
            <div className="font-display text-lg md:text-xl tracking-tight">BankGuard</div>
            <div className="hidden md:block text-[10px] uppercase tracking-[0.18em] nav-muted">
              Fraud &amp; Risk Console
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-3 md:gap-4">
        <div className="hidden sm:flex flex-col items-end leading-tight">
          <span className="text-sm font-semibold">{username}</span>
          <span className="text-[11px] nav-muted">{ROLE_LABEL[role]}</span>
        </div>

        <button onClick={logout} className="btn-navbar-ghost">Logout</button>
      </div>
    </header>
  );
};

export default Topbar;
