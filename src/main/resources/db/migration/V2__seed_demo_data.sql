-- Demo/seed data only. All UUIDs below are fixed literals purely so this file
-- can self-reference rows across INSERT statements; nothing about them is
-- meaningful outside this seed.

-- SYSTEM funding account: an exogenous source that "mints" the starting
-- balances for demo customers below. It is expected to carry a large negative
-- derived balance (it only ever debits itself to credit someone else) — that
-- is intentional, not a bug, and is documented in docs/decisions/.
-- Its owning customer is disabled and has no usable password: it exists only
-- to satisfy the accounts.customer_id FK, never to log in.
INSERT INTO customers (id, first_name, last_name, email, password_hash, role, enabled)
VALUES ('00000000-0000-0000-0000-000000000001', 'System', 'Treasury', 'system@byrnesbanking.internal',
        '$2y$10$FsupQirn3dsk2nIXBC7v9eVdIhDoSHVr16sHNqrsbOIJhRvun5lI.', 'ADMIN', FALSE);

INSERT INTO accounts (id, customer_id, account_number, account_type, currency, status)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
        'SYS0000000001', 'SYSTEM', 'USD', 'ACTIVE');

-- Demo customers. All three share the password "Demo1234!" (bcrypt-hashed
-- below) so reviewers can log in with any of them — see README "Login" section.
INSERT INTO customers (id, first_name, last_name, email, password_hash, role, enabled)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'Alice', 'Nguyen', 'alice@byrnesbanking.dev',
     '$2y$10$8BKaST9q662Xum9id1AFNeICgnCXVg0I7UDa5VplRArL8aLHQZLDy', 'CUSTOMER', TRUE),
    ('00000000-0000-0000-0000-000000000201', 'Ben', 'Carter', 'ben@byrnesbanking.dev',
     '$2y$10$8BKaST9q662Xum9id1AFNeICgnCXVg0I7UDa5VplRArL8aLHQZLDy', 'CUSTOMER', TRUE),
    ('00000000-0000-0000-0000-000000000301', 'Priya', 'Shah', 'priya@byrnesbanking.dev',
     '$2y$10$8BKaST9q662Xum9id1AFNeICgnCXVg0I7UDa5VplRArL8aLHQZLDy', 'CUSTOMER', TRUE);

INSERT INTO accounts (id, customer_id, account_number, account_type, currency, status)
VALUES
    ('00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000101', 'ACC1000001001', 'CHECKING', 'USD', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000112', '00000000-0000-0000-0000-000000000101', 'ACC1000001002', 'SAVINGS', 'USD', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000211', '00000000-0000-0000-0000-000000000201', 'ACC1000002001', 'CHECKING', 'USD', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000212', '00000000-0000-0000-0000-000000000201', 'ACC1000002002', 'SAVINGS', 'USD', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000311', '00000000-0000-0000-0000-000000000301', 'ACC1000003001', 'CHECKING', 'USD', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000312', '00000000-0000-0000-0000-000000000301', 'ACC1000003002', 'SAVINGS', 'USD', 'ACTIVE');

-- Funding transfers from SYSTEM into each demo account, plus one peer-to-peer
-- transfer between demo accounts, purely so the demo data has real
-- transaction history to page through, not just a single opening balance.
INSERT INTO transfers (id, source_account_id, destination_account_id, amount_cents, currency, status, idempotency_key, completed_at)
VALUES
    ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000111', 250000,  'USD', 'COMPLETED', 'seed-fund-alice-checking', now()),
    ('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000112', 1000000, 'USD', 'COMPLETED', 'seed-fund-alice-savings',  now()),
    ('00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000211', 120050,  'USD', 'COMPLETED', 'seed-fund-ben-checking',   now()),
    ('00000000-0000-0000-0000-000000001004', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000212', 500000,  'USD', 'COMPLETED', 'seed-fund-ben-savings',    now()),
    ('00000000-0000-0000-0000-000000001005', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000311', 80000,   'USD', 'COMPLETED', 'seed-fund-priya-checking', now()),
    ('00000000-0000-0000-0000-000000001006', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000312', 1525075, 'USD', 'COMPLETED', 'seed-fund-priya-savings',  now()),
    ('00000000-0000-0000-0000-000000001007', '00000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000211', 5000,    'USD', 'COMPLETED', 'seed-alice-pays-ben',      now());

INSERT INTO ledger_entries (transfer_id, account_id, entry_type, amount_cents)
VALUES
    ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000002', 'DEBIT',  250000),
    ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000111', 'CREDIT', 250000),

    ('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000002', 'DEBIT',  1000000),
    ('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000112', 'CREDIT', 1000000),

    ('00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000000002', 'DEBIT',  120050),
    ('00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000000211', 'CREDIT', 120050),

    ('00000000-0000-0000-0000-000000001004', '00000000-0000-0000-0000-000000000002', 'DEBIT',  500000),
    ('00000000-0000-0000-0000-000000001004', '00000000-0000-0000-0000-000000000212', 'CREDIT', 500000),

    ('00000000-0000-0000-0000-000000001005', '00000000-0000-0000-0000-000000000002', 'DEBIT',  80000),
    ('00000000-0000-0000-0000-000000001005', '00000000-0000-0000-0000-000000000311', 'CREDIT', 80000),

    ('00000000-0000-0000-0000-000000001006', '00000000-0000-0000-0000-000000000002', 'DEBIT',  1525075),
    ('00000000-0000-0000-0000-000000001006', '00000000-0000-0000-0000-000000000312', 'CREDIT', 1525075),

    ('00000000-0000-0000-0000-000000001007', '00000000-0000-0000-0000-000000000111', 'DEBIT',  5000),
    ('00000000-0000-0000-0000-000000001007', '00000000-0000-0000-0000-000000000211', 'CREDIT', 5000);
