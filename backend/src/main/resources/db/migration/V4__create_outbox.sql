-- Transactional outbox: domain events are written here in the same transaction as the
-- aggregate that produced them, then a relay publishes them after commit. This removes
-- the dual-write problem (DB commit + broker publish can't both fail independently).

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    event_type     VARCHAR(200) NOT NULL,
    aggregate_type VARCHAR(200) NOT NULL,
    payload        TEXT         NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMP WITH TIME ZONE,
    attempts       INTEGER NOT NULL DEFAULT 0
);

-- The relay only ever scans for unpublished rows.
CREATE INDEX idx_outbox_unpublished ON outbox_events (occurred_at) WHERE published_at IS NULL;
