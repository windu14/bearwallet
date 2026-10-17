-- ============================================================================
-- BEARWALLET POSTGRESQL MIGRATION
-- Project: BearWallet - Social Banking & Closed-Loop Economy Simulator
-- Currency: BEAR (B) | Initial Supply: 1,000,000 B | Starting Rate: 1 BEAR = IDR 10
-- ============================================================================

-- Enable pgcrypto for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. CENTRAL BANK SETTINGS
CREATE TABLE IF NOT EXISTS public.central_bank_settings (
    id SERIAL PRIMARY KEY,
    total_authorized_supply BIGINT NOT NULL DEFAULT 1000000,
    update_interval_hours INT NOT NULL DEFAULT 4,
    is_market_paused BOOLEAN NOT NULL DEFAULT FALSE,
    current_rate_idr NUMERIC(12, 4) NOT NULL DEFAULT 10.0000,
    default_credit_score INT NOT NULL DEFAULT 600,
    min_reserve_ratio NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Seed single row if not exists
INSERT INTO public.central_bank_settings (id, total_authorized_supply, update_interval_hours, is_market_paused, current_rate_idr)
VALUES (1, 1000000, 4, FALSE, 10.0000)
ON CONFLICT (id) DO NOTHING;

-- 2. SYSTEM ACCOUNTS (Double-entry source & sink pools)
CREATE TABLE IF NOT EXISTS public.system_accounts (
    account_code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    balance BIGINT NOT NULL CHECK (balance >= 0),
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Seed initial supply split (Total = 1,000,000 B)
INSERT INTO public.system_accounts (account_code, name, balance, is_locked)
VALUES
    ('SYS_LOCKED_RESERVE', 'Locked Central Bank Reserve', 100000, TRUE),
    ('SYS_BANK_LENDING_POOL', 'Bank Lending Pool', 250000, FALSE),
    ('SYS_EXCHANGE_INVENTORY', 'Exchange Market Inventory', 200000, FALSE),
    ('SYS_USER_DISTRIBUTION_POOL', 'User Signup & Distribution Pool', 450000, FALSE),
    ('SYS_BANK_INTEREST_INCOME', 'Bank Accumulated Interest Income', 0, FALSE)
ON CONFLICT (account_code) DO NOTHING;

-- 3. USER PROFILES
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    credit_score INT NOT NULL DEFAULT 600 CHECK (credit_score BETWEEN 300 AND 850),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 4. USER WALLETS
CREATE TABLE IF NOT EXISTS public.wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    currency TEXT NOT NULL DEFAULT 'BEAR',
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    locked_balance BIGINT NOT NULL DEFAULT 0 CHECK (locked_balance >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, currency)
);

-- 5. IMMUTABLE LEDGER TRANSACTIONS & DOUBLE ENTRY
CREATE TABLE IF NOT EXISTS public.ledger_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key TEXT UNIQUE NOT NULL,
    transaction_type TEXT NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    fee BIGINT NOT NULL DEFAULT 0 CHECK (fee >= 0),
    source_account TEXT NOT NULL,
    destination_account TEXT NOT NULL,
    note TEXT DEFAULT '',
    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED', 'PENDING')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 6. EXCHANGE RATES & ORDERS
CREATE TABLE IF NOT EXISTS public.exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rate_idr NUMERIC(12, 4) NOT NULL CHECK (rate_idr > 0),
    previous_rate NUMERIC(12, 4) NOT NULL CHECK (previous_rate > 0),
    change_percent NUMERIC(8, 4) NOT NULL,
    market_event TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

INSERT INTO public.exchange_rates (rate_idr, previous_rate, change_percent, market_event)
VALUES (10.0000, 10.0000, 0.0000, 'Initial Bear Market Inception');

CREATE TABLE IF NOT EXISTS public.exchange_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    order_type TEXT NOT NULL CHECK (order_type IN ('BUY', 'SELL')),
    bear_amount BIGINT NOT NULL CHECK (bear_amount > 0),
    idr_amount BIGINT NOT NULL CHECK (idr_amount > 0),
    rate_applied NUMERIC(12, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 7. LOAN PRODUCTS
CREATE TABLE IF NOT EXISTS public.loan_products (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    tenor_days INT NOT NULL CHECK (tenor_days > 0),
    interest_rate_percent NUMERIC(5, 2) NOT NULL CHECK (interest_rate_percent >= 0),
    min_amount BIGINT NOT NULL CHECK (min_amount > 0),
    max_amount BIGINT NOT NULL CHECK (max_amount >= min_amount),
    description TEXT NOT NULL
);

INSERT INTO public.loan_products (id, name, tenor_days, interest_rate_percent, min_amount, max_amount, description)
VALUES
    ('prod_quick', 'Bear Quick', 7, 2.00, 50, 1000, 'Rapid 7-day micro liquidity with 2% flat interest'),
    ('prod_regular', 'Bear Regular', 30, 5.00, 200, 5000, 'Standard 30-day term loan with 5% flat interest'),
    ('prod_long', 'Bear Long', 60, 9.00, 500, 15000, 'Extended 60-day enterprise facility with 9% flat interest')
ON CONFLICT (id) DO NOTHING;

-- 8. LOAN APPLICATIONS (4-Stage processing)
CREATE TABLE IF NOT EXISTS public.loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL REFERENCES public.loan_products(id),
    product_name TEXT NOT NULL,
    requested_amount BIGINT NOT NULL CHECK (requested_amount > 0),
    current_stage INT NOT NULL DEFAULT 1 CHECK (current_stage BETWEEN 1 AND 4),
    stage_status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK (stage_status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED', 'ACCEPTED')),
    eligibility_status TEXT NOT NULL DEFAULT 'Pending',
    credit_score INT NOT NULL DEFAULT 600,
    credit_rating TEXT NOT NULL DEFAULT 'Fair',
    liquidity_status TEXT NOT NULL DEFAULT 'Checking',
    available_bank_liquidity BIGINT NOT NULL DEFAULT 0,
    approved_principal BIGINT NOT NULL DEFAULT 0,
    interest_rate_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    interest_amount BIGINT NOT NULL DEFAULT 0,
    total_repayment BIGINT NOT NULL DEFAULT 0,
    tenor_days INT NOT NULL DEFAULT 30,
    due_at TIMESTAMPTZ,
    decision_reason TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 9. ACTIVE LOANS & REPAYMENTS
CREATE TABLE IF NOT EXISTS public.loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES public.loan_applications(id),
    product_name TEXT NOT NULL,
    principal_amount BIGINT NOT NULL CHECK (principal_amount > 0),
    interest_amount BIGINT NOT NULL CHECK (interest_amount >= 0),
    total_amount BIGINT NOT NULL CHECK (total_amount >= principal_amount),
    remaining_amount BIGINT NOT NULL CHECK (remaining_amount >= 0),
    tenor_days INT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'OVERDUE', 'PAID')),
    due_at TIMESTAMPTZ NOT NULL,
    disbursed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE TABLE IF NOT EXISTS public.loan_repayments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES public.loans(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL CHECK (amount > 0),
    principal_portion BIGINT NOT NULL CHECK (principal_portion >= 0),
    interest_portion BIGINT NOT NULL CHECK (interest_portion >= 0),
    repaid_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 10. NOTIFICATIONS & AUDIT LOGS
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_email TEXT NOT NULL,
    action TEXT NOT NULL,
    target_entity TEXT NOT NULL,
    before_value TEXT,
    after_value TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ledger_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.central_bank_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exchange_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exchange_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loan_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loan_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loan_repayments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Public can read usernames & handles for transfer discovery
CREATE POLICY "Public profile lookup" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "User update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Wallets: Users can only read their own wallet
CREATE POLICY "Users read own wallet" ON public.wallets FOR SELECT USING (auth.uid() = user_id);

-- System accounts: read-only for all authenticated
CREATE POLICY "Authenticated read system accounts" ON public.system_accounts FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated read settings" ON public.central_bank_settings FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated read rates" ON public.exchange_rates FOR SELECT TO authenticated USING (true);
CREATE POLICY "Authenticated read loan products" ON public.loan_products FOR SELECT TO authenticated USING (true);

-- Ledger: Users can only read transactions they are involved in
CREATE POLICY "Users read own transactions" ON public.ledger_transactions FOR SELECT TO authenticated
    USING (source_account = auth.uid()::text OR destination_account = auth.uid()::text);

-- Loans: Users read own loan apps, active loans, and repayments
CREATE POLICY "Users read own loan apps" ON public.loan_applications FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users read own loans" ON public.loans FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users read own repayments" ON public.loan_repayments FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM public.loans l WHERE l.id = loan_repayments.loan_id AND l.user_id = auth.uid()));

-- Notifications
CREATE POLICY "Users read own notifications" ON public.notifications FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users update own notifications" ON public.notifications FOR UPDATE TO authenticated USING (auth.uid() = user_id);

-- Audit logs: Read by admins or authenticated
CREATE POLICY "Authenticated read audit logs" ON public.audit_logs FOR SELECT TO authenticated USING (true);

-- ============================================================================
-- SECURE TRANSACTION FUNCTIONS (SECURITY DEFINER)
-- ============================================================================

-- Function 1: Handle User Signup (Initial 500 B bonus from finite distribution pool)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_bonus BIGINT := 500;
    v_pool_balance BIGINT;
    v_username TEXT;
BEGIN
    v_username := COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1));

    -- Create profile
    INSERT INTO public.profiles (id, email, username, full_name, role, credit_score)
    VALUES (
        NEW.id,
        NEW.email,
        v_username,
        COALESCE(NEW.raw_user_meta_data->>'full_name', v_username),
        'user',
        600
    ) ON CONFLICT (id) DO NOTHING;

    -- Create wallet with initial 0 B balance (user requests zero initial balance)
    INSERT INTO public.wallets (user_id, currency, balance)
    VALUES (NEW.id, 'BEAR', 0)
    ON CONFLICT (user_id, currency) DO NOTHING;

    RETURN NEW;
END;
$$;

-- Trigger on auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Function 2: Atomic Transfer between Users
CREATE OR REPLACE FUNCTION public.execute_transfer(
    p_recipient_handle TEXT,
    p_amount BIGINT,
    p_idempotency_key TEXT,
    p_note TEXT DEFAULT ''
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_sender_id UUID;
    v_recipient_id UUID;
    v_sender_balance BIGINT;
    v_existing_tx UUID;
BEGIN
    v_sender_id := auth.uid();
    IF v_sender_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: User must be signed in';
    END IF;

    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Invalid transfer amount: Amount must be greater than 0';
    END IF;

    -- Idempotency check
    SELECT id INTO v_existing_tx
    FROM public.ledger_transactions
    WHERE idempotency_key = p_idempotency_key;

    IF v_existing_tx IS NOT NULL THEN
        RETURN json_build_object('success', true, 'status', 'DUPLICATE_IGNORED', 'tx_id', v_existing_tx);
    END IF;

    -- Find recipient by username (case-insensitive) or email
    SELECT id INTO v_recipient_id
    FROM public.profiles
    WHERE lower(username) = lower(trim(leading '@' from p_recipient_handle))
       OR lower(email) = lower(p_recipient_handle);

    IF v_recipient_id IS NULL THEN
        RAISE EXCEPTION 'Recipient % not found', p_recipient_handle;
    END IF;

    IF v_sender_id = v_recipient_id THEN
        RAISE EXCEPTION 'Cannot transfer Bear to yourself';
    END IF;

    -- Concurrency-safe balance lock on sender wallet
    SELECT balance INTO v_sender_balance
    FROM public.wallets
    WHERE user_id = v_sender_id AND currency = 'BEAR'
    FOR UPDATE;

    IF v_sender_balance IS NULL OR v_sender_balance < p_amount THEN
        RAISE EXCEPTION 'Insufficient Bear balance. Available: % B, Requested: % B', COALESCE(v_sender_balance, 0), p_amount;
    END IF;

    -- Atomic Debit Sender
    UPDATE public.wallets
    SET balance = balance - p_amount, updated_at = now()
    WHERE user_id = v_sender_id AND currency = 'BEAR';

    -- Atomic Credit Recipient
    UPDATE public.wallets
    SET balance = balance + p_amount, updated_at = now()
    WHERE user_id = v_recipient_id AND currency = 'BEAR';

    -- Record transaction
    INSERT INTO public.ledger_transactions (
        idempotency_key, transaction_type, amount, fee,
        source_account, destination_account, note, status
    ) VALUES (
        p_idempotency_key,
        'TRANSFER',
        p_amount,
        0,
        v_sender_id::text,
        v_recipient_id::text,
        p_note,
        'COMPLETED'
    );

    -- Notify recipient
    INSERT INTO public.notifications (user_id, title, message, type)
    VALUES (
        v_recipient_id,
        'Incoming Bear Transfer',
        'You received ' || p_amount || ' B from @' || (SELECT username FROM public.profiles WHERE id = v_sender_id),
        'TRANSFER'
    );

    RETURN json_build_object(
        'success', true,
        'amount', p_amount,
        'recipient', p_recipient_handle,
        'new_balance', v_sender_balance - p_amount
    );
END;
$$;

-- Function 3: Virtual Top-Up from finite distribution pool
CREATE OR REPLACE FUNCTION public.execute_topup(
    p_amount BIGINT,
    p_idempotency_key TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_pool_balance BIGINT;
    v_existing_tx UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    IF p_amount <= 0 OR p_amount > 500 THEN
        RAISE EXCEPTION 'Top-up limit: between 1 and 500 B per request';
    END IF;

    -- Idempotency
    SELECT id INTO v_existing_tx
    FROM public.ledger_transactions
    WHERE idempotency_key = p_idempotency_key;

    IF v_existing_tx IS NOT NULL THEN
        RETURN json_build_object('success', true, 'status', 'DUPLICATE_IGNORED');
    END IF;

    -- Lock system distribution pool
    SELECT balance INTO v_pool_balance
    FROM public.system_accounts
    WHERE account_code = 'SYS_USER_DISTRIBUTION_POOL'
    FOR UPDATE;

    IF v_pool_balance < p_amount THEN
        RAISE EXCEPTION 'Distribution inventory exhausted. Available in pool: % B', v_pool_balance;
    END IF;

    -- Debit pool, credit user
    UPDATE public.system_accounts
    SET balance = balance - p_amount, updated_at = now()
    WHERE account_code = 'SYS_USER_DISTRIBUTION_POOL';

    UPDATE public.wallets
    SET balance = balance + p_amount, updated_at = now()
    WHERE user_id = v_user_id AND currency = 'BEAR';

    INSERT INTO public.ledger_transactions (
        idempotency_key, transaction_type, amount, fee,
        source_account, destination_account, note, status
    ) VALUES (
        p_idempotency_key,
        'TOPUP',
        p_amount,
        0,
        'SYS_USER_DISTRIBUTION_POOL',
        v_user_id::text,
        'Simulated community faucet top-up',
        'COMPLETED'
    );

    RETURN json_build_object('success', true, 'amount', p_amount);
END;
$$;

-- Function 4: Virtual Exchange Trade (BUY or SELL BEAR)
CREATE OR REPLACE FUNCTION public.execute_exchange_trade(
    p_order_type TEXT,
    p_bear_amount BIGINT,
    p_idempotency_key TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_current_rate NUMERIC(12, 4);
    v_is_paused BOOLEAN;
    v_exchange_inventory BIGINT;
    v_user_balance BIGINT;
    v_idr_amount BIGINT;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    -- Check market state
    SELECT current_rate_idr, is_market_paused
    INTO v_current_rate, v_is_paused
    FROM public.central_bank_settings
    WHERE id = 1;

    IF v_is_paused THEN
        RAISE EXCEPTION 'The Bear Exchange is currently paused by the Central Bank';
    END IF;

    v_idr_amount := ROUND(p_bear_amount * v_current_rate);

    IF p_order_type = 'BUY' THEN
        -- Check system exchange inventory
        SELECT balance INTO v_exchange_inventory
        FROM public.system_accounts
        WHERE account_code = 'SYS_EXCHANGE_INVENTORY'
        FOR UPDATE;

        IF v_exchange_inventory < p_bear_amount THEN
            RAISE EXCEPTION 'Exchange inventory insufficient. Available: % B', v_exchange_inventory;
        END IF;

        -- Move BEAR from exchange inventory to user wallet
        UPDATE public.system_accounts
        SET balance = balance - p_bear_amount, updated_at = now()
        WHERE account_code = 'SYS_EXCHANGE_INVENTORY';

        UPDATE public.wallets
        SET balance = balance + p_bear_amount, updated_at = now()
        WHERE user_id = v_user_id AND currency = 'BEAR';

        INSERT INTO public.ledger_transactions (
            idempotency_key, transaction_type, amount, fee,
            source_account, destination_account, note, status
        ) VALUES (
            p_idempotency_key,
            'EXCHANGE_BUY',
            p_bear_amount,
            0,
            'SYS_EXCHANGE_INVENTORY',
            v_user_id::text,
            'Purchased at simulated rate ' || v_current_rate || ' IDR/BEAR',
            'COMPLETED'
        );

    ELSIF p_order_type = 'SELL' THEN
        -- Check user balance
        SELECT balance INTO v_user_balance
        FROM public.wallets
        WHERE user_id = v_user_id AND currency = 'BEAR'
        FOR UPDATE;

        IF v_user_balance < p_bear_amount THEN
            RAISE EXCEPTION 'Insufficient Bear balance to sell. Available: % B', v_user_balance;
        END IF;

        -- Move BEAR from user wallet back to exchange inventory
        UPDATE public.wallets
        SET balance = balance - p_bear_amount, updated_at = now()
        WHERE user_id = v_user_id AND currency = 'BEAR';

        UPDATE public.system_accounts
        SET balance = balance + p_bear_amount, updated_at = now()
        WHERE account_code = 'SYS_EXCHANGE_INVENTORY';

        INSERT INTO public.ledger_transactions (
            idempotency_key, transaction_type, amount, fee,
            source_account, destination_account, note, status
        ) VALUES (
            p_idempotency_key,
            'EXCHANGE_SELL',
            p_bear_amount,
            0,
            v_user_id::text,
            'SYS_EXCHANGE_INVENTORY',
            'Sold at simulated rate ' || v_current_rate || ' IDR/BEAR',
            'COMPLETED'
        );
    ELSE
        RAISE EXCEPTION 'Invalid order type: %', p_order_type;
    END IF;

    -- Record exchange order
    INSERT INTO public.exchange_orders (user_id, order_type, bear_amount, idr_amount, rate_applied)
    VALUES (v_user_id, p_order_type, p_bear_amount, v_idr_amount, v_current_rate);

    RETURN json_build_object(
        'success', true,
        'order_type', p_order_type,
        'bear_amount', p_bear_amount,
        'idr_amount', v_idr_amount,
        'rate', v_current_rate
    );
END;
$$;

-- Function 5: Submit Loan Application (Initiates Stage 1)
CREATE OR REPLACE FUNCTION public.submit_loan_application(
    p_product_id TEXT,
    p_requested_amount BIGINT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_product public.loan_products%ROWTYPE;
    v_active_loans_count INT;
    v_user_profile public.profiles%ROWTYPE;
    v_app_id UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    SELECT * INTO v_product FROM public.loan_products WHERE id = p_product_id;
    IF v_product.id IS NULL THEN
        RAISE EXCEPTION 'Product % not found', p_product_id;
    END IF;

    IF p_requested_amount < v_product.min_amount OR p_requested_amount > v_product.max_amount THEN
        RAISE EXCEPTION 'Requested amount must be between % and % B', v_product.min_amount, v_product.max_amount;
    END IF;

    -- Check for existing active or overdue loans
    SELECT count(*) INTO v_active_loans_count
    FROM public.loans
    WHERE user_id = v_user_id AND status IN ('ACTIVE', 'OVERDUE');

    IF v_active_loans_count > 0 THEN
        RAISE EXCEPTION 'You already have an outstanding active loan. Repay it before applying for a new loan.';
    END IF;

    SELECT * INTO v_user_profile FROM public.profiles WHERE id = v_user_id;

    -- Insert application at Stage 1
    INSERT INTO public.loan_applications (
        user_id, product_id, product_name, requested_amount,
        current_stage, stage_status, eligibility_status, credit_score,
        credit_rating, tenor_days, interest_rate_percent
    ) VALUES (
        v_user_id,
        v_product.id,
        v_product.name,
        p_requested_amount,
        1,
        'IN_PROGRESS',
        'Screening in progress',
        COALESCE(v_user_profile.credit_score, 600),
        CASE
            WHEN COALESCE(v_user_profile.credit_score, 600) >= 750 THEN 'Excellent'
            WHEN COALESCE(v_user_profile.credit_score, 600) >= 650 THEN 'Good'
            ELSE 'Fair'
        END,
        v_product.tenor_days,
        v_product.interest_rate_percent
    ) RETURNING id INTO v_app_id;

    RETURN json_build_object('success', true, 'application_id', v_app_id, 'stage', 1);
END;
$$;

-- Function 6: Process 4-Stage Loan Application Progression
CREATE OR REPLACE FUNCTION public.process_loan_stage(p_application_id UUID)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_app public.loan_applications%ROWTYPE;
    v_lending_pool_balance BIGINT;
    v_interest_amount BIGINT;
    v_total_repayment BIGINT;
BEGIN
    SELECT * INTO v_app FROM public.loan_applications WHERE id = p_application_id FOR UPDATE;
    IF v_app.id IS NULL THEN
        RAISE EXCEPTION 'Application not found';
    END IF;

    IF v_app.stage_status != 'IN_PROGRESS' THEN
        RETURN json_build_object('application_id', p_application_id, 'stage', v_app.current_stage, 'status', v_app.stage_status);
    END IF;

    -- Check Lending Pool Balance
    SELECT balance INTO v_lending_pool_balance
    FROM public.system_accounts
    WHERE account_code = 'SYS_BANK_LENDING_POOL';

    IF v_app.current_stage = 1 THEN
        -- Complete Stage 1 -> Advance to Stage 2
        UPDATE public.loan_applications
        SET current_stage = 2,
            eligibility_status = 'Verified: No overdue debt, authenticated account',
            updated_at = now()
        WHERE id = p_application_id;
        RETURN json_build_object('application_id', p_application_id, 'stage', 2, 'status', 'IN_PROGRESS');

    ELSIF v_app.current_stage = 2 THEN
        -- Complete Stage 2 -> Advance to Stage 3
        UPDATE public.loan_applications
        SET current_stage = 3,
            liquidity_status = 'Evaluating bank lending reserve...',
            available_bank_liquidity = v_lending_pool_balance,
            updated_at = now()
        WHERE id = p_application_id;
        RETURN json_build_object('application_id', p_application_id, 'stage', 3, 'status', 'IN_PROGRESS');

    ELSIF v_app.current_stage = 3 THEN
        -- Evaluate Liquidity & Advance to Stage 4 (Decision)
        IF v_lending_pool_balance < v_app.requested_amount THEN
            -- Insufficient bank pool liquidity
            UPDATE public.loan_applications
            SET current_stage = 4,
                stage_status = 'REJECTED',
                liquidity_status = 'Insufficient bank liquidity',
                decision_reason = 'Bank lending pool liquidity is currently constrained (' || v_lending_pool_balance || ' B available).',
                updated_at = now()
            WHERE id = p_application_id;
            RETURN json_build_object('application_id', p_application_id, 'stage', 4, 'status', 'REJECTED');
        ELSE
            -- Liquidity OK! Compute loan terms
            v_interest_amount := ROUND(v_app.requested_amount * (v_app.interest_rate_percent / 100.0));
            v_total_repayment := v_app.requested_amount + v_interest_amount;

            UPDATE public.loan_applications
            SET current_stage = 4,
                stage_status = 'APPROVED',
                liquidity_status = 'Sufficient liquidity verified (' || v_lending_pool_balance || ' B available)',
                approved_principal = v_app.requested_amount,
                interest_amount = v_interest_amount,
                total_repayment = v_total_repayment,
                due_at = now() + (v_app.tenor_days || ' days')::interval,
                decision_reason = 'Credit score and bank liquidity criteria met. Offer ready for review.',
                updated_at = now()
            WHERE id = p_application_id;
            RETURN json_build_object('application_id', p_application_id, 'stage', 4, 'status', 'APPROVED');
        END IF;
    END IF;

    RETURN json_build_object('application_id', p_application_id, 'stage', v_app.current_stage, 'status', v_app.stage_status);
END;
$$;

-- Function 7: Explicit Loan Disbursement
CREATE OR REPLACE FUNCTION public.disburse_loan(p_application_id UUID)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_app public.loan_applications%ROWTYPE;
    v_lending_pool_balance BIGINT;
    v_loan_id UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    SELECT * INTO v_app FROM public.loan_applications WHERE id = p_application_id AND user_id = v_user_id FOR UPDATE;
    IF v_app.id IS NULL THEN
        RAISE EXCEPTION 'Application not found';
    END IF;

    IF v_app.stage_status != 'APPROVED' THEN
        RAISE EXCEPTION 'Loan application is not in APPROVED state (current: %)', v_app.stage_status;
    END IF;

    -- Lock lending pool
    SELECT balance INTO v_lending_pool_balance
    FROM public.system_accounts
    WHERE account_code = 'SYS_BANK_LENDING_POOL'
    FOR UPDATE;

    IF v_lending_pool_balance < v_app.approved_principal THEN
        RAISE EXCEPTION 'Bank lending pool liquidity has changed and is now insufficient.';
    END IF;

    -- Debit Bank Lending Pool
    UPDATE public.system_accounts
    SET balance = balance - v_app.approved_principal, updated_at = now()
    WHERE account_code = 'SYS_BANK_LENDING_POOL';

    -- Credit User Wallet
    UPDATE public.wallets
    SET balance = balance + v_app.approved_principal, updated_at = now()
    WHERE user_id = v_user_id AND currency = 'BEAR';

    -- Create Active Loan record
    INSERT INTO public.loans (
        user_id, application_id, product_name, principal_amount,
        interest_amount, total_amount, remaining_amount, tenor_days,
        status, due_at
    ) VALUES (
        v_user_id,
        v_app.id,
        v_app.product_name,
        v_app.approved_principal,
        v_app.interest_amount,
        v_app.total_repayment,
        v_app.total_repayment,
        v_app.tenor_days,
        'ACTIVE',
        v_app.due_at
    ) RETURNING id INTO v_loan_id;

    -- Mark Application ACCEPTED
    UPDATE public.loan_applications
    SET stage_status = 'ACCEPTED', updated_at = now()
    WHERE id = p_application_id;

    -- Record in Immutable Ledger
    INSERT INTO public.ledger_transactions (
        idempotency_key, transaction_type, amount, fee,
        source_account, destination_account, note, status
    ) VALUES (
        'disburse_' || v_app.id::text,
        'LOAN_DISBURSEMENT',
        v_app.approved_principal,
        0,
        'SYS_BANK_LENDING_POOL',
        v_user_id::text,
        'Loan disbursed for product ' || v_app.product_name,
        'COMPLETED'
    );

    RETURN json_build_object('success', true, 'loan_id', v_loan_id, 'amount', v_app.approved_principal);
END;
$$;

-- Function 8: Loan Repayment with Principal & Interest routing
CREATE OR REPLACE FUNCTION public.repay_loan(
    p_loan_id UUID,
    p_amount BIGINT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_loan public.loans%ROWTYPE;
    v_user_balance BIGINT;
    v_principal_portion BIGINT;
    v_interest_portion BIGINT;
    v_new_remaining BIGINT;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;

    SELECT * INTO v_loan FROM public.loans WHERE id = p_loan_id AND user_id = v_user_id FOR UPDATE;
    IF v_loan.id IS NULL THEN
        RAISE EXCEPTION 'Loan not found';
    END IF;

    IF v_loan.status = 'PAID' OR v_loan.remaining_amount = 0 THEN
        RAISE EXCEPTION 'This loan is already fully settled.';
    END IF;

    IF p_amount <= 0 OR p_amount > v_loan.remaining_amount THEN
        RAISE EXCEPTION 'Repayment amount must be between 1 and % B', v_loan.remaining_amount;
    END IF;

    -- Check user balance
    SELECT balance INTO v_user_balance
    FROM public.wallets
    WHERE user_id = v_user_id AND currency = 'BEAR'
    FOR UPDATE;

    IF v_user_balance < p_amount THEN
        RAISE EXCEPTION 'Insufficient Bear balance. Required: % B, Available: % B', p_amount, v_user_balance;
    END IF;

    -- Proportionate split between principal and interest
    v_interest_portion := ROUND(p_amount * (v_loan.interest_amount::numeric / v_loan.total_amount::numeric));
    v_principal_portion := p_amount - v_interest_portion;

    -- Debit user wallet
    UPDATE public.wallets
    SET balance = balance - p_amount, updated_at = now()
    WHERE user_id = v_user_id AND currency = 'BEAR';

    -- Route principal back to bank lending pool
    UPDATE public.system_accounts
    SET balance = balance + v_principal_portion, updated_at = now()
    WHERE account_code = 'SYS_BANK_LENDING_POOL';

    -- Route interest to bank interest income account
    UPDATE public.system_accounts
    SET balance = balance + v_interest_portion, updated_at = now()
    WHERE account_code = 'SYS_BANK_INTEREST_INCOME';

    v_new_remaining := v_loan.remaining_amount - p_amount;

    UPDATE public.loans
    SET remaining_amount = v_new_remaining,
        status = CASE WHEN v_new_remaining = 0 THEN 'PAID' ELSE 'ACTIVE' END
    WHERE id = p_loan_id;

    -- Record repayment
    INSERT INTO public.loan_repayments (loan_id, amount, principal_portion, interest_portion)
    VALUES (p_loan_id, p_amount, v_principal_portion, v_interest_portion);

    -- Record in Ledger
    INSERT INTO public.ledger_transactions (
        idempotency_key, transaction_type, amount, fee,
        source_account, destination_account, note, status
    ) VALUES (
        'repay_' || gen_random_uuid()::text,
        'LOAN_REPAYMENT',
        p_amount,
        0,
        v_user_id::text,
        'SYS_BANK_LENDING_POOL',
        'Loan repayment (Principal: ' || v_principal_portion || ' B, Interest: ' || v_interest_portion || ' B)',
        'COMPLETED'
    );

    -- Increase credit score on full settlement
    IF v_new_remaining = 0 THEN
        UPDATE public.profiles
        SET credit_score = LEAST(850, credit_score + 25)
        WHERE id = v_user_id;
    END IF;

    RETURN json_build_object(
        'success', true,
        'repaid', p_amount,
        'remaining', v_new_remaining,
        'is_fully_paid', (v_new_remaining = 0)
    );
END;
$$;

-- Function 9: Dynamic Exchange Rate Update
CREATE OR REPLACE FUNCTION public.update_market_rate(p_market_event TEXT DEFAULT 'Scheduled Market Equilibrium')
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_old_rate NUMERIC(12, 4);
    v_change_pct NUMERIC(8, 4);
    v_new_rate NUMERIC(12, 4);
    v_random_factor NUMERIC(8, 4);
BEGIN
    SELECT current_rate_idr INTO v_old_rate FROM public.central_bank_settings WHERE id = 1;

    -- Pseudo-random market shock bounded between -2.8% and +2.8%
    v_random_factor := ((random() * 5.6) - 2.8)::numeric(8, 4);
    v_change_pct := v_random_factor;
    v_new_rate := ROUND(v_old_rate * (1.0 + (v_change_pct / 100.0)), 4);

    -- Floor guardrail
    IF v_new_rate < 1.0000 THEN
        v_new_rate := 1.0000;
    END IF;

    UPDATE public.central_bank_settings
    SET current_rate_idr = v_new_rate, updated_at = now()
    WHERE id = 1;

    INSERT INTO public.exchange_rates (rate_idr, previous_rate, change_percent, market_event)
    VALUES (v_new_rate, v_old_rate, v_change_pct, p_market_event);

    RETURN json_build_object('success', true, 'new_rate', v_new_rate, 'change_pct', v_change_pct);
END;
$$;
