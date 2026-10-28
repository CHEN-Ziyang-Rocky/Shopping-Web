'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { getCsrfToken } from '@/lib/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

interface User {
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (user: User) => void;
  logout: () => Promise<void>;
  clearUser: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // 网页一加载，就去后端问：“我之前登录过吗？”（浏览器会自动带上 HttpOnly Cookie）
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/auth/me`, {
          credentials: 'include', // 【核心】必须带上这句，才会发送 Cookie
        });
        if (res.ok) {
          const data = await res.json();
          setUser({ email: data.email, role: data.role });
        } else {
          setUser(null);
        }
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    checkAuth();
  }, []);

  const login = (userData: User) => {
    setUser(userData);
  };

  const logout = async () => {
    try {
      // 告诉后端销毁 Session
      const token = await getCsrfToken();
      const response = await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': token },
      });
      if (!response.ok && response.status !== 401) throw new Error('Logout failed. Please try again.');
      setUser(null);
      router.push('/login'); // 登出后跳回登录页
    } catch (error) {
      alert(error instanceof Error ? error.message : "Logout failed. Please try again.");
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, clearUser: () => setUser(null) }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
