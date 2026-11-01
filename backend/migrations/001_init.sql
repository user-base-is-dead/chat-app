CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username TEXT NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    show_last_seen BOOLEAN NOT NULL DEFAULT TRUE,
    read_receipts BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX users_username_key ON users (lower(username));

CREATE TABLE devices (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    platform TEXT NOT NULL CHECK (platform IN ('web', 'android', 'ios')),
    name TEXT NOT NULL DEFAULT '',
    identity_key TEXT,
    signing_key TEXT,
    fallback_key TEXT,
    push_token TEXT,
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX devices_user_id_idx ON devices (user_id);

CREATE TABLE one_time_keys (
    device_id BIGINT NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    key_id TEXT NOT NULL,
    public_key TEXT NOT NULL,
    PRIMARY KEY (device_id, key_id)
);

CREATE TABLE to_device_messages (
    id BIGINT PRIMARY KEY,
    recipient_device_id BIGINT NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    sender_device_id BIGINT NOT NULL,
    ciphertext BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX to_device_messages_recipient_idx ON to_device_messages (recipient_device_id, id);

CREATE TABLE chats (
    id BIGINT PRIMARY KEY,
    type TEXT NOT NULL CHECK (type IN ('dm')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_members (
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'request', 'restricted')),
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    muted BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX chat_members_user_id_idx ON chat_members (user_id);

CREATE TABLE media (
    id BIGINT PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    storage_key TEXT NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_device_id BIGINT NOT NULL,
    ciphertext BYTEA,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (chat_id, id)
) PARTITION BY HASH (chat_id);

DO $$
BEGIN
    FOR i IN 0..15 LOOP
        EXECUTE format(
            'CREATE TABLE messages_p%s PARTITION OF messages FOR VALUES WITH (MODULUS 16, REMAINDER %s)',
            i, i
        );
    END LOOP;
END
$$;
