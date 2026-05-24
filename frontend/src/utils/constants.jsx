export const API_BASE_URL = '';

export const ROLES = {
  SUPER_ADMIN:   'SUPER_ADMIN',
  FRAUD_ANALYST: 'FRAUD_ANALYST',
  RISK_MANAGER:  'RISK_MANAGER'
};

// Friendly labels for the role picker on login/signup
export const ROLE_LABEL = {
  SUPER_ADMIN:   'Super Admin',
  FRAUD_ANALYST: 'Fraud Analyst',
  RISK_MANAGER:  'Risk Manager'
};

export const TRANSACTION_STATUSES = ['pending', 'genuine', 'flagged', 'terminated'];

// Colors for the status badge in the transactions table (light theme).
export const STATUS_BADGE = {
  genuine:    'bg-emerald-50 text-emerald-700 border-emerald-200',
  flagged:    'bg-red-50      text-red-700      border-red-200',
  terminated: 'bg-slate-100   text-slate-700    border-slate-300',
  pending:    'bg-amber-50    text-amber-700    border-amber-200'
};

export const STORAGE_KEYS = {
  TOKEN:    'token',
  ROLE:     'role',
  USERNAME: 'username'
};
