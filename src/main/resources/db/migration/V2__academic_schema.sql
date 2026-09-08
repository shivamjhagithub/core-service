-- Academic structure and the academic records of people.
--
--   department -> program -> semester -> subject
--
-- Students and teachers reference the person by users.user_id and hold
-- department/program as plain ids, so those modules share no entity graph with
-- the academic module.

CREATE TABLE academic_departments
(
    id          varchar(36)  NOT NULL,
    college_id  varchar(36)  NOT NULL,
    name        varchar(150) NOT NULL,
    code        varchar(32)  NOT NULL,
    description varchar(1000),
    created_at  datetime(6)  NOT NULL,
    updated_at  datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_departments_college_code UNIQUE (college_id, code),
    CONSTRAINT fk_academic_departments_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_academic_departments_college_id ON academic_departments (college_id);

-- At most one session per college has is_current = 1; AcademicService keeps
-- that invariant. "current" is a reserved word, hence the column name.
CREATE TABLE academic_sessions
(
    id         varchar(36) NOT NULL,
    college_id varchar(36) NOT NULL,
    name       varchar(64) NOT NULL,
    start_date date        NOT NULL,
    end_date   date        NOT NULL,
    is_current bit         NOT NULL DEFAULT 0,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_sessions_college_name UNIQUE (college_id, name),
    CONSTRAINT fk_academic_sessions_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_academic_sessions_college_id ON academic_sessions (college_id);

CREATE TABLE academic_programs
(
    id             varchar(36)  NOT NULL,
    college_id     varchar(36)  NOT NULL,
    name           varchar(150) NOT NULL,
    code           varchar(32)  NOT NULL,
    duration_years integer,
    description    varchar(1000),
    department_id  varchar(36)  NOT NULL,
    created_at     datetime(6)  NOT NULL,
    updated_at     datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_programs_college_code UNIQUE (college_id, code),
    CONSTRAINT fk_academic_programs_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_academic_programs_department FOREIGN KEY (department_id) REFERENCES academic_departments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_academic_programs_college_id ON academic_programs (college_id);
CREATE INDEX idx_academic_programs_department_id ON academic_programs (department_id);

CREATE TABLE academic_semesters
(
    id                  varchar(36)  NOT NULL,
    college_id          varchar(36)  NOT NULL,
    number              integer      NOT NULL,
    name                varchar(100) NOT NULL,
    program_id          varchar(36)  NOT NULL,
    academic_session_id varchar(36),
    created_at          datetime(6)  NOT NULL,
    updated_at          datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_semesters_college_program_number UNIQUE (college_id, program_id, number),
    CONSTRAINT fk_academic_semesters_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_academic_semesters_program FOREIGN KEY (program_id) REFERENCES academic_programs (id),
    CONSTRAINT fk_academic_semesters_session FOREIGN KEY (academic_session_id) REFERENCES academic_sessions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_academic_semesters_college_id ON academic_semesters (college_id);
CREATE INDEX idx_academic_semesters_program_id ON academic_semesters (program_id);
CREATE INDEX idx_academic_semesters_academic_session_id ON academic_semesters (academic_session_id);

CREATE TABLE academic_subjects
(
    id          varchar(36)  NOT NULL,
    college_id  varchar(36)  NOT NULL,
    name        varchar(150) NOT NULL,
    code        varchar(32)  NOT NULL,
    credits     integer,
    description varchar(1000),
    semester_id varchar(36)  NOT NULL,
    created_at  datetime(6)  NOT NULL,
    updated_at  datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_subjects_college_code UNIQUE (college_id, code),
    CONSTRAINT fk_academic_subjects_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_academic_subjects_semester FOREIGN KEY (semester_id) REFERENCES academic_semesters (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_academic_subjects_college_id ON academic_subjects (college_id);
CREATE INDEX idx_academic_subjects_semester_id ON academic_subjects (semester_id);

-- ---------------------------------------------------------------------------
-- Academic records of people
-- ---------------------------------------------------------------------------
-- Optional identifiers are stored as NULL rather than '' so that the unique
-- constraints below tolerate many records without them.
CREATE TABLE students
(
    id                  varchar(36)  NOT NULL,
    college_id          varchar(36)  NOT NULL,
    user_id             varchar(255) NOT NULL,
    roll_number         varchar(64),
    registration_number varchar(64),
    father_name         varchar(150),
    blood_group         varchar(8),
    semester            integer,
    department_id       varchar(36),
    program_id          varchar(36),
    active              bit          NOT NULL DEFAULT 1,
    created_at          datetime(6)  NOT NULL,
    updated_at          datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_college_user UNIQUE (college_id, user_id),
    CONSTRAINT uk_students_college_roll_number UNIQUE (college_id, roll_number),
    CONSTRAINT uk_students_college_registration_number UNIQUE (college_id, registration_number),
    CONSTRAINT fk_students_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_students_college_id ON students (college_id);
CREATE INDEX idx_students_user_id ON students (user_id);
CREATE INDEX idx_students_department_id ON students (department_id);
CREATE INDEX idx_students_program_id ON students (program_id);

CREATE TABLE teachers
(
    id            varchar(36)  NOT NULL,
    college_id    varchar(36)  NOT NULL,
    user_id       varchar(255) NOT NULL,
    employee_id   varchar(64),
    designation   varchar(100),
    qualification varchar(255),
    department_id varchar(36),
    active        bit          NOT NULL DEFAULT 1,
    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teachers_college_user UNIQUE (college_id, user_id),
    CONSTRAINT uk_teachers_college_employee_id UNIQUE (college_id, employee_id),
    CONSTRAINT fk_teachers_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_teachers_college_id ON teachers (college_id);
CREATE INDEX idx_teachers_user_id ON teachers (user_id);
CREATE INDEX idx_teachers_department_id ON teachers (department_id);

CREATE TABLE teacher_subjects
(
    id         varchar(36) NOT NULL,
    college_id varchar(36) NOT NULL,
    teacher_id varchar(36) NOT NULL,
    subject_id varchar(36) NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_subjects_college_teacher_subject UNIQUE (college_id, teacher_id, subject_id),
    CONSTRAINT fk_teacher_subjects_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_teacher_subjects_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_teacher_subjects_college_id ON teacher_subjects (college_id);
CREATE INDEX idx_teacher_subjects_teacher_id ON teacher_subjects (teacher_id);
CREATE INDEX idx_teacher_subjects_subject_id ON teacher_subjects (subject_id);
