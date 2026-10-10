import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { Role } from "../api/types";

export function ProtectedRoute({ children, requireRole }: { children: ReactNode; requireRole?: Role }) {
  const { session } = useAuth();

  if (!session) {
    return <Navigate to="/login" replace />;
  }
  if (requireRole && session.role !== requireRole) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
