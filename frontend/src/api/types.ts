export type Role = "CUSTOMER" | "ADMIN";

export interface AuthResponse {
  token: string;
  customerId: string;
  role: Role;
}

export interface CustomerResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export type AccountType = "CHECKING" | "SAVINGS" | "SYSTEM";
export type AccountStatus = "ACTIVE" | "FROZEN" | "CLOSED";

export interface AccountResponse {
  id: string;
  customerId: string;
  accountNumber: string;
  accountType: AccountType;
  currency: string;
  status: AccountStatus;
  balanceCents: number;
}

export type TransferStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface TransferResponse {
  id: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amountCents: number;
  currency: string;
  status: TransferStatus;
  idempotencyKey: string;
  failureReason: string | null;
  createdAt: string;
  completedAt: string | null;
}

export type EntryType = "DEBIT" | "CREDIT";

export interface LedgerEntryResponse {
  id: string;
  transferId: string;
  accountId: string;
  entryType: EntryType;
  amountCents: number;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LedgerBalanceCheckResponse {
  netCents: number;
  balanced: boolean;
}

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
}
