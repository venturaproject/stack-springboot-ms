-- @Version on every event-emitting aggregate root (AbstractAggregateRoot). A stale
-- update now fails loudly with OptimisticLockingFailureException (-> HTTP 409) instead of
-- a last-writer-wins clobber — matters most where the fulfillment saga and a request
-- thread can touch the same Order concurrently.
ALTER TABLE users       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE api_clients ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
