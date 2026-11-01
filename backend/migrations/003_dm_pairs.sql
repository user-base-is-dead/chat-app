CREATE TABLE dm_pairs (
    user_low BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_high BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL UNIQUE REFERENCES chats (id) ON DELETE CASCADE,
    PRIMARY KEY (user_low, user_high),
    CHECK (user_low < user_high)
);
