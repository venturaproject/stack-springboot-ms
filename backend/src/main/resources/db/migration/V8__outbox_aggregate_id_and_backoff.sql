-- #1 per-aggregate ordering: the Kafka message key is now the aggregate id, so an event
-- needs to carry it. Existing rows (dev only) get their own id as a harmless placeholder.
ALTER TABLE outbox_events ADD COLUMN aggregate_id UUID;
UPDATE outbox_events SET aggregate_id = id WHERE aggregate_id IS NULL;
ALTER TABLE outbox_events ALTER COLUMN aggregate_id SET NOT NULL;

-- #4 exponential backoff: a failed publish is parked until next_attempt_at instead of
-- being retried every tick.
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;

DROP INDEX idx_outbox_unpublished;
CREATE INDEX idx_outbox_due ON outbox_events (occurred_at)
    WHERE published_at IS NULL AND failed_at IS NULL;

-- #3 retention sweep scans published rows by age.
CREATE INDEX idx_outbox_published_at ON outbox_events (published_at)
    WHERE published_at IS NOT NULL;
