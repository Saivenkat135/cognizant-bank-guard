import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ROLES, ROLE_LABEL } from '../../utils/constants.jsx';

const MENU = {
  [ROLES.SUPER_ADMIN]: [
    { to: '/admin/users',        label: 'Users' },
    { to: '/fraud/dashboard',    label: 'Dashboard' },
    { to: '/admin/transactions', label: 'Transactions' },
    { to: '/admin/customers',    label: 'Customers' },
    { to: '/risk/rules',         label: 'Rules' },
    { to: '/fraud/reports',      label: 'Reports' }
  ],
  [ROLES.RISK_MANAGER]: [
    { to: '/risk/transactions', label: 'Transactions' },
    { to: '/risk/customers',    label: 'Customers' },
    { to: '/risk/rules',        label: 'Rules' }
  ],
  [ROLES.FRAUD_ANALYST]: [
    { to: '/fraud/dashboard',    label: 'Dashboard' },
    { to: '/fraud/transactions', label: 'Transactions' },
    { to: '/fraud/customers',    label: 'Customers' },
    { to: '/fraud/reports',      label: 'Reports' }
  ]
};

const CloseIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18" />
    <line x1="6"  y1="6" x2="18" y2="18" />
  </svg>
);

/**
 * Responsive sidebar.
 *  - md+:  always visible, sits in normal flow.
 *  - <md:  fixed off-canvas drawer; slides in when `isOpen` is true.
 */
const Sidebar = ({ isOpen, onClose }) => {
  const { role } = useAuth();
  const items = MENU[role] || [];

  // Translate is for mobile only; md: resets it.
  const slideClass = isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0';

  return (
    <aside
      className={`
        sidebar-slide
        fixed md:static top-16 md:top-0 left-0 bottom-0
        z-40 w-64 shrink-0
        bg-surface border-r border-app flex flex-col
        ${slideClass}
      `}
    >
      <div className="flex items-center justify-between px-5 py-4 border-b border-app">
        <div>
          <div className="text-xs font-semibold text-muted uppercase tracking-wider">Signed in as</div>
          <div className="text-sm font-semibold text-slate-900 mt-0.5">{ROLE_LABEL[role] || 'User'}</div>
        </div>
        <button
          onClick={onClose}
          className="md:hidden p-1.5 rounded hover:bg-surface-2 text-slate-600"
          aria-label="Close menu"
        >
          <CloseIcon />
        </button>
      </div>

      <nav className="flex-1 overflow-y-auto p-3 space-y-0.5">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onClose}
            className={({ isActive }) =>
              `relative block pl-5 pr-3.5 py-2.5 rounded-md text-sm transition-colors font-medium ${
                isActive
                  ? 'bg-primary-soft text-primary'
                  : 'text-slate-700 hover:bg-surface-2 hover:text-slate-900'
              }`
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <span
                    className="absolute left-1.5 top-2 bottom-2 w-1 rounded-full bg-[var(--primary)]"
                    aria-hidden="true"
                  />
                )}
                {item.label}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="px-5 py-4 border-t border-app text-xs text-muted">
        BankGuard · v1.0
      </div>
    </aside>
  );
};

export default Sidebar;
