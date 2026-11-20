ALTER TABLE chats DROP CONSTRAINT chats_type_check;
ALTER TABLE chats ADD CONSTRAINT chats_type_check CHECK (type IN ('dm', 'group', 'channel', 'text'));

CREATE TABLE room_categories (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 32),
    position INT NOT NULL
);

CREATE INDEX room_categories_room_idx ON room_categories (room_id, position);

CREATE TABLE room_channels (
    chat_id BIGINT PRIMARY KEY REFERENCES chats (id) ON DELETE CASCADE,
    room_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES room_categories (id) ON DELETE SET NULL,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 32),
    position INT NOT NULL
);

CREATE INDEX room_channels_room_idx ON room_channels (room_id, position);

CREATE TABLE channel_reads (
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (chat_id, user_id)
);

INSERT INTO room_channels (chat_id, room_id, category_id, name, position)
SELECT id, id, NULL, 'general', 0 FROM chats WHERE type = 'group';
