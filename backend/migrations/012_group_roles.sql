CREATE TABLE room_roles (
    id BIGINT PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 32),
    color TEXT,
    separate BOOLEAN NOT NULL DEFAULT FALSE,
    permissions TEXT[] NOT NULL DEFAULT '{}',
    position INT NOT NULL,
    builtin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX room_roles_room_idx ON room_roles (room_id, position);

CREATE TABLE member_roles (
    room_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES room_roles (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, user_id)
);

CREATE INDEX member_roles_member_idx ON member_roles (room_id, user_id);

INSERT INTO room_roles (id, room_id, name, color, separate, permissions, position, builtin)
SELECT c.id, c.id, 'Admin', NULL, FALSE, ARRAY['administrator'], 0, TRUE
FROM chats c WHERE c.type = 'group';

INSERT INTO member_roles (room_id, user_id, role_id)
SELECT m.chat_id, m.user_id, m.chat_id
FROM chat_members m JOIN chats c ON c.id = m.chat_id AND c.type = 'group'
WHERE m.role = 'admin';

UPDATE chat_members m SET role = 'member'
FROM chats c WHERE c.id = m.chat_id AND c.type = 'group' AND m.role = 'admin';
