import { useEffect, useState, type FormEvent } from "react";
import { createTransfer, getCustomerAccounts, ApiError } from "../api/client";
import type { AccountResponse, TransferResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";

const DEMO_ACCOUNTS = [
  { label: "Alice checking", id: "00000000-0000-0000-0000-000000000111" },
  { label: "Alice savings", id: "00000000-0000-0000-0000-000000000112" },
  { label: "Ben checking", id: "00000000-0000-0000-0000-000000000211" },
  { label: "Ben savings", id: "00000000-0000-0000-0000-000000000212" },
  { label: "Priya checking", id: "00000000-0000-0000-0000-000000000311" },
  { label: "Priya savings", id: "00000000-0000-0000-0000-000000000312" },
];

export function TransferPage() {
  const { session } = useAuth();
  const [ownAccounts, setOwnAccounts] = useState<AccountResponse[]>([]);
  const [sourceAccountId, setSourceAccountId] = useState("");
  const [destinationAccountId, setDestinationAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [result, setResult] = useState<TransferResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!session) return;
    getCustomerAccounts(session.token, session.customerId).then((accounts) => {
      setOwnAccounts(accounts);
      if (accounts.length > 0) setSourceAccountId(accounts[0].id);
    });
  }, [session]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const amountCents = Math.round(Number(amount) * 100);
      const idempotencyKey = crypto.randomUUID();
      const transfer = await createTransfer(
        session.token,
        sourceAccountId,
        destinationAccountId,
        amountCents,
        idempotencyKey,
      );
      setResult(transfer);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Transfer failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Make a transfer</h1>
      <form onSubmit={handleSubmit}>
        <label>
          From
          <select value={sourceAccountId} onChange={(e) => setSourceAccountId(e.target.value)} required>
            {ownAccounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.accountType} {account.accountNumber}
              </option>
            ))}
          </select>
        </label>
        <label>
          To (account ID)
          <input
            value={destinationAccountId}
            onChange={(e) => setDestinationAccountId(e.target.value)}
            placeholder="00000000-0000-0000-0000-000000000211"
            required
          />
        </label>
        <label>
          Amount (USD)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </label>
        {error && <p className="error">{error}</p>}
        {result && (
          <p className="success">
            Transfer {result.status.toLowerCase()} ({result.id})
          </p>
        )}
        <button type="submit" disabled={submitting}>
          Send
        </button>
      </form>

      <section>
        <h2>Demo account IDs</h2>
        <p>For trying transfers to a demo account that isn't one of yours:</p>
        <ul className="demo-account-list">
          {DEMO_ACCOUNTS.map((account) => (
            <li key={account.id}>
              {account.label}: <code>{account.id}</code>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
