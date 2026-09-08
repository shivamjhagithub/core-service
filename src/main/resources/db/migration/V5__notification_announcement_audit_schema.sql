-- Notifications, push device handles, announcements and the audit trail.

-- One row per recipient, which is what makes "mark as read" and the unread
-- badge per-user operations. The column is is_read because READ is a reserved
-- word in MySQL.
--
-- `type` carries no CHECK constraint on purpose: NotificationType grows with
-- every new feature, and a constraint would force a migration each time. The
-- stable, small enums below (platform, target) do get one.
CREATE TABLE notifications
(
    id                varchar(36)  NOT NULL,
    college_id        varchar(36)  NOT NULL,
    recipient_user_id varchar(255) NOT NULL,
    type              varchar(40)  NOT NULL,
    title             varchar(255) NOT NULL,
    body              varchar(1000),
    reference_id      varchar(36),
    is_read           bit          NOT NULL DEFAULT 0,
    read_at           datetime(6),
    created_at        datetime(6)  NOT NULL,
    updated_at        datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_college_id ON notifications (college_id);
CREATE INDEX idx_notifications_recipient_user_id ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

-- Serves the inbox query and the unread badge in one index.
CREATE INDEX idx_notifications_recipient_unread
    ON notifications (college_id, recipient_user_id, is_read, created_at DESC);

-- The token is unique platform-wide, so a device handed to another user is
-- rebound rather than duplicated.
CREATE TABLE device_tokens
(
    id         varchar(36)  NOT NULL,
    college_id varchar(36)  NOT NULL,
    user_id    varchar(255) NOT NULL,
    token      varchar(512) NOT NULL,
    platform   varchar(16)  NOT NULL,
    active     bit          NOT NULL DEFAULT 1,
    created_at datetime(6)  NOT NULL,
    updated_at datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT ck_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    CONSTRAINT fk_device_tokens_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_device_tokens_college_id ON device_tokens (college_id);
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

-- Stored once and read by many. The audience is described by target plus at
-- most one identifier and resolved at publish time, so it is not frozen into
-- the row. created_by has no foreign key: authorship is historical attribution
-- and must outlive the author's account.
CREATE TABLE announcements
(
    id             varchar(36)   NOT NULL,
    college_id     varchar(36)   NOT NULL,
    title          varchar(255)  NOT NULL,
    body           varchar(4000) NOT NULL,
    target         varchar(20)   NOT NULL,
    target_id      varchar(36),
    target_user_id varchar(255),
    created_by     varchar(255)  NOT NULL,
    published      bit           NOT NULL DEFAULT 0,
    published_at   datetime(6),
    created_at     datetime(6)   NOT NULL,
    updated_at     datetime(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_announcements_target CHECK (target IN ('COLLEGE', 'DEPARTMENT', 'CLASSROOM', 'USER')),
    CONSTRAINT fk_announcements_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_announcements_college_id ON announcements (college_id);
CREATE INDEX idx_announcements_target ON announcements (target);
CREATE INDEX idx_announcements_created_at ON announcements (created_at);
CREATE INDEX idx_announcements_college_published ON announcements (college_id, published, published_at DESC);

-- Who did what to which resource. There is deliberately no free-form payload
-- column, so the trail can never end up holding a password, a token or a
-- secret regardless of what a calling module passes.
--
-- user_id has no foreign key either: an audit trail that disappears when the
-- account is deleted is not an audit trail.
CREATE TABLE audit_logs
(
    id            varchar(36)  NOT NULL,
    college_id    varchar(36)  NOT NULL,
    user_id       varchar(255),
    action        varchar(100) NOT NULL,
    resource_type varchar(100) NOT NULL,
    resource_id   varchar(100),
    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_college_id ON audit_logs (college_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
