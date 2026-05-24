import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

const STORAGE_KEY = "vaultx_customer";
const API_BASE = "http://localhost:8089/api/customers";

export function AuthProvider({ children }) {
  const [customer, setCustomer] = useState(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (customer) localStorage.setItem(STORAGE_KEY, JSON.stringify(customer));
    else localStorage.removeItem(STORAGE_KEY);
  }, [customer]);

  const login = async (email, password) => {
    const res = await fetch(`${API_BASE}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    if (res.status === 401) throw new Error("Invalid email or password.");
    if (!res.ok) throw new Error(`Login failed (${res.status})`);
    const data = await res.json();
    setCustomer(data);
    return data;
  };

  const signup = async (payload) => {
    const res = await fetch(API_BASE, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      let message = `Signup failed (${res.status})`;
      try {
        const errData = await res.json();
        if (errData?.message) message = errData.message;
      } catch { /* ignore */ }
      throw new Error(message);
    }
    const data = await res.json();
    setCustomer(data);
    return data;
  };

  const logout = () => setCustomer(null);

  const value = {
    customer,
    customerId: customer?.customerId ?? null,
    email: customer?.email ?? null,
    isAuthenticated: !!customer,
    login,
    signup,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}

export { AuthContext };
