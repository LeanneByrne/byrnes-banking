import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getAccount, getAccountTransactions, ApiError } from "../api/client";
import type { AccountResponse, LedgerEntryResponse, Page } from "../api/types";
import { useAuth } from "../auth/AuthContext";

const PAGE_SIZE = 10;

function centsToDisplay(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

export function AccountHistoryPage() {
  const { accountId } = useParams<{ accountId: string }>();
  const { session } = useAuth();
  const [account, setAccount] = useState<AccountResponse | null>(null);
  const [transactions, setTransactions] = useState<Page<LedgerEntryResponse> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session || !accountId) return;
    getAccount(session.token, accountId)
      .then(setAccount)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Failed to load account"));
  }, [session, accountId]);

  useEffect(() => {
    if (!session || !accountId) return;
    getAccountTransactions(session.token, accountId, pageNumber, PAGE_SIZE)
      .then(setTransactions)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Failed to load transactions"));
  }, [session, accountId, pageNumber]);

  return (
    <div className="page">
      <Link to="/">&larr; Back to accounts</Link>
      <h1>{account ? `${account.accountType} ${account.accountNumber}` : "Account"}</h1>
      {error && <p className="error">{error}</p>}

      <table className="history-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          {transactions?.content.map((entry) => (
            <tr key={entry.id}>
              <td>{new Date(entry.createdAt).toLocaleString()}</td>
              <td>{entry.entryType}</td>
              <td>{centsToDisplay(entry.amountCents)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {transactions && transactions.totalPages > 1 && (
        <div className="pagination">
          <button type="button" disabled={pageNumber === 0} onClick={() => setPageNumber((p) => p - 1)}>
            Previous
          </button>
          <span>
            Page {pageNumber + 1} of {transactions.totalPages}
          </span>
          <button
            type="button"
            disabled={pageNumber + 1 >= transactions.totalPages}
            onClick={() => setPageNumber((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
