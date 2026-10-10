import type {
  AccountResponse,
  AuthResponse,
  CustomerResponse,
  LedgerBalanceCheckResponse,
  LedgerEntryResponse,
  Page,
  ProblemDetail,
  TransferResponse,
} from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, detail: string) {
    super(detail);
    this.status = status;
  }
}

async function request<T>(
  path: string,
  options: { method?: string; token?: string; body?: unknown; headers?: Record<string, string> } = {},
): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    let detail = response.statusText;
    try {
      const problem = (await response.json()) as ProblemDetail;
      detail = problem.detail ?? problem.title ?? detail;
    } catch {
      // Response body wasn't a ProblemDetail (e.g. a plain 401 with no
      // body) -- fall back to the status text already captured above.
    }
    throw new ApiError(response.status, detail);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export function register(
  firstName: string,
  lastName: string,
  email: string,
  password: string,
): Promise<AuthResponse> {
  return request<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: { firstName, lastName, email, password },
  });
}

export function getCustomerAccounts(token: string, customerId: string): Promise<AccountResponse[]> {
  return request<AccountResponse[]>(`/api/customers/${customerId}/accounts`, { token });
}

export function getAllCustomers(token: string): Promise<CustomerResponse[]> {
  return request<CustomerResponse[]>("/api/customers", { token });
}

export function getAccount(token: string, accountId: string): Promise<AccountResponse> {
  return request<AccountResponse>(`/api/accounts/${accountId}`, { token });
}

export function getAccountTransactions(
  token: string,
  accountId: string,
  page: number,
  size: number,
): Promise<Page<LedgerEntryResponse>> {
  return request<Page<LedgerEntryResponse>>(
    `/api/accounts/${accountId}/transactions?page=${page}&size=${size}`,
    { token },
  );
}

export function createTransfer(
  token: string,
  sourceAccountId: string,
  destinationAccountId: string,
  amountCents: number,
  idempotencyKey: string,
): Promise<TransferResponse> {
  return request<TransferResponse>("/api/transfers", {
    method: "POST",
    token,
    headers: { "Idempotency-Key": idempotencyKey },
    body: { sourceAccountId, destinationAccountId, amountCents },
  });
}

export function getLedgerBalanceCheck(token: string): Promise<LedgerBalanceCheckResponse> {
  return request<LedgerBalanceCheckResponse>("/api/ledger/balance-check", { token });
}
