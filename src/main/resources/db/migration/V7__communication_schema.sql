-- Real-time chat and video meeting management.
--
-- Messages live here, not in the broker: WebSocket carries the live copy while
-- MySQL is the history. `file_id` is a soft reference to stored_files for the
-- same reason as in V6.

-- direct_key holds the two participants' user ids sorted and joined with '|',
-- which is what makes opening a direct room idempotent for a pair. It is NULL
-- for classroom rooms, and classroom_id is NULL for direct rooms; MySQL treats
-- NULLs as distinct, so neither unique constraint blocks the other kind.
CREATE TABLE chat_rooms
(
    id              varchar(36) NOT NULL,
    college_id      varchar(36) NOT NULL,
    name            varchar(255),
    room_type       varchar(32) NOT NULL,
    classroom_id    varchar(36),
    permission_mode varchar(32) NOT NULL,
    direct_key      varchar(512),
    created_at      datetime(6) NOT NULL,
    updated_at      datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_rooms_college_direct_key UNIQUE (college_id, direct_key),
    CONSTRAINT uk_chat_rooms_college_classroom UNIQUE (college_id, classroom_id),
    CONSTRAINT ck_chat_rooms_room_type CHECK (room_type IN ('DIRECT', 'CLASSROOM')),
    CONSTRAINT ck_chat_rooms_permission_mode CHECK (permission_mode IN
        ('EVERYONE', 'TEACHERS_ONLY', 'READ_ONLY', 'DISABLED')),
    CONSTRAINT fk_chat_rooms_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_rooms_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_rooms_college_id ON chat_rooms (college_id);
CREATE INDEX idx_chat_rooms_classroom_id ON chat_rooms (classroom_id);

CREATE TABLE chat_participants
(
    id           varchar(36)  NOT NULL,
    college_id   varchar(36)  NOT NULL,
    chat_room_id varchar(36)  NOT NULL,
    user_id      varchar(255) NOT NULL,
    joined_at    datetime(6),
    last_read_at datetime(6),
    created_at   datetime(6)  NOT NULL,
    updated_at   datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_participants_room_user UNIQUE (chat_room_id, user_id),
    CONSTRAINT fk_chat_participants_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_participants_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_participants_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_participants_college_id ON chat_participants (college_id);
CREATE INDEX idx_chat_participants_room_id ON chat_participants (chat_room_id);
CREATE INDEX idx_chat_participants_user_id ON chat_participants (user_id);

-- Membership lookup on the subscription-authorization path: it runs on every
-- SUBSCRIBE frame, so it must be an index-only check.
CREATE INDEX idx_chat_participants_lookup ON chat_participants (college_id, chat_room_id, user_id);

CREATE TABLE chat_messages
(
    id             varchar(36)  NOT NULL,
    college_id     varchar(36)  NOT NULL,
    chat_room_id   varchar(36)  NOT NULL,
    sender_user_id varchar(255) NOT NULL,
    content        varchar(4000),
    message_type   varchar(32)  NOT NULL,
    file_id        varchar(36),
    deleted        bit          NOT NULL DEFAULT 0,
    created_at     datetime(6)  NOT NULL,
    updated_at     datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_chat_messages_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),
    CONSTRAINT fk_chat_messages_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_messages_college_id ON chat_messages (college_id);
CREATE INDEX idx_chat_messages_room_id ON chat_messages (chat_room_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at);

-- Serves the newest-first history page and the unread count.
CREATE INDEX idx_chat_messages_room_created_at ON chat_messages (chat_room_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Video meetings
-- ---------------------------------------------------------------------------
-- The backend manages lifecycle, participants and WebRTC signaling only; it
-- never carries media. provider_room_id is the handle held by whichever
-- VideoConferenceProvider is configured.
CREATE TABLE meetings
(
    id                   varchar(36)  NOT NULL,
    college_id           varchar(36)  NOT NULL,
    classroom_id         varchar(36),
    title                varchar(255) NOT NULL,
    description          varchar(2000),
    host_user_id         varchar(255) NOT NULL,
    scheduled_start_time datetime(6)  NOT NULL,
    scheduled_end_time   datetime(6),
    actual_start_time    datetime(6),
    actual_end_time      datetime(6),
    status               varchar(32)  NOT NULL,
    provider_name        varchar(64),
    provider_room_id     varchar(255),
    created_at           datetime(6)  NOT NULL,
    updated_at           datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_meetings_status CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT fk_meetings_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_meetings_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_meetings_college_id ON meetings (college_id);
CREATE INDEX idx_meetings_classroom_id ON meetings (classroom_id);
CREATE INDEX idx_meetings_status ON meetings (status);
CREATE INDEX idx_meetings_scheduled_start_time ON meetings (scheduled_start_time);
CREATE INDEX idx_meetings_college_status_start ON meetings (college_id, status, scheduled_start_time);

CREATE TABLE meeting_participants
(
    id         varchar(36)  NOT NULL,
    college_id varchar(36)  NOT NULL,
    meeting_id varchar(36)  NOT NULL,
    user_id    varchar(255) NOT NULL,
    role       varchar(32)  NOT NULL,
    joined_at  datetime(6),
    left_at    datetime(6),
    created_at datetime(6)  NOT NULL,
    updated_at datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_participants_meeting_user UNIQUE (meeting_id, user_id),
    CONSTRAINT ck_meeting_participants_role CHECK (role IN ('HOST', 'CO_HOST', 'ATTENDEE')),
    CONSTRAINT fk_meeting_participants_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_participants_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_participants_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_meeting_participants_college_id ON meeting_participants (college_id);
CREATE INDEX idx_meeting_participants_meeting_id ON meeting_participants (meeting_id);
CREATE INDEX idx_meeting_participants_user_id ON meeting_participants (user_id);

-- Signaling re-checks active participation on every relayed frame.
CREATE INDEX idx_meeting_participants_lookup ON meeting_participants (college_id, meeting_id, user_id);
