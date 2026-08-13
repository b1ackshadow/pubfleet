-- Unit 00 walking skeleton. One table holds the whole job life.
--
-- The control plane owns this schema. The worker reads and claims rows in it, but it
-- runs no migration of its own.

CREATE TABLE jobs (
    id          uuid        PRIMARY KEY,
    supplier_id text        NOT NULL,
    payload_ref text        NOT NULL,
    status      text        NOT NULL,
    worker_id   text,
    result      text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT jobs_status_known
        CHECK (status IN ('PENDING', 'CLAIMED', 'SUCCEEDED', 'FAILED'))
);

-- The list endpoint pages by keyset on (created_at, id) descending. This index makes
-- that an index seek instead of a sort of the whole table.
CREATE INDEX jobs_created_at_id_idx ON jobs (created_at DESC, id DESC);
