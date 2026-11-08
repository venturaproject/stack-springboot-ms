-- Inbox: the consumer-side counterpart to the outbox. The outbox only promises
-- at-least-once delivery, so any consumer whose reaction is more than a log line must
-- dedupe redeliveries. A claim is a single-row insert per (consumer, event_id); a retried
-- or duplicated delivery hits the unique constraint and is skipped.
CREATE TABLE inbox_processed_events (
    id           UUID PRIMARY KEY,
    consumer     VARCHAR(200) NOT NULL,
    event_id     UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inbox_consumer_event UNIQUE (consumer, event_id)
);
