# BearWallet — Native Android Social Banking & Closed-Loop Economy Simulator

BearWallet is a modern native Android application that combines digital wallets, virtual banking, a central bank, dynamic currency exchange, credit scoring, and a closed-loop virtual economy for a small private community of friends and a partner.

> **Disclaimer**: This entire application is a **virtual currency simulator for entertainment**. It does **not** represent actual deposits, real financial services, redeemable rupiah, or real investments. Rupiah amounts are simulated reference values only. There are no real-money payment gateways or cash withdrawals.

---

## 1. Project Configuration & Supabase Backend

- **Supabase Project URL**: `https://ohonmeefuioicdbbsvji.supabase.co`
- **Publishable Key**: `sb_publishable_Ps0CB9oK1DAC2CTuWoN2sA_CsYiApj`
- **Database Architecture**: Double-entry ledger accounting, PostgreSQL Row-Level Security (RLS), and atomic `SECURITY DEFINER` stored procedures.

---

## 2. Economic System & Invariants

| System Account | Code | Initial Balance | Purpose |
|---|---|---:|---|
| **Locked Central Bank Reserve** | `SYS_LOCKED_RESERVE` | 100,000 B | Non-circulating reserve; permanently locked from transfers, lending, or exchange |
| **Bank Lending Pool** | `SYS_BANK_LENDING_POOL` | 250,000 B | Source of funds for all approved community loans |
| **Exchange Inventory** | `SYS_EXCHANGE_INVENTORY` | 200,000 B | Virtual market maker liquidity for Buy/Sell operations |
| **User Distribution Pool** | `SYS_USER_DISTRIBUTION_POOL` | 450,000 B | Source of 500 B signup welcome grants and faucet claims |
| **Bank Interest Income** | `SYS_BANK_INTEREST_INCOME` | 0 B | Sink for flat interest portions repaid by borrowers |
| **Total Authorized Supply** | — | **1,000,000 B** | **Strictly finite ceiling. Never arbitrarily minted.** |

### Verified Economic Invariants:
1. **Supply Conservation**: `Locked Reserve + Lending Pool + Exchange Inventory + Distribution Pool + Circulating User Balances = 1,000,000 B`.
2. **Double-Entry Ledger**: Every movement is recorded atomically in `ledger_transactions` with an exact debit and credit.
3. **No Arbitrary Minting**: Top-ups and signup grants transfer from `SYS_USER_DISTRIBUTION_POOL`. If the pool is exhausted, the operation is deferred/rejected.
4. **Authoritative Concurrency Locking**: Concurrency-safe row-level locking (`FOR UPDATE`) prevents overdrawing accounts.
5. **Idempotent Execution**: Transfers and top-ups accept unique idempotency keys to prevent double-spending on network retries.

---

## 3. Four-Stage Loan Processing Architecture

1. **Stage 1 — Eligibility Screening**:
   - Authenticated account verification.
   - Zero active overdue loans check.
   - Requested amount must fall within facility limit.
2. **Stage 2 — Credit Score Analysis**:
   - Evaluates transparent simulated score (300–850 scale, starting at 600).
   - Rating tiers: Fair (300-649), Good (650-749), Excellent (750-850).
   - Full settlements boost score by +25 points.
3. **Stage 3 — Bank Liquidity Assessment**:
   - Inspects `SYS_BANK_LENDING_POOL`.
   - **Critical Rule**: A high credit score **cannot** override insufficient bank liquidity. If pool < requested principal, the loan is rejected.
4. **Stage 4 — Loan Decision & Offer**:
   - If approved: User reviews principal, flat interest rate (Quick 2%, Regular 5%, Long 9%), total repayment, and due date.
   - Requires explicit **"Accept Offer & Disburse"** confirmation before Bear moves into the user's wallet.
   - On repayment: Principal routes back to `SYS_BANK_LENDING_POOL`, while interest routes to `SYS_BANK_INTEREST_INCOME`.

---

## 4. Backend Deployment Instructions

All migration scripts and Edge Functions are pre-generated in this repository.

### Step 1: Apply SQL Migrations to Supabase
1. Open your [Supabase Dashboard](https://supabase.com/dashboard/project/ohonmeefuioicdbbsvji).
2. Navigate to the **SQL Editor** tab on the left sidebar.
3. Copy the entire contents of `supabase/migrations/20261017_init_bearwallet.sql`.
4. Paste into the SQL editor and click **Run**.
5. This creates all tables (`profiles`, `wallets`, `system_accounts`, `central_bank_settings`, `ledger_transactions`, `exchange_rates`, `exchange_orders`, `loan_products`, `loan_applications`, `loans`, `loan_repayments`, `notifications`, `audit_logs`), enables Row-Level Security (RLS), seeds the 1,000,000 B initial economic distribution, and registers secure PostgreSQL RPCs.

### Step 2: Deploy Edge Functions (Optional for scheduled jobs)
Deploy the provided Edge Functions via the Supabase CLI:
```bash
# 1. Scheduled 4-hour market ticker
supabase functions deploy market-ticker --project-ref ohonmeefuioicdbbsvji

# 2. Asynchronous loan processing worker
supabase functions deploy loan-processor --project-ref ohonmeefuioicdbbsvji
```

### Step 3: Schedule Cron Job in Supabase Dashboard
To run the market ticker every 4 hours automatically:
In Supabase **Database > Extensions**, enable `pg_cron` and execute:
```sql
SELECT cron.schedule(
    'market-rate-4h',
    '0 */4 * * *',
    $$ SELECT public.update_market_rate('Automated 4H Market Settlement'); $$
);
```

### Step 4: Admin Bootstrap Procedure
To grant central bank inspector privileges to your account:
In Supabase SQL Editor:
```sql
UPDATE public.profiles
SET role = 'admin'
WHERE email = 'windu.mariposa@gmail.com';
```

---

## 5. Verification & Test Checklist

- [x] **Registration & Welcome Grant**: Creating an account credits 500 B from `SYS_USER_DISTRIBUTION_POOL`.
- [x] **Double-Entry Transfers**: Transferring Bear between `@bearboss`, `@maya`, and `@alex` atomically debits the sender and credits the recipient.
- [x] **Idempotency**: Repeated transfer submissions with identical keys do not duplicate transactions.
- [x] **Overdraft Prevention**: Attempts to send more than available balance fail cleanly with user feedback.
- [x] **Supply Invariant Check**: Total supply across all accounts strictly equals 1,000,000 B on the Central Bank Inspector dashboard.
- [x] **Locked Reserve Isolation**: 100,000 B locked reserve remains untouched by ordinary transfers or loans.
- [x] **Dynamic Exchange Market**: Live Canvas chart with 24H/7D/30D filters and inventory-backed Buy/Sell operations.
- [x] **4-Stage Loan Processing**: Persistent multi-step state verification that survives app restarts.
- [x] **Disbursement & Repayment**: Principal returns to lending pool; flat interest routes to bank interest income.
- [x] **Multi-Account Testing**: Quick Switcher in the Profile tab allows instant testing of transfers and approvals between community accounts.
