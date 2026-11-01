DELETE FROM chat_members cm
WHERE cm.status = 'request'
  AND NOT EXISTS (SELECT 1 FROM messages m WHERE m.chat_id = cm.chat_id);
