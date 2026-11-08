-- ShedLock: one row per named scheduled job; whichever instance holds the row until
-- `lock_until` is the one that runs that tick. Not a Hibernate entity — Flyway owns it,
-- ddl-auto=validate ignores it.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
