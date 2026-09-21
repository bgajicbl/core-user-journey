import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { setUnauthorizedHandler } from "../api/client";

interface AuthState {
  token: string | null;
  studentName: string | null;
  login: (token: string, studentName: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

const TOKEN_KEY = "cuj.token";
const NAME_KEY = "cuj.studentName";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [studentName, setStudentName] = useState<string | null>(() => localStorage.getItem(NAME_KEY));
  const navigate = useNavigate();

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    setToken(null);
    setStudentName(null);
  }, []);

  // If a token is rejected as missing/invalid/expired by an authenticated request, force a
  // logout and send the student back to /login rather than leaving them stuck on a page that
  // will just keep failing every request.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
      navigate("/login", { replace: true });
    });
    return () => setUnauthorizedHandler(null);
  }, [logout, navigate]);

  const value = useMemo<AuthState>(
    () => ({
      token,
      studentName,
      login: (newToken: string, newStudentName: string) => {
        localStorage.setItem(TOKEN_KEY, newToken);
        localStorage.setItem(NAME_KEY, newStudentName);
        setToken(newToken);
        setStudentName(newStudentName);
      },
      logout,
    }),
    [token, studentName, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
