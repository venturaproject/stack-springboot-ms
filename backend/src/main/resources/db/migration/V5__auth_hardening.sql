-- Refresh-token families: every login starts a family; a refresh rotates within it.
-- Replaying a revoked token → the whole family is revoked (theft response).
-- refresh_tokens holds only ephemeral session state, so clearing it here is safe.
DELETE FROM refresh_tokens;
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID NOT NULL;
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

-- Account lockout: too many failed logins in a window locks the account temporarily.
ALTER TABLE users ADD COLUMN failed_login_attempts   INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN first_failed_login_at   TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN locked_until            TIMESTAMP WITH TIME ZONE;
