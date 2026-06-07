-- Add moderation state for users and feed posts, plus reports table

ALTER TABLE users
ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user';

ALTER TABLE users
ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'active';

ALTER TABLE users
ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS locked_reason TEXT;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS locked_by INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'users_role_check'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT users_role_check
        CHECK (role IN ('user', 'admin'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'users_account_status_check'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT users_account_status_check
        CHECK (account_status IN ('active', 'locked'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'users_locked_by_fkey'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT users_locked_by_fkey
        FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE collection_items
ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(20) NOT NULL DEFAULT 'active';

ALTER TABLE collection_items
ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ;

ALTER TABLE collection_items
ADD COLUMN IF NOT EXISTS locked_reason TEXT;

ALTER TABLE collection_items
ADD COLUMN IF NOT EXISTS locked_by INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'collection_items_moderation_status_check'
    ) THEN
        ALTER TABLE collection_items
        ADD CONSTRAINT collection_items_moderation_status_check
        CHECK (moderation_status IN ('active', 'locked'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'collection_items_locked_by_fkey'
    ) THEN
        ALTER TABLE collection_items
        ADD CONSTRAINT collection_items_locked_by_fkey
        FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS reports (
    id SERIAL PRIMARY KEY,
    reporter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(30) NOT NULL,
    target_id INTEGER NOT NULL,
    reason_code VARCHAR(30) NOT NULL,
    reason_detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    resolution_note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'reports_target_type_check'
    ) THEN
        ALTER TABLE reports
        ADD CONSTRAINT reports_target_type_check
        CHECK (target_type IN ('user', 'collection_item'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'reports_status_check'
    ) THEN
        ALTER TABLE reports
        ADD CONSTRAINT reports_status_check
        CHECK (status IN ('pending', 'reviewed', 'dismissed', 'actioned'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_account_status
ON users(account_status);

CREATE INDEX IF NOT EXISTS idx_collection_items_moderation_status
ON collection_items(moderation_status);

CREATE INDEX IF NOT EXISTS idx_reports_target
ON reports(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_reports_status_created_at
ON reports(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_reporter_created_at
ON reports(reporter_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_reports_one_pending_per_reporter_target
ON reports(reporter_id, target_type, target_id)
WHERE status = 'pending';
