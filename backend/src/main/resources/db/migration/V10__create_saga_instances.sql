-- #5 saga engine state. One row per running or finished saga instance. The orchestrator
-- persists its state after every step so a crash resumes from the last committed step
-- rather than restarting or losing the workflow.
CREATE TABLE saga_instances (
    id             UUID PRIMARY KEY,
    saga_type      VARCHAR(100) NOT NULL,
    correlation_id UUID NOT NULL,
    state          VARCHAR(60)  NOT NULL,
    payload        TEXT         NOT NULL DEFAULT '{}',
    last_error     TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_saga_type_correlation UNIQUE (saga_type, correlation_id)
);

-- The recovery job scans instances that are still running and have not moved recently.
CREATE INDEX idx_saga_stalled ON saga_instances (updated_at) WHERE completed_at IS NULL;
-- The retention sweep scans finished instances by age.
CREATE INDEX idx_saga_completed_at ON saga_instances (completed_at) WHERE completed_at IS NOT NULL;
