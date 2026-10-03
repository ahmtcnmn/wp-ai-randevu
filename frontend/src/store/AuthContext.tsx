"use client";
import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
} from "react";
import Cookies from "js-cookie";
import { useRouter } from "next/navigation";
import { AuthResponse, UserResponse } from "@/types/auth";
import {
  COOKIE_REFRESH,
  COOKIE_TOKEN,
  REFRESH_EXPIRES_DAYS,
  TOKEN_EXPIRES_DAYS,
} from "@/lib/constants";
import { authApi } from "@/lib/api";

interface AuthContextType {
  user: AuthResponse | null;
  fullUser: UserResponse | null;
  /** Login response geldiğinde çağrılır (token + cookie + state) */
  setSession: (data: AuthResponse) => void;
  /** Cookie'leri ve state'i temizle */
  logout: () => Promise<void>;
  /** /auth/me ile fullUser refresh et */
  refreshMe: () => Promise<void>;
  isLoading: boolean;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

const STORAGE_KEY = "user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [fullUser, setFullUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // İlk yüklemede localStorage'tan oku
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as AuthResponse;
        if (Cookies.get(COOKIE_TOKEN)) {
          setUser(parsed);
        } else {
          localStorage.removeItem(STORAGE_KEY);
        }
      }
    } catch {
      // ignore
    }
    setIsLoading(false);
  }, []);

  // user değişince fullUser'ı çek
  useEffect(() => {
    if (user?.token) {
      authApi
        .me()
        .then(setFullUser)
        .catch(() => setFullUser(null));
    } else {
      setFullUser(null);
    }
  }, [user?.token]);

  const setSession = useCallback((data: AuthResponse) => {
    if (!data.token) {
      return;
    }
    Cookies.set(COOKIE_TOKEN, data.token, {
      expires: TOKEN_EXPIRES_DAYS,
      sameSite: "lax",
    });
    if (data.refreshToken) {
      Cookies.set(COOKIE_REFRESH, data.refreshToken, {
        expires: REFRESH_EXPIRES_DAYS,
        sameSite: "lax",
      });
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    setUser(data);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore — yine de local cleanup
    }
    Cookies.remove(COOKIE_TOKEN);
    Cookies.remove(COOKIE_REFRESH);
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
    setFullUser(null);
    router.push("/login");
  }, [router]);

  const refreshMe = useCallback(async () => {
    if (!user?.token) return;
    try {
      const me = await authApi.me();
      setFullUser(me);
    } catch {
      // ignore
    }
  }, [user?.token]);

  return (
    <AuthContext.Provider
      value={{
        user,
        fullUser,
        setSession,
        logout,
        refreshMe,
        isLoading,
        isAuthenticated: !!user?.token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
