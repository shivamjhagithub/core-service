-- Core platform schema: tenants, identity, dynamic RBAC and module toggles.
--
-- MySQL notes that apply to every migration in this project:
--   * UUID keys are stored as varchar(36) rather than binary(16) so rows can be
--     found by pasting an id straight from a log or an API response.
--   * Enum-backed columns are varchar, not MySQL's native ENUM, so adding a
--     constant does not require an ALTER TABLE.
--   * Every tenant-owned table references colleges(college_id) with
--     ON DELETE CASCADE: removing a college removes everything it owns.

CREATE TABLE colleges
(
    college_id          varchar(36)  NOT NULL,
    college_name        varchar(255) NOT NULL,
    college_code        varchar(255) NOT NULL,
    college_phone       varchar(255),
    college_email       varchar(255) NOT NULL,
    college_address     varchar(255),
    college_city        varchar(255),
    college_state       varchar(255),
    college_zip         varchar(255),
    college_country     varchar(255),
    university_name     varchar(255) NOT NULL,
    university_code     varchar(255) NOT NULL,
    college_description varchar(2000),
    active              bit          NOT NULL DEFAULT 1,
    created_at          datetime(6)  NOT NULL,
    updated_at          datetime(6)  NOT NULL,
    PRIMARY KEY (college_id),
    CONSTRAINT uk_colleges_college_code UNIQUE (college_code),
    CONSTRAINT uk_colleges_college_email UNIQUE (college_email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_colleges_college_city ON colleges (college_city);
CREATE INDEX idx_colleges_university_name ON colleges (university_name);

-- college_id is nullable: MAIN_ADMIN operates above any single college.
CREATE TABLE users
(
    user_id                varchar(255) NOT NULL,
    user_name              varchar(255) NOT NULL,
    father_name            varchar(255),
    college_id             varchar(36),
    image                  varchar(255),
    phone_number           bigint,
    alternate_phone_number bigint,
    blood_group            varchar(8),
    email                  varchar(255) NOT NULL,
    password               varchar(255) NOT NULL,
    activate               bit,
    created_at             datetime(6)  NOT NULL,
    updated_at             datetime(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_users_college_id ON users (college_id);
CREATE INDEX idx_users_email ON users (email);

CREATE TABLE auth_refresh_tokens
(
    id         varchar(255) NOT NULL,
    user_id    varchar(255) NOT NULL,
    expires_at datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_auth_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_id ON auth_refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON auth_refresh_tokens (expires_at);

-- college_id is nullable: a role with no college is platform-wide.
CREATE TABLE roles
(
    role_id          varchar(36)  NOT NULL,
    role_name        varchar(255) NOT NULL,
    role_description varchar(255),
    college_id       varchar(36),
    created_at       datetime(6)  NOT NULL,
    updated_at       datetime(6)  NOT NULL,
    PRIMARY KEY (role_id),
    CONSTRAINT fk_roles_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_roles_college_id ON roles (college_id);
CREATE UNIQUE INDEX uk_roles_college_name ON roles (college_id, role_name);

CREATE TABLE permissions
(
    permission_id          varchar(255) NOT NULL,
    permission_name        varchar(255) NOT NULL,
    permission_code        varchar(255) NOT NULL,
    permission_description varchar(255),
    PRIMARY KEY (permission_id),
    CONSTRAINT uk_permissions_permission_code UNIQUE (permission_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_roles
(
    id      varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    role_id varchar(36)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE role_permissions
(
    id            varchar(255) NOT NULL,
    role_id       varchar(36)  NOT NULL,
    permission_id varchar(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE TABLE system_modules
(
    module_id          varchar(255) NOT NULL,
    module_name        varchar(255) NOT NULL,
    module_description varchar(255),
    module_code        varchar(255) NOT NULL,
    PRIMARY KEY (module_id),
    CONSTRAINT uk_system_modules_module_code UNIQUE (module_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE college_modules
(
    id         bigint       NOT NULL AUTO_INCREMENT,
    college_id varchar(36)  NOT NULL,
    module_id  varchar(255) NOT NULL,
    enabled    bit          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_college_modules_college_module UNIQUE (college_id, module_id),
    CONSTRAINT fk_college_modules_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_college_modules_module FOREIGN KEY (module_id) REFERENCES system_modules (module_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_college_modules_college_id ON college_modules (college_id);
