import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

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
      logout: () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(NAME_KEY);
        setToken(null);
        setStudentName(null);
      },
    }),
    [token, studentName],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
