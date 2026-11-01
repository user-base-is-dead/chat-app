ALTER TABLE chat_members DROP COLUMN archived;
ALTER TABLE chat_members DROP COLUMN pinned;
ALTER TABLE chat_members ADD COLUMN pinned_at TIMESTAMPTZ;
