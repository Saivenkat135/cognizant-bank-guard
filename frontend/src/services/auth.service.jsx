import api from './api.config';
import { STORAGE_KEYS } from '../utils/constants.jsx';

const wrap = async (promise, fallback) => {
  try {
    const res = await promise;
    return { success: true, data: res.data };
  } catch (err) {
    if (!err.response) {
      return { success: false, error: 'Cannot reach server. Is the backend running on port 1001?' };
    }
    return {
      success: false,
      error: err.response.data?.message || `${fallback} (${err.response.status})`
    };
  }
};

export const login = async ({ username, password }) => {
  const result = await wrap(api.post('/auth/login', { username, password }), 'Login failed');
  if (result.success && result.data?.token) {
    localStorage.setItem(STORAGE_KEYS.TOKEN, result.data.token);
    localStorage.setItem(STORAGE_KEYS.ROLE, result.data.role);
    localStorage.setItem(STORAGE_KEYS.USERNAME, result.data.username);
  } else if (result.success && !result.data?.token) {
    // Server replied 200 but no token (e.g. pending approval message)
    return { success: false, error: result.data?.message || 'Login rejected' };
  }
  return result;
};

export const register = ({ username, password, role }) =>
  wrap(api.post('/auth/register', { username, password, role }), 'Signup failed');

export const logout = () => {
  localStorage.removeItem(STORAGE_KEYS.TOKEN);
  localStorage.removeItem(STORAGE_KEYS.ROLE);
  localStorage.removeItem(STORAGE_KEYS.USERNAME);
  window.location.href = '/';
};

export const isAuthenticated = () => !!localStorage.getItem(STORAGE_KEYS.TOKEN);
export const getRole     = () => localStorage.getItem(STORAGE_KEYS.ROLE);
export const getUsername = () => localStorage.getItem(STORAGE_KEYS.USERNAME);

// SuperAdmin: user-management endpoints
export const listUsers       = () => wrap(api.get('/auth/users'),       'Failed to load users');
export const listPendingUsers = () => wrap(api.get('/auth/users/pending'), 'Failed to load pending users');
export const approveUser     = (id) => wrap(api.put(`/auth/users/${id}/approve`), 'Failed to approve');
export const declineUser     = (id) => wrap(api.delete(`/auth/users/${id}`),       'Failed to decline');
