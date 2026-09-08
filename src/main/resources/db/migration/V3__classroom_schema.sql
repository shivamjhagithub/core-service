-- Classrooms: the unit teachers and students actually work in.
--
-- Membership drives authorization for assignments, attendance, materials, chat
-- and meetings, and classroom_permissions is what lets a teacher decide how
-- much students may do inside one classroom.

CREATE TABLE classrooms
(
    id          varchar(36)  NOT NULL,
    college_id  varchar(36)  NOT NULL,
    name        varchar(150) NOT NULL,
    description varchar(1000),
    created_by  varchar(64)  NOT NULL,
    active      bit          NOT NULL DEFAULT 1,
    archived    bit          NOT NULL DEFAULT 0,
    created_at  datetime(6)  NOT NULL,
    updated_at  datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_classrooms_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_classrooms_college_id ON classrooms (college_id);
CREATE INDEX idx_classrooms_created_by ON classrooms (created_by);

CREATE TABLE classroom_members
(
    id           varchar(36) NOT NULL,
    college_id   varchar(36) NOT NULL,
    classroom_id varchar(36) NOT NULL,
    user_id      varchar(64) NOT NULL,
    member_type  varchar(20) NOT NULL,
    joined_at    datetime(6) NOT NULL,
    created_at   datetime(6) NOT NULL,
    updated_at   datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_classroom_members_classroom_user UNIQUE (classroom_id, user_id),
    CONSTRAINT ck_classroom_members_member_type CHECK (member_type IN ('TEACHER', 'STUDENT')),
    CONSTRAINT fk_classroom_members_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_classroom_members_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_classroom_members_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_classroom_members_college_id ON classroom_members (college_id);
CREATE INDEX idx_classroom_members_classroom_id ON classroom_members (classroom_id);
CREATE INDEX idx_classroom_members_user_id ON classroom_members (user_id);

-- A missing row is read as TEACHERS_ONLY, so the absence of configuration is
-- the restrictive case rather than the permissive one.
CREATE TABLE classroom_permissions
(
    id              varchar(36) NOT NULL,
    college_id      varchar(36) NOT NULL,
    classroom_id    varchar(36) NOT NULL,
    permission_type varchar(40) NOT NULL,
    mode            varchar(20) NOT NULL,
    created_at      datetime(6) NOT NULL,
    updated_at      datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_classroom_permissions_classroom_type UNIQUE (classroom_id, permission_type),
    CONSTRAINT ck_classroom_permissions_type CHECK (permission_type IN
        ('SEND_MESSAGE', 'UPLOAD_FILE', 'CREATE_MEETING', 'CREATE_POST')),
    CONSTRAINT ck_classroom_permissions_mode CHECK (mode IN
        ('EVERYONE', 'TEACHERS_ONLY', 'DISABLED')),
    CONSTRAINT fk_classroom_permissions_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_classroom_permissions_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_classroom_permissions_college_id ON classroom_permissions (college_id);
CREATE INDEX idx_classroom_permissions_classroom_id ON classroom_permissions (classroom_id);
