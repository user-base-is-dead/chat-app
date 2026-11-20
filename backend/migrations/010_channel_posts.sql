SET client_encoding = 'UTF8';

ALTER TABLE messages ADD COLUMN allow_reactions BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE messages ADD COLUMN allow_comments BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE messages ADD COLUMN view_count INT NOT NULL DEFAULT 0;
ALTER TABLE messages ADD COLUMN comment_count INT NOT NULL DEFAULT 0;

CREATE TABLE post_views (
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, message_id, user_id),
    FOREIGN KEY (chat_id, message_id) REFERENCES messages (chat_id, id) ON DELETE CASCADE
);

CREATE TABLE post_reactions (
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    emoji TEXT NOT NULL CHECK (emoji IN ('👍', '❤️', '🔥', '😂', '😮', '😢')),
    PRIMARY KEY (chat_id, message_id, user_id),
    FOREIGN KEY (chat_id, message_id) REFERENCES messages (chat_id, id) ON DELETE CASCADE
);

CREATE TABLE post_comments (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (chat_id, message_id) REFERENCES messages (chat_id, id) ON DELETE CASCADE
);

CREATE INDEX post_comments_post_idx ON post_comments (chat_id, message_id, id);
