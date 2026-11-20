ALTER TABLE room_channels ADD COLUMN synced BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE channel_overrides (
    scope_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    target_type TEXT NOT NULL CHECK (target_type IN ('everyone', 'role', 'member')),
    target_id BIGINT NOT NULL,
    allow TEXT[] NOT NULL DEFAULT '{}',
    deny TEXT[] NOT NULL DEFAULT '{}',
    PRIMARY KEY (scope_id, target_type, target_id)
);

CREATE INDEX channel_overrides_room_idx ON channel_overrides (room_id);

CREATE FUNCTION apply_override(perms TEXT[], deny TEXT[], allow TEXT[]) RETURNS TEXT[]
LANGUAGE sql IMMUTABLE AS $$
    SELECT COALESCE(array_agg(DISTINCT key), '{}') FROM (
        SELECT unnest(perms) AS key EXCEPT SELECT unnest(COALESCE(deny, '{}'))
        UNION SELECT unnest(COALESCE(allow, '{}'))
    ) keys
$$;

CREATE FUNCTION scope_permissions(p_room BIGINT, p_scope BIGINT, p_user BIGINT) RETURNS TEXT[]
LANGUAGE plpgsql STABLE AS $$
DECLARE
    all_keys CONSTANT TEXT[] := ARRAY['viewChannel', 'sendMessages', 'deleteMessages', 'manageChannel', 'managePermissions'];
    v_role TEXT;
    v_status TEXT;
    v_can_send BOOLEAN;
    v_group TEXT[];
    v_perms TEXT[];
    v_allow TEXT[];
    v_deny TEXT[];
BEGIN
    SELECT m.role, m.status INTO v_role, v_status FROM chat_members m WHERE m.chat_id = p_room AND m.user_id = p_user;
    IF v_status IS DISTINCT FROM 'active' THEN
        RETURN '{}';
    END IF;
    IF v_role = 'owner' THEN
        RETURN all_keys;
    END IF;

    SELECT COALESCE(array_agg(DISTINCT p), '{}') INTO v_group
    FROM member_roles mr JOIN room_roles rr ON rr.id = mr.role_id CROSS JOIN LATERAL unnest(rr.permissions) AS p
    WHERE mr.room_id = p_room AND mr.user_id = p_user;
    IF 'administrator' = ANY (v_group) THEN
        RETURN all_keys;
    END IF;

    SELECT r.members_can_send INTO v_can_send FROM rooms r WHERE r.chat_id = p_room;
    v_perms := ARRAY['viewChannel'];
    IF v_can_send OR 'sendAlways' = ANY (v_group) THEN
        v_perms := v_perms || 'sendMessages'::TEXT;
    END IF;
    IF 'deleteMessages' = ANY (v_group) THEN
        v_perms := v_perms || 'deleteMessages'::TEXT;
    END IF;
    IF 'manageChannels' = ANY (v_group) THEN
        v_perms := v_perms || 'manageChannel'::TEXT;
    END IF;
    IF 'manageRoles' = ANY (v_group) THEN
        v_perms := v_perms || 'managePermissions'::TEXT;
    END IF;

    SELECT o.deny, o.allow INTO v_deny, v_allow FROM channel_overrides o
    WHERE o.scope_id = p_scope AND o.target_type = 'everyone';
    v_perms := apply_override(v_perms, v_deny, v_allow);

    SELECT COALESCE(array_agg(DISTINCT d), '{}') INTO v_deny
    FROM channel_overrides o JOIN member_roles mr ON mr.role_id = o.target_id AND mr.user_id = p_user
    CROSS JOIN LATERAL unnest(o.deny) AS d
    WHERE o.scope_id = p_scope AND o.target_type = 'role';
    SELECT COALESCE(array_agg(DISTINCT a), '{}') INTO v_allow
    FROM channel_overrides o JOIN member_roles mr ON mr.role_id = o.target_id AND mr.user_id = p_user
    CROSS JOIN LATERAL unnest(o.allow) AS a
    WHERE o.scope_id = p_scope AND o.target_type = 'role';
    v_perms := apply_override(v_perms, v_deny, v_allow);

    v_deny := NULL;
    v_allow := NULL;
    SELECT o.deny, o.allow INTO v_deny, v_allow FROM channel_overrides o
    WHERE o.scope_id = p_scope AND o.target_type = 'member' AND o.target_id = p_user;
    v_perms := apply_override(v_perms, v_deny, v_allow);

    IF NOT ('viewChannel' = ANY (v_perms)) THEN
        RETURN '{}';
    END IF;
    RETURN v_perms;
END
$$;

CREATE FUNCTION channel_scope(p_channel BIGINT) RETURNS BIGINT
LANGUAGE sql STABLE AS $$
    SELECT CASE WHEN rc.synced AND rc.category_id IS NOT NULL THEN rc.category_id ELSE rc.chat_id END
    FROM room_channels rc WHERE rc.chat_id = p_channel
$$;

CREATE FUNCTION channel_permissions(p_channel BIGINT, p_user BIGINT) RETURNS TEXT[]
LANGUAGE sql STABLE AS $$
    SELECT CASE WHEN rc.chat_id IS NULL THEN NULL
        ELSE scope_permissions(rc.room_id, channel_scope(rc.chat_id), p_user) END
    FROM room_channels rc WHERE rc.chat_id = p_channel
$$;

CREATE FUNCTION can_view_channel(p_channel BIGINT, p_user BIGINT) RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
    SELECT COALESCE('viewChannel' = ANY (channel_permissions(p_channel, p_user)), TRUE)
$$;
