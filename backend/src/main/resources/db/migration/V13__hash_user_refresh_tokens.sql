-- Refresh tokens are random opaque credentials: retain the ability to validate them while
-- ensuring a database read does not disclose usable bearer credentials.
ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;
UPDATE refresh_tokens SET token_hash = encode(digest(token_hash, 'sha256'), 'hex');
ALTER INDEX idx_refresh_tokens_token RENAME TO idx_refresh_tokens_token_hash;
