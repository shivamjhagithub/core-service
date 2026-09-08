-- File storage, syllabi, assignments and study material.
--
-- `file_id` columns carry no foreign key on purpose: the file module owns file
-- lifecycle, a stored file may be referenced from more than one place, and
-- deleting content must not delete the bytes (nor the reverse). Content modules
-- hold a soft reference and resolve it through FileStorageService.

CREATE TABLE stored_files
(
    id                 varchar(36)  NOT NULL,
    college_id         varchar(36)  NOT NULL,
    original_file_name varchar(255) NOT NULL,
    content_type       varchar(150),
    file_size          bigint       NOT NULL,
    storage_path       varchar(512) NOT NULL,
    uploaded_by        varchar(64)  NOT NULL,
    storage_provider   varchar(32)  NOT NULL,
    created_at         datetime(6)  NOT NULL,
    updated_at         datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stored_files_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_stored_files_college_id ON stored_files (college_id);
CREATE INDEX idx_stored_files_uploaded_by ON stored_files (uploaded_by);

-- ---------------------------------------------------------------------------
-- Syllabus: syllabus -> unit -> topic
-- ---------------------------------------------------------------------------
-- classroom_id and subject_id degrade to NULL rather than cascading, so losing
-- a classroom or subject downgrades a syllabus to college level instead of
-- destroying curriculum content.
CREATE TABLE syllabi
(
    id           varchar(36)  NOT NULL,
    college_id   varchar(36)  NOT NULL,
    title        varchar(200) NOT NULL,
    description  varchar(2000),
    subject_id   varchar(36),
    classroom_id varchar(36),
    published    bit          NOT NULL DEFAULT 0,
    created_at   datetime(6)  NOT NULL,
    updated_at   datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syllabi_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_syllabi_subject FOREIGN KEY (subject_id) REFERENCES academic_subjects (id) ON DELETE SET NULL,
    CONSTRAINT fk_syllabi_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_syllabi_college_id ON syllabi (college_id);
CREATE INDEX idx_syllabi_subject_id ON syllabi (subject_id);
CREATE INDEX idx_syllabi_classroom_id ON syllabi (classroom_id);

CREATE TABLE syllabus_units
(
    id          varchar(36)  NOT NULL,
    college_id  varchar(36)  NOT NULL,
    syllabus_id varchar(36)  NOT NULL,
    title       varchar(200) NOT NULL,
    description varchar(2000),
    order_index integer,
    created_at  datetime(6)  NOT NULL,
    updated_at  datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syllabus_units_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_syllabus_units_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabi (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_syllabus_units_college_id ON syllabus_units (college_id);
CREATE INDEX idx_syllabus_units_syllabus_id ON syllabus_units (syllabus_id);

CREATE TABLE syllabus_topics
(
    id               varchar(36)  NOT NULL,
    college_id       varchar(36)  NOT NULL,
    syllabus_unit_id varchar(36)  NOT NULL,
    title            varchar(200) NOT NULL,
    description      varchar(2000),
    order_index      integer,
    completed        bit          NOT NULL DEFAULT 0,
    completed_at     datetime(6),
    completed_by     varchar(64),
    created_at       datetime(6)  NOT NULL,
    updated_at       datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_syllabus_topics_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_syllabus_topics_unit FOREIGN KEY (syllabus_unit_id) REFERENCES syllabus_units (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_syllabus_topics_college_id ON syllabus_topics (college_id);
CREATE INDEX idx_syllabus_topics_unit_id ON syllabus_topics (syllabus_unit_id);

-- Supports the syllabus progress aggregate.
CREATE INDEX idx_syllabus_topics_unit_completed ON syllabus_topics (syllabus_unit_id, completed);

-- ---------------------------------------------------------------------------
-- Assignments and submissions
-- ---------------------------------------------------------------------------
CREATE TABLE assignments
(
    id           varchar(36)  NOT NULL,
    college_id   varchar(36)  NOT NULL,
    classroom_id varchar(36)  NOT NULL,
    subject_id   varchar(36),
    title        varchar(200) NOT NULL,
    description  varchar(4000),
    created_by   varchar(64)  NOT NULL,
    due_at       datetime(6),
    max_marks    float(53),
    status       varchar(20)  NOT NULL,
    published_at datetime(6),
    created_at   datetime(6)  NOT NULL,
    updated_at   datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_assignments_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    CONSTRAINT fk_assignments_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_subject FOREIGN KEY (subject_id) REFERENCES academic_subjects (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_assignments_college_id ON assignments (college_id);
CREATE INDEX idx_assignments_classroom_id ON assignments (classroom_id);
CREATE INDEX idx_assignments_status ON assignments (status);

-- Serves the "upcoming assignments" dashboard query.
CREATE INDEX idx_assignments_classroom_status_due ON assignments (college_id, classroom_id, status, due_at);

CREATE TABLE assignment_attachments
(
    id            varchar(36) NOT NULL,
    college_id    varchar(36) NOT NULL,
    assignment_id varchar(36) NOT NULL,
    file_id       varchar(36) NOT NULL,
    created_at    datetime(6) NOT NULL,
    updated_at    datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assignment_attachments_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_attachments_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_assignment_attachments_college_id ON assignment_attachments (college_id);
CREATE INDEX idx_assignment_attachments_assignment_id ON assignment_attachments (assignment_id);

-- NOT_SUBMITTED is never persisted; it describes the absence of a row and
-- exists only for computed views. The constraint still admits it so the
-- database never contradicts the enum.
CREATE TABLE assignment_submissions
(
    id              varchar(36) NOT NULL,
    college_id      varchar(36) NOT NULL,
    assignment_id   varchar(36) NOT NULL,
    student_user_id varchar(64) NOT NULL,
    content         varchar(4000),
    submitted_at    datetime(6),
    status          varchar(20) NOT NULL,
    marks           float(53),
    feedback        varchar(2000),
    graded_by       varchar(64),
    graded_at       datetime(6),
    created_at      datetime(6) NOT NULL,
    updated_at      datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assignment_submissions_assignment_student UNIQUE (assignment_id, student_user_id),
    CONSTRAINT ck_assignment_submissions_status CHECK (status IN
        ('NOT_SUBMITTED', 'SUBMITTED', 'LATE', 'GRADED')),
    CONSTRAINT fk_assignment_submissions_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_submissions_student FOREIGN KEY (student_user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_assignment_submissions_college_id ON assignment_submissions (college_id);
CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions (assignment_id);
CREATE INDEX idx_assignment_submissions_student_user_id ON assignment_submissions (student_user_id);

-- Serves the teacher dashboard's "awaiting grading" count.
CREATE INDEX idx_assignment_submissions_status ON assignment_submissions (college_id, status);

CREATE TABLE submission_attachments
(
    id                       varchar(36) NOT NULL,
    college_id               varchar(36) NOT NULL,
    assignment_submission_id varchar(36) NOT NULL,
    file_id                  varchar(36) NOT NULL,
    created_at               datetime(6) NOT NULL,
    updated_at               datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_submission_attachments_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_attachments_submission FOREIGN KEY (assignment_submission_id)
        REFERENCES assignment_submissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_submission_attachments_college_id ON submission_attachments (college_id);
CREATE INDEX idx_submission_attachments_submission_id ON submission_attachments (assignment_submission_id);

-- ---------------------------------------------------------------------------
-- Study material
-- ---------------------------------------------------------------------------
-- Exactly one of file_id / link_url is set, enforced by StudyMaterialService.
-- topic_id is a soft reference: validating it would make material depend on the
-- syllabus module.
CREATE TABLE study_materials
(
    id            varchar(36)  NOT NULL,
    college_id    varchar(36)  NOT NULL,
    title         varchar(200) NOT NULL,
    description   varchar(2000),
    material_type varchar(20)  NOT NULL,
    file_id       varchar(36),
    link_url      varchar(2000),
    classroom_id  varchar(36),
    subject_id    varchar(36),
    topic_id      varchar(36),
    uploaded_by   varchar(64)  NOT NULL,
    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_study_materials_type CHECK (material_type IN
        ('PDF', 'DOCUMENT', 'IMAGE', 'VIDEO', 'LINK')),
    CONSTRAINT fk_study_materials_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_study_materials_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE SET NULL,
    CONSTRAINT fk_study_materials_subject FOREIGN KEY (subject_id) REFERENCES academic_subjects (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_study_materials_college_id ON study_materials (college_id);
CREATE INDEX idx_study_materials_classroom_id ON study_materials (classroom_id);
CREATE INDEX idx_study_materials_subject_id ON study_materials (subject_id);
