import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getCustomerAccounts, ApiError } from "../api/client";
import type { AccountResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";

function centsToDisplay(cents: number): string {
  const sign = cents < 0 ? "-" : "";
  const abs = Math.abs(cents);
  return `${sign}$${(abs / 100).toFixed(2)}`;
}

export function DashboardPage() {
  const { session } = useAuth();
  const [accounts, setAccounts] = useState<AccountResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    getCustomerAccounts(session.token, session.customerId)
      .then(setAccounts)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Failed to load accounts"));
  }, [session]);

  return (
    <div className="page">
      <h1>Your accounts</h1>
      {error && <p className="error">{error}</p>}
      {!accounts && !error && <p>Loading...</p>}
      <div className="account-grid">
        {accounts?.map((account) => (
          <div key={account.id} className="account-card">
            <div className="account-type">{account.accountType}</div>
            <div className="account-number">{account.accountNumber}</div>
            <div className="account-balance">{centsToDisplay(account.balanceCents)}</div>
            <div className="account-status">{account.status}</div>
            <Link to={`/accounts/${account.id}`}>View history</Link>
          </div>
        ))}
      </div>
    </div>
  );
}
