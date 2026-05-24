/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from 'react';
import {
  login as loginApi,
  logout as logoutApi,
  isAuthenticated,
  getRole,
  getUsername
} from '../services/auth.service';

const AuthContext = createContext(null);

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
};

export const AuthProvider = ({ children }) => {
  // Seed state from localStorage so a refresh doesn't bounce to /login
  const [isLoggedIn, setIsLoggedIn] = useState(isAuthenticated());
  const [role, setRole] = useState(getRole());
  const [username, setUsername] = useState(getUsername());

  const login = async (credentials) => {
    const result = await loginApi(credentials);
    if (result.success) {
      setIsLoggedIn(true);
      setRole(result.data.role);
      setUsername(result.data.username);
    }
    return result;
  };

  const logout = () => {
    setIsLoggedIn(false);
    setRole(null);
    setUsername(null);
    logoutApi();
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, role, username, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
