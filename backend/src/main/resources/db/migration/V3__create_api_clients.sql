CREATE TABLE api_clients (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    client_id     VARCHAR(100) UNIQUE NOT NULL,
    secret_hash   VARCHAR(255) NOT NULL,
    scopes        TEXT         NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_used_at  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE api_client_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id           VARCHAR(100) NOT NULL REFERENCES api_clients(client_id) ON DELETE CASCADE,
    access_token_hash   VARCHAR(255) UNIQUE NOT NULL,
    refresh_token_hash  VARCHAR(255) UNIQUE NOT NULL,
    scopes              TEXT         NOT NULL,
    access_expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_client_tokens_access  ON api_client_tokens(access_token_hash);
CREATE INDEX idx_api_client_tokens_refresh ON api_client_tokens(refresh_token_hash);
