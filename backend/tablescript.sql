-- RTS manual schema (MySQL 8.0). Aligns with JPA entities in com.rts.*
-- Run once per database, e.g.: mysql -u root -p < tablescript.sql
-- Dev profile may use ddl-auto=update; production should keep this script authoritative.
--
-- If you already have an older rts_dev from a previous script (first_name/last_name):
--   1) Back up data, then either drop and recreate candidates + dependent tables, or
--   2) ALTER TABLE candidates ADD COLUMN name VARCHAR(150) NOT NULL AFTER id;
--      migrate data; DROP first_name, last_name; ALTER phone SET NOT NULL; etc.

CREATE DATABASE IF NOT EXISTS rts_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rts_dev;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidates (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    position VARCHAR(100) NOT NULL,
    stage VARCHAR(50) NOT NULL,
    experience VARCHAR(200) NULL,
    notes VARCHAR(500) NULL,
    eval_score DECIMAL(4,2) NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_candidates_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_documents (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    linked_at DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidate_documents_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stage_history (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stage_history_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interviews (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    round VARCHAR(20) NOT NULL,
    date_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    meeting_link VARCHAR(500) NULL,
    location VARCHAR(255) NULL,
    notes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_interviews_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_interviewers (
    interview_id VARCHAR(36) NOT NULL,
    interviewer_username VARCHAR(100) NOT NULL,
    CONSTRAINT fk_interview_interviewers_interview FOREIGN KEY (interview_id) REFERENCES interviews (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feedback (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    interview_id VARCHAR(36) NOT NULL,
    candidate_id VARCHAR(36) NOT NULL,
    submitted_by_username VARCHAR(100) NOT NULL,
    technical_rating INT NOT NULL,
    communication_rating INT NOT NULL,
    problem_solving_rating INT NOT NULL,
    leadership_rating INT NOT NULL,
    culture_rating INT NOT NULL,
    recommendation VARCHAR(20) NOT NULL,
    comments VARCHAR(1000) NULL,
    submitted_at DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_interview FOREIGN KEY (interview_id) REFERENCES interviews (id),
    CONSTRAINT fk_feedback_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id),
    UNIQUE KEY uk_feedback_interview_submitter (interview_id, submitted_by_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Manual patches for databases created before eval_score / feedback (Day 18).
-- Idempotent: safe to re-run. Fresh installs already get these from CREATE above.
-- ---------------------------------------------------------------------------

-- candidates.eval_score (no-op if column already exists)
SET @patch_eval_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'candidates'
              AND COLUMN_NAME = 'eval_score'
        ),
        'SELECT 1',
        'ALTER TABLE candidates ADD COLUMN eval_score DECIMAL(4,2) NULL AFTER notes'
    )
);
PREPARE patch_eval_score_stmt FROM @patch_eval_score;
EXECUTE patch_eval_score_stmt;
DEALLOCATE PREPARE patch_eval_score_stmt;

-- feedback: defined once above (CREATE TABLE IF NOT EXISTS feedback). Re-running this script
-- on an old DB creates that table if it was missing; no separate ALTER required.

-- Optional seed admin (password: Admin@123). BCrypt strength 12, Spring-compatible ($2a$).
-- With the default app profile, DataSeeder also creates this user if missing.
INSERT INTO users (id, username, email, password, role, is_deleted, created_by, updated_by)
VALUES (
    UUID(),
    'admin',
    'admin@rts.com',
    '$2a$12$YlfvzJCzCUdkv.PFmu/r0.CkqNxwhrk/Ir70ibuf5BVCnlzuVl0su',
    'ADMIN',
    0,
    NULL,
    NULL
)
ON DUPLICATE KEY UPDATE username = username;
