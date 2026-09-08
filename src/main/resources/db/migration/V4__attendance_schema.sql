-- Attendance sessions and per-student records.
--
-- The unique constraint below stops a second session for the same classroom,
-- date and subject. It cannot cover subject-less sessions, because MySQL treats
-- NULLs as distinct in a unique constraint; AttendanceService adds an explicit
-- "subject_id IS NULL" existence check for that case.

CREATE TABLE attendance_sessions
(
    id              varchar(36) NOT NULL,
    college_id      varchar(36) NOT NULL,
    classroom_id    varchar(36) NOT NULL,
    subject_id      varchar(36),
    teacher_user_id varchar(64) NOT NULL,
    session_date    date        NOT NULL,
    remark          varchar(500),
    locked          bit         NOT NULL DEFAULT 0,
    created_at      datetime(6) NOT NULL,
    updated_at      datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_sessions_classroom_date_subject
        UNIQUE (college_id, classroom_id, session_date, subject_id),
    CONSTRAINT fk_attendance_sessions_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_sessions_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_sessions_subject FOREIGN KEY (subject_id) REFERENCES academic_subjects (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_sessions_college_id ON attendance_sessions (college_id);
CREATE INDEX idx_attendance_sessions_classroom_id ON attendance_sessions (classroom_id);
CREATE INDEX idx_attendance_sessions_session_date ON attendance_sessions (session_date);

CREATE TABLE attendance_records
(
    id                    varchar(36) NOT NULL,
    college_id            varchar(36) NOT NULL,
    attendance_session_id varchar(36) NOT NULL,
    student_user_id       varchar(64) NOT NULL,
    status                varchar(20) NOT NULL,
    remark                varchar(500),
    created_at            datetime(6) NOT NULL,
    updated_at            datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_records_session_student UNIQUE (attendance_session_id, student_user_id),
    CONSTRAINT ck_attendance_records_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    CONSTRAINT fk_attendance_records_college FOREIGN KEY (college_id) REFERENCES colleges (college_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_records_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_records_student FOREIGN KEY (student_user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_records_college_id ON attendance_records (college_id);
CREATE INDEX idx_attendance_records_session_id ON attendance_records (attendance_session_id);
CREATE INDEX idx_attendance_records_student_user_id ON attendance_records (student_user_id);

-- Supports the per-student attendance percentage aggregate.
CREATE INDEX idx_attendance_records_student_status ON attendance_records (college_id, student_user_id, status);
