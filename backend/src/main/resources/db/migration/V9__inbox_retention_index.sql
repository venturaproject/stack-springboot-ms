-- #3 retention sweep for the inbox dedupe table scans by age.
CREATE INDEX idx_inbox_processed_at ON inbox_processed_events (processed_at);
