import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function NavBar() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  if (!session) {
    return null;
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <nav className="nav-bar">
      <Link to="/">Accounts</Link>
      <Link to="/transfer">Transfer</Link>
      {session.role === "ADMIN" && <Link to="/admin">Admin</Link>}
      <span className="nav-spacer" />
      <span className="nav-role">{session.role}</span>
      <button type="button" onClick={handleLogout}>
        Log out
      </button>
    </nav>
  );
}
