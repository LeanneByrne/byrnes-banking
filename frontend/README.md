# ByrnesBanking frontend

React + TypeScript + Vite single-page app for the ByrnesBanking API. See the repo root
[`README.md`](../README.md) for the full project context and the "demo, not a real bank" note.

## Running it

The backend must already be running (see the root README) on `http://localhost:8080`.

```bash
cp .env.example .env   # default already points at localhost:8080
npm install
npm run dev             # http://localhost:5173
```

## Structure

- `src/api/` — a thin typed `fetch` wrapper (`client.ts`) and TypeScript types mirroring the
  backend's DTOs (`types.ts`)
- `src/auth/AuthContext.tsx` — holds the JWT in React state only, deliberately never
  `localStorage`/`sessionStorage` (a token in browser storage is readable by any script that runs
  on the page, including via a dependency's XSS bug; an in-memory-only token is wiped by a
  refresh and isn't reachable from another tab). The tradeoff: refreshing the page logs you out —
  acceptable here since logging back in is one click via the demo buttons.
- `src/pages/` — one file per route: login (with one-click demo logins), the accounts dashboard,
  per-account transaction history, the transfer form, and the admin panel (customer list, ledger
  balance check, and the concurrent-transfer stress test)
- `src/components/` — `NavBar` and `ProtectedRoute` (redirects to `/login` if there's no session,
  or to `/` if a non-ADMIN hits an ADMIN-only route)

## Why a separate app instead of server-rendered pages

The backend is a pure JSON API with no concept of HTML. This app consumes it exactly the way a
mobile app or third-party integrator would — which is also a second, independent proof that the
API is a usable contract, not just something the Java code itself understands.
