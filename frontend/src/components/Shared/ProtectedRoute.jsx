import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

// Wrap any route that requires a logged-in user.
// Pass `allowedRoles` to additionally restrict to specific roles.
const ProtectedRoute = ({ children, allowedRoles }) => {
  const { isLoggedIn, role } = useAuth();

  if (!isLoggedIn) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(role)) return <Navigate to="/unauthorized" replace />;

  return children;
};

export default ProtectedRoute;
