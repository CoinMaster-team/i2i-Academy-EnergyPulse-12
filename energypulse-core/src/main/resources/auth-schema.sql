CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_users_email_lower
    ON app_users (LOWER(email));

CREATE TABLE IF NOT EXISTS auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_auth_sessions_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_id
    ON auth_sessions (user_id);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires_at
    ON auth_sessions (expires_at);

ALTER TABLE auth_sessions
    ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE appliances
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE appliances
    DROP CONSTRAINT IF EXISTS uq_appliances_home_name;

ALTER TABLE operational_events
    ADD COLUMN IF NOT EXISTS notification_processed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE operational_events event
SET notification_processed = TRUE
WHERE EXISTS (
    SELECT 1
    FROM ai_notifications notification
    WHERE notification.operational_event_id = event.id
);

DELETE FROM ai_notifications
WHERE id IN (
    SELECT id
    FROM ai_notifications
    ORDER BY created_at DESC, id DESC
    OFFSET 15
);
