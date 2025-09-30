-- Phase 6a: Attendance Events and Day Rollups
-- Stores punch events (CHECK_IN/OUT, BREAK_START/END, PUNCHED) and daily attendance rollups

CREATE TABLE IF NOT EXISTS attendance_event (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,

    event_kind TEXT NOT NULL CHECK (event_kind IN ('CHECK_IN', 'CHECK_OUT', 'BREAK_START', 'BREAK_END', 'PUNCHED')),
    event_source TEXT NOT NULL CHECK (event_source IN ('MANUAL', 'GEOFENCE', 'WIFI', 'SUPERVISOR')),
    event_action TEXT NOT NULL CHECK (event_action IN ('MANUAL', 'AUTO')),

    ts_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    client_local_ts TIMESTAMPTZ,
    client_tz TEXT,

    fence_id BIGINT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    accuracy_m DOUBLE PRECISION,
    under_range BOOLEAN,                     -- inside a valid fence or not

    success BOOLEAN NOT NULL,                -- true=accepted (pass or warn); false=failed
    verdict TEXT NOT NULL CHECK (verdict IN ('PASS', 'WARN', 'FAIL')),
    fail_reason TEXT,                        -- ExceptionCode or null
    flags JSONB NOT NULL DEFAULT '{}'::JSONB, -- extra signals (low_accuracy, out_of_fence, etc.)

    punch_request_id BIGINT,                 -- optional linkage for PUNCHED
    requester_account_id BIGINT,             -- manager who requested (if any)

    idempotency_key TEXT,
    created_datetime TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for org + account + time queries
CREATE INDEX IF NOT EXISTS idx_att_event_org_acc_ts ON attendance_event(org_id, account_id, ts_utc DESC);

-- Index for event kind queries
CREATE INDEX IF NOT EXISTS idx_att_event_org_kind ON attendance_event(org_id, event_kind);

-- Unique index for idempotency
CREATE UNIQUE INDEX IF NOT EXISTS uq_att_event_idem ON attendance_event(org_id, account_id, event_kind, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Daily attendance rollup table
CREATE TABLE IF NOT EXISTS attendance_day (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    date_key DATE NOT NULL,                  -- anchored to office start in operational TZ
    first_in_utc TIMESTAMPTZ,
    last_out_utc TIMESTAMPTZ,
    worked_seconds INTEGER NOT NULL DEFAULT 0,
    break_seconds INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'INCOMPLETE', 'FLAGGED', 'REQUIRES_REVIEW')) DEFAULT 'ABSENT',
    anomalies JSONB NOT NULL DEFAULT '{}'::JSONB,
    UNIQUE (org_id, account_id, date_key)
);

-- Index for org + date queries
CREATE INDEX IF NOT EXISTS idx_att_day_org_date ON attendance_day(org_id, date_key);

-- Index for org + account queries
CREATE INDEX IF NOT EXISTS idx_att_day_org_acc ON attendance_day(org_id, account_id);
