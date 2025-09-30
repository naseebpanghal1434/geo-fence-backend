-- Phase 5: Punch Request table
-- Stores manager/admin-created punch requests targeting users, teams, projects, or orgs

CREATE TABLE IF NOT EXISTS punch_request (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,

    entity_type_id INTEGER NOT NULL CHECK (entity_type_id IN (1, 2, 4, 5)), -- 1=USER, 2=ORG, 4=PROJECT, 5=TEAM
    entity_id BIGINT NOT NULL,                                               -- userId/teamId/projectId/orgId

    requester_account_id BIGINT NOT NULL,
    requested_datetime TIMESTAMPTZ NOT NULL,                                 -- server UTC
    respond_within_minutes INTEGER NOT NULL CHECK (respond_within_minutes > 0),
    expires_at TIMESTAMPTZ NOT NULL,                                         -- requested_datetime + respondWithinMinutes

    state TEXT NOT NULL CHECK (state IN ('PENDING', 'FULFILLED', 'EXPIRED', 'CANCELLED')) DEFAULT 'PENDING',

    created_datetime TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_datetime TIMESTAMPTZ
);

-- Index for org + state queries
CREATE INDEX IF NOT EXISTS idx_pr_org_state ON punch_request(org_id, state);

-- Index for time window queries
CREATE INDEX IF NOT EXISTS idx_pr_org_window ON punch_request(org_id, requested_datetime, expires_at);

-- Index for target entity lookups
CREATE INDEX IF NOT EXISTS idx_pr_target ON punch_request(org_id, entity_type_id, entity_id);
