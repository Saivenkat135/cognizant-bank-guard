import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ROLES } from './utils/constants.jsx';

// Shared
import Layout          from './components/Shared/Layout';
import ProtectedRoute  from './components/Shared/ProtectedRoute';

// Auth
import Login  from './components/Auth/Login';
import Signup from './components/Auth/Signup';

// SuperAdmin
import Users from './components/SuperAdmin/Users';

// RiskManager
import Rules from './components/RiskManager/Rules';

// FraudAnalyst
import Dashboard from './components/FraudAnalyst/Dashboard';
import Reports   from './components/FraudAnalyst/Reports';

// CommonPages — reused across roles
import TransactionsTable from './components/CommonPages/TransactionsTable';
import CustomersTable    from './components/CommonPages/CustomersTable';

// "/" — send the user home based on their role, or to /login if not signed in
const RoleHome = () => {
  const { isLoggedIn, role } = useAuth();
  if (!isLoggedIn) return <Navigate to="/login" replace />;
  if (role === ROLES.SUPER_ADMIN)   return <Navigate to="/admin/users"        replace />;
  if (role === ROLES.RISK_MANAGER)  return <Navigate to="/risk/transactions"  replace />;
  if (role === ROLES.FRAUD_ANALYST) return <Navigate to="/fraud/dashboard"    replace />;
  return <Navigate to="/login" replace />;
};

const Unauthorized = () => (
  <div className="min-h-screen flex items-center justify-center bg-app">
    <div className="text-center">
      <h1 className="text-5xl font-bold text-red-700 mb-3">403</h1>
      <p className="text-muted">You don't have permission to view that page.</p>
    </div>
  </div>
);

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public */}
          <Route path="/login"  element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/" element={<RoleHome />} />

          {/* SuperAdmin */}
          <Route
            element={
              <ProtectedRoute allowedRoles={[ROLES.SUPER_ADMIN]}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/admin/users"        element={<Users />} />
            <Route path="/admin/transactions" element={<TransactionsTable />} />
            <Route path="/admin/customers"    element={<CustomersTable />} />
          </Route>

          {/* RiskManager pages — SUPER_ADMIN can also enter */}
          <Route
            element={
              <ProtectedRoute allowedRoles={[ROLES.RISK_MANAGER, ROLES.SUPER_ADMIN]}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/risk/transactions" element={<TransactionsTable />} />
            <Route path="/risk/customers"    element={<CustomersTable />} />
            <Route path="/risk/rules"        element={<Rules />} />
          </Route>

          {/* FraudAnalyst pages — SUPER_ADMIN can also enter */}
          <Route
            element={
              <ProtectedRoute allowedRoles={[ROLES.FRAUD_ANALYST, ROLES.SUPER_ADMIN]}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/fraud/dashboard"    element={<Dashboard />} />
            <Route path="/fraud/transactions" element={<TransactionsTable />} />
            <Route path="/fraud/customers"    element={<CustomersTable />} />
            <Route path="/fraud/reports"      element={<Reports />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
