-- EnergyPulse PostgreSQL schema
-- Executed automatically when the PostgreSQL volume is initialized.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE homes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    contact_email VARCHAR(254) NOT NULL,

    energy_quota_kwh NUMERIC(14, 4) NOT NULL,
    budget_limit NUMERIC(14, 2) NOT NULL,
    base_tariff NUMERIC(14, 6) NOT NULL,
    penalty_tariff NUMERIC(14, 6) NOT NULL,

    accumulated_energy_kwh NUMERIC(18, 6) NOT NULL DEFAULT 0,
    accumulated_cost NUMERIC(18, 6) NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_homes_energy_quota_positive
        CHECK (energy_quota_kwh > 0),

    CONSTRAINT chk_homes_budget_limit_positive
        CHECK (budget_limit > 0),

    CONSTRAINT chk_homes_base_tariff_non_negative
        CHECK (base_tariff >= 0),

    CONSTRAINT chk_homes_penalty_tariff
        CHECK (penalty_tariff > base_tariff),

    CONSTRAINT chk_homes_accumulated_energy_non_negative
        CHECK (accumulated_energy_kwh >= 0),

    CONSTRAINT chk_homes_accumulated_cost_non_negative
        CHECK (accumulated_cost >= 0)
);

CREATE INDEX idx_homes_contact_email
    ON homes (contact_email);



CREATE TABLE appliances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,

    safe_limit_watt NUMERIC(12, 2) NOT NULL,
    simulation_min_watt NUMERIC(12, 2) NOT NULL,
    simulation_max_watt NUMERIC(12, 2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appliances_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_appliances_home_name
        UNIQUE (home_id, name),

    CONSTRAINT chk_appliances_safe_limit_positive
        CHECK (safe_limit_watt > 0),

    CONSTRAINT chk_appliances_simulation_min_non_negative
        CHECK (simulation_min_watt >= 0),

    CONSTRAINT chk_appliances_simulation_range
        CHECK (simulation_max_watt > simulation_min_watt)
);

CREATE INDEX idx_appliances_home_id
    ON appliances (home_id);



CREATE TABLE billing_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_id UUID NOT NULL,
    telemetry_event_id UUID NOT NULL,

    energy_delta_kwh NUMERIC(18, 6) NOT NULL,
    tariff_rate NUMERIC(14, 6) NOT NULL,
    cost_delta NUMERIC(18, 6) NOT NULL,

    total_energy_kwh NUMERIC(18, 6) NOT NULL,
    total_cost NUMERIC(18, 6) NOT NULL,
    tariff_state VARCHAR(20) NOT NULL,

    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_billing_ledger_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_billing_ledger_telemetry_event
        UNIQUE (telemetry_event_id),

    CONSTRAINT chk_billing_ledger_energy_delta_positive
        CHECK (energy_delta_kwh > 0),

    CONSTRAINT chk_billing_ledger_tariff_non_negative
        CHECK (tariff_rate >= 0),

    CONSTRAINT chk_billing_ledger_cost_delta_non_negative
        CHECK (cost_delta >= 0),

    CONSTRAINT chk_billing_ledger_total_energy_non_negative
        CHECK (total_energy_kwh >= 0),

    CONSTRAINT chk_billing_ledger_total_cost_non_negative
        CHECK (total_cost >= 0),

    CONSTRAINT chk_billing_ledger_tariff_state
        CHECK (tariff_state IN ('NORMAL', 'PENALTY'))
);

CREATE INDEX idx_billing_ledger_home_recorded_at
    ON billing_ledger (home_id, recorded_at DESC);


CREATE TABLE consumption_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_id UUID NOT NULL,

    total_energy_kwh NUMERIC(18, 6) NOT NULL,
    total_cost NUMERIC(18, 6) NOT NULL,

    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consumption_snapshots_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_consumption_snapshots_home_time
        UNIQUE (home_id, captured_at),

    CONSTRAINT chk_consumption_snapshots_energy_non_negative
        CHECK (total_energy_kwh >= 0),

    CONSTRAINT chk_consumption_snapshots_cost_non_negative
        CHECK (total_cost >= 0)
);

CREATE INDEX idx_consumption_snapshots_home_captured_at
    ON consumption_snapshots (home_id, captured_at DESC);


CREATE TABLE operational_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_id UUID NOT NULL,
    appliance_id UUID,
    source_event_id UUID NOT NULL,

    event_type VARCHAR(50) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::JSONB,

    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_operational_events_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_operational_events_appliance
        FOREIGN KEY (appliance_id)
        REFERENCES appliances (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_operational_events_type
        CHECK (
            event_type IN (
                'QUOTA_80_REACHED',
                'QUOTA_100_REACHED',
                'PENALTY_TARIFF_ACTIVATED',
                'APPLIANCE_ANOMALY_DETECTED',
                'APPLIANCE_RECOVERED'
            )
        ),

    CONSTRAINT chk_operational_events_appliance_required
        CHECK (
            event_type NOT IN (
                'APPLIANCE_ANOMALY_DETECTED',
                'APPLIANCE_RECOVERED'
            )
            OR appliance_id IS NOT NULL
        ),

    CONSTRAINT uq_operational_events_source_type
        UNIQUE (home_id, source_event_id, event_type)
);

CREATE INDEX idx_operational_events_home_occurred_at
    ON operational_events (home_id, occurred_at DESC);

CREATE INDEX idx_operational_events_appliance_occurred_at
    ON operational_events (appliance_id, occurred_at DESC)
    WHERE appliance_id IS NOT NULL;


CREATE TABLE ai_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    home_id UUID NOT NULL,
    operational_event_id UUID NOT NULL,

    recipient_email VARCHAR(254) NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'GEMINI',
    language_code VARCHAR(5) NOT NULL DEFAULT 'tr',

    prompt_text TEXT NOT NULL,
    recommendation_text TEXT NOT NULL,

    generation_status VARCHAR(20) NOT NULL,
    email_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    generation_error TEXT,
    email_error TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,

    CONSTRAINT fk_ai_notifications_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_notifications_operational_event
        FOREIGN KEY (operational_event_id)
        REFERENCES operational_events (id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_ai_notifications_operational_event
        UNIQUE (operational_event_id),

    CONSTRAINT chk_ai_notifications_generation_status
        CHECK (
            generation_status IN (
                'GENERATED',
                'FALLBACK'
            )
        ),

    CONSTRAINT chk_ai_notifications_email_status
        CHECK (
            email_status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_ai_notifications_sent_at
        CHECK (
            email_status <> 'SENT'
            OR sent_at IS NOT NULL
        )
);

CREATE INDEX idx_ai_notifications_home_created_at
    ON ai_notifications (home_id, created_at DESC);

CREATE INDEX idx_ai_notifications_email_status
    ON ai_notifications (email_status);

COMMIT;