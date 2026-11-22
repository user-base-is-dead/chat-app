ALTER TABLE messages ADD COLUMN reply_to_id BIGINT;
ALTER TABLE messages ADD COLUMN forwarded_from_id BIGINT;
ALTER TABLE messages ADD COLUMN forwarded_from_chat_id BIGINT;
ALTER TABLE chats ADD COLUMN pinned_message_id BIGINT;
