ALTER TABLE chats DROP CONSTRAINT chats_type_check;
ALTER TABLE chats ADD CONSTRAINT chats_type_check CHECK (type IN ('dm', 'group', 'channel'));

ALTER TABLE chat_members ADD COLUMN role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member'));
ALTER TABLE chat_members ADD COLUMN joined_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE rooms (
    chat_id BIGINT PRIMARY KEY REFERENCES chats (id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    about TEXT NOT NULL DEFAULT '',
    username TEXT,
    is_public BOOLEAN NOT NULL,
    photo_id BIGINT REFERENCES media (id) ON DELETE SET NULL,
    invite_code TEXT NOT NULL UNIQUE,
    members_can_send BOOLEAN NOT NULL DEFAULT TRUE,
    members_can_add BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (is_public = (username IS NOT NULL))
);

CREATE UNIQUE INDEX rooms_username_idx ON rooms (lower(username));

CREATE TABLE room_bans (
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, user_id)
);
