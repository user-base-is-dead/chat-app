DELETE FROM messages WHERE deleted_at IS NOT NULL;

DELETE FROM chats c
WHERE NOT EXISTS (SELECT 1 FROM messages m WHERE m.chat_id = c.id);
