-- Poison-message handling: after too many failed publish attempts a row is dead-lettered
-- (failed_at set) so it stops blocking the relay. Inspect / replay it out of band.
ALTER TABLE outbox_events ADD COLUMN failed_at TIMESTAMP WITH TIME ZONE;

DROP INDEX idx_outbox_unpublished;
CREATE INDEX idx_outbox_unpublished ON outbox_events (occurred_at)
    WHERE published_at IS NULL AND failed_at IS NULL;
