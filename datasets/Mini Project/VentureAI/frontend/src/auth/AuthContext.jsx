import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('ventureai_user');
    return stored ? JSON.parse(stored) : null;
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      localStorage.setItem('ventureai_user', JSON.stringify(user));
    }
  }, [user]);

  const login = async (payload) => {
    setLoading(true);
    try {
      const data = await authApi.login(payload);
      persistSession(data);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const register = async (payload) => {
    setLoading(true);
    try {
      const data = await authApi.register(payload);
      persistSession(data);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const persistSession = (data) => {
    localStorage.setItem('ventureai_token', data.token);
    const nextUser = { id: data.id, fullName: data.fullName, email: data.email, userType: data.userType };
    localStorage.setItem('ventureai_user', JSON.stringify(nextUser));
    setUser(nextUser);
  };

  const logout = () => {
    localStorage.removeItem('ventureai_token');
    localStorage.removeItem('ventureai_user');
    setUser(null);
  };

  const value = useMemo(
    () => ({ user, isAuthenticated: !!user, loading, login, register, logout }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
