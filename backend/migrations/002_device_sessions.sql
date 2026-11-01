ALTER TABLE devices
    ADD COLUMN refresh_token_hash BYTEA,
    ADD COLUMN refresh_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX devices_refresh_token_hash_key ON devices (refresh_token_hash);
