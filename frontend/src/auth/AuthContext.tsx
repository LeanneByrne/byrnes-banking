import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { Role } from "../api/types";

interface Session {
  token: string;
  customerId: string;
  role: Role;
}

interface AuthContextValue {
  session: Session | null;
  login: (session: Session) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Held in React state only -- never localStorage/sessionStorage. A token in
 * browser storage is readable by any script that runs on the page (e.g. via
 * an XSS bug in a dependency), and persists across tabs/reloads; an
 * in-memory-only token is wiped by a refresh and isn't reachable by code
 * running in a different tab. The cost is that refreshing the page logs you
 * out -- an acceptable tradeoff here since logging back in is one click via
 * the demo buttons on the login page.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      login: setSession,
      logout: () => setSession(null),
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
