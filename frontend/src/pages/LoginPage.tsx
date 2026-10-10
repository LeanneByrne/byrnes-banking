import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { login, ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";

const DEMO_LOGINS = [
  { label: "Alice (customer)", email: "alice@byrnesbanking.dev" },
  { label: "Ben (customer)", email: "ben@byrnesbanking.dev" },
  { label: "Priya (customer)", email: "priya@byrnesbanking.dev" },
];
const DEMO_PASSWORD = "Demo1234!";
const ADMIN_EMAIL = "admin@byrnesbanking.dev";
const ADMIN_PASSWORD = "AdminDemo1234!";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { login: setSession } = useAuth();
  const navigate = useNavigate();

  async function doLogin(loginEmail: string, loginPassword: string) {
    setError(null);
    setLoading(true);
    try {
      const auth = await login(loginEmail, loginPassword);
      setSession({ token: auth.token, customerId: auth.customerId, role: auth.role });
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    void doLogin(email, password);
  }

  return (
    <div className="page page-narrow">
      <h1>ByrnesBanking</h1>
      <p className="demo-disclaimer">This is a demo, not a real bank. No real money moves here.</p>

      <section>
        <h2>One-click demo login</h2>
        <div className="demo-buttons">
          {DEMO_LOGINS.map((demo) => (
            <button
              key={demo.email}
              type="button"
              disabled={loading}
              onClick={() => void doLogin(demo.email, DEMO_PASSWORD)}
            >
              {demo.label}
            </button>
          ))}
          <button type="button" disabled={loading} onClick={() => void doLogin(ADMIN_EMAIL, ADMIN_PASSWORD)}>
            Admin
          </button>
        </div>
      </section>

      <section>
        <h2>Or log in</h2>
        <form onSubmit={handleSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={loading}>
            Log in
          </button>
        </form>
      </section>
    </div>
  );
}
