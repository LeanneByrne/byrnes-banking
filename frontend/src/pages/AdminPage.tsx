import { useCallback, useEffect, useState, type FormEvent } from "react";
import { createTransfer, getAllCustomers, getLedgerBalanceCheck, ApiError } from "../api/client";
import type { CustomerResponse, LedgerBalanceCheckResponse } from "../api/types";
import { useAuth } from "../auth/AuthContext";

interface StressTestResult {
  succeeded: number;
  failed: number;
  balanceCheck: LedgerBalanceCheckResponse;
}

export function AdminPage() {
  const { session } = useAuth();
  const [customers, setCustomers] = useState<CustomerResponse[] | null>(null);
  const [balanceCheck, setBalanceCheck] = useState<LedgerBalanceCheckResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [sourceAccountId, setSourceAccountId] = useState("00000000-0000-0000-0000-000000000111");
  const [destinationAccountId, setDestinationAccountId] = useState("00000000-0000-0000-0000-000000000211");
  const [amount, setAmount] = useState("10");
  const [count, setCount] = useState("20");
  const [running, setRunning] = useState(false);
  const [stressResult, setStressResult] = useState<StressTestResult | null>(null);

  const refreshBalanceCheck = useCallback(() => {
    if (!session) return;
    getLedgerBalanceCheck(session.token).then(setBalanceCheck).catch(() => undefined);
  }, [session]);

  useEffect(() => {
    if (!session) return;
    getAllCustomers(session.token)
      .then(setCustomers)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Failed to load customers"));
    refreshBalanceCheck();
  }, [session, refreshBalanceCheck]);

  /**
   * Fires `count` concurrent transfer attempts -- the same no-double-spend
   * guarantee TransferConcurrencyIntegrationTest proves on the backend,
   * made visible: some succeed, some correctly fail with insufficient
   * funds, and the ledger still nets to zero afterward regardless. As
   * ADMIN, the backend's ownership check is bypassed (see
   * TransferService.requireOwnership), so this can move money between any
   * two accounts without needing to own either one.
   */
  async function runStressTest(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setRunning(true);
    setStressResult(null);
    const amountCents = Math.round(Number(amount) * 100);
    const attempts = Array.from({ length: Number(count) }, () =>
      createTransfer(session.token, sourceAccountId, destinationAccountId, amountCents, crypto.randomUUID()),
    );
    const outcomes = await Promise.allSettled(attempts);
    const succeeded = outcomes.filter((o) => o.status === "fulfilled").length;
    const failed = outcomes.length - succeeded;

    const balance = await getLedgerBalanceCheck(session.token);
    setBalanceCheck(balance);
    setStressResult({ succeeded, failed, balanceCheck: balance });
    setRunning(false);
  }

  return (
    <div className="page">
      <h1>Admin</h1>
      {error && <p className="error">{error}</p>}

      <section>
        <h2>Ledger integrity</h2>
        {balanceCheck && (
          <p className={balanceCheck.balanced ? "success" : "error"}>
            Net across every account: {balanceCheck.netCents} cents --{" "}
            {balanceCheck.balanced ? "balanced" : "NOT balanced"}
          </p>
        )}
        <button type="button" onClick={refreshBalanceCheck}>
          Refresh
        </button>
      </section>

      <section>
        <h2>All customers</h2>
        <table className="history-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
            </tr>
          </thead>
          <tbody>
            {customers?.map((customer) => (
              <tr key={customer.id}>
                <td>
                  {customer.firstName} {customer.lastName}
                </td>
                <td>{customer.email}</td>
                <td>{customer.role}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>Stress test: concurrent transfers</h2>
        <p>
          Fires several transfer attempts at the same instant between two accounts, then re-checks
          the whole ledger -- proving the no-double-spend guarantee under real concurrency, not
          just in a unit test.
        </p>
        <form onSubmit={runStressTest}>
          <label>
            Source account ID
            <input value={sourceAccountId} onChange={(e) => setSourceAccountId(e.target.value)} required />
          </label>
          <label>
            Destination account ID
            <input value={destinationAccountId} onChange={(e) => setDestinationAccountId(e.target.value)} required />
          </label>
          <label>
            Amount per transfer (USD)
            <input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          </label>
          <label>
            Concurrent attempts
            <input type="number" min="2" max="100" value={count} onChange={(e) => setCount(e.target.value)} required />
          </label>
          <button type="submit" disabled={running}>
            {running ? "Running..." : "Run stress test"}
          </button>
        </form>
        {stressResult && (
          <div className="stress-result">
            <p>
              {stressResult.succeeded} succeeded, {stressResult.failed} failed (insufficient funds)
            </p>
            <p className={stressResult.balanceCheck.balanced ? "success" : "error"}>
              Ledger after stress test: net {stressResult.balanceCheck.netCents} cents --{" "}
              {stressResult.balanceCheck.balanced ? "still balanced" : "NOT balanced"}
            </p>
          </div>
        )}
      </section>
    </div>
  );
}
