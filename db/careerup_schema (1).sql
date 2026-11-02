-- CareerUp Database Schema
-- MySQL 8.0+
-- Run this file in MySQL Workbench or your MySQL client.
-- WARNING: The DROP DATABASE line is intentionally omitted so existing data is not deleted.
-- Create the database first, or uncomment the CREATE DATABASE / USE lines below.

-- CREATE DATABASE careerup CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- USE careerup;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables in dependency-safe order if you need to rebuild the schema.
-- These DROP statements are commented out to prevent accidental data loss.
-- DROP TABLE IF EXISTS reports;
-- DROP TABLE IF EXISTS advisor_notes;
-- DROP TABLE IF EXISTS advisor_students;
-- DROP TABLE IF EXISTS notifications;
-- DROP TABLE IF EXISTS messages;
-- DROP TABLE IF EXISTS invitations;
-- DROP TABLE IF EXISTS application_posts;
-- DROP TABLE IF EXISTS applications;
-- DROP TABLE IF EXISTS job_majors;
-- DROP TABLE IF EXISTS job_skills;
-- DROP TABLE IF EXISTS jobs;
-- DROP TABLE IF EXISTS interactions;
-- DROP TABLE IF EXISTS comments;
-- DROP TABLE IF EXISTS post_skills;
-- DROP TABLE IF EXISTS post_media;
-- DROP TABLE IF EXISTS posts;
-- DROP TABLE IF EXISTS student_skills;
-- DROP TABLE IF EXISTS settings;
-- DROP TABLE IF EXISTS role_permissions;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS companies;
-- DROP TABLE IF EXISTS industries;
-- DROP TABLE IF EXISTS skills;
-- DROP TABLE IF EXISTS majors;
USE CareerUpDatabase;
CREATE TABLE IF NOT EXISTS industries (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_industries_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS majors (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    discipline VARCHAR(150) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_majors_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS skills (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_skills_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS companies (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    industry_id INT UNSIGNED NULL,
    description TEXT NULL,
    logo_url VARCHAR(2048) NULL,
    website VARCHAR(2048) NULL,
    locations TEXT NULL,
    culture TEXT NULL,
    status ENUM('PENDING','ACTIVE','REJECTED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_companies_industry_id (industry_id),
    KEY idx_companies_status (status),
    CONSTRAINT fk_companies_industry
        FOREIGN KEY (industry_id) REFERENCES industries(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT','RECRUITER','COMPANY_REP','CAREER_ADVISOR','ADMIN') NOT NULL,
    status ENUM('PENDING','ACTIVE','SUSPENDED','BANNED','INACTIVE') NOT NULL DEFAULT 'PENDING',
    name VARCHAR(150) NOT NULL,
    photo_url VARCHAR(2048) NULL,
    phone VARCHAR(30) NULL,
    organization VARCHAR(255) NULL,
    company_id INT UNSIGNED NULL,
    major_id INT UNSIGNED NULL,
    school VARCHAR(255) NULL,
    grad_year SMALLINT UNSIGNED NULL,
    bio TEXT NULL,
    interests TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role_status (role, status),
    KEY idx_users_major_id (major_id),
    KEY idx_users_grad_year (grad_year),
    KEY idx_users_company_id (company_id),
    KEY idx_users_name (name),
    CONSTRAINT fk_users_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_users_major
        FOREIGN KEY (major_id) REFERENCES majors(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    role ENUM('STUDENT','RECRUITER','COMPANY_REP','CAREER_ADVISOR','ADMIN') NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS student_skills (
    student_id INT UNSIGNED NOT NULL,
    skill_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (student_id, skill_id),
    KEY idx_student_skills_skill_id (skill_id),
    CONSTRAINT fk_student_skills_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS posts (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_id INT UNSIGNED NOT NULL,
    company_id INT UNSIGNED NULL,
    major_id INT UNSIGNED NULL,
    post_type ENUM('PORTFOLIO','COMPANY') NOT NULL DEFAULT 'PORTFOLIO',
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    verification_status ENUM('NOT_REQUESTED','PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'NOT_REQUESTED',
    verified_by INT UNSIGNED NULL,
    verified_at DATETIME NULL,
    is_showcased BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('DRAFT','PUBLISHED','HIDDEN','REMOVED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_posts_author_id (author_id),
    KEY idx_posts_company_id (company_id),
    KEY idx_posts_major_id (major_id),
    KEY idx_posts_verification_status (verification_status),
    KEY idx_posts_status_created (status, created_at),
    FULLTEXT KEY ft_posts_title_description (title, description),
    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_posts_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_posts_major
        FOREIGN KEY (major_id) REFERENCES majors(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_posts_verified_by
        FOREIGN KEY (verified_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_media (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id INT UNSIGNED NOT NULL,
    media_type ENUM('VIDEO','IMAGE') NOT NULL,
    url VARCHAR(2048) NOT NULL,
    duration_seconds INT UNSIGNED NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_media_post_id (post_id),
    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_skills (
    post_id INT UNSIGNED NOT NULL,
    skill_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (post_id, skill_id),
    KEY idx_post_skills_skill_id (skill_id),
    CONSTRAINT fk_post_skills_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_post_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS comments (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id INT UNSIGNED NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    content TEXT NOT NULL,
    status ENUM('VISIBLE','HIDDEN','REMOVED') NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comments_post_created (post_id, created_at),
    KEY idx_comments_user_id (user_id),
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS interactions (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    target_type ENUM('POST','COMPANY','STUDENT','JOB') NOT NULL,
    target_id INT UNSIGNED NOT NULL,
    type ENUM('VIEW','LIKE','SAVE','SHARE','FOLLOW') NOT NULL,
    list_name VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_interactions_target (target_type, target_id, type),
    KEY idx_interactions_user_created (user_id, created_at),
    KEY idx_interactions_user_target_type (user_id, target_type, target_id, type),
    CONSTRAINT fk_interactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
    -- target_id is polymorphic and intentionally has no single foreign key.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS jobs (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_id INT UNSIGNED NOT NULL,
    posted_by INT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NULL,
    employment_type ENUM('FULL_TIME','PART_TIME','INTERNSHIP','CONTRACT','OTHER') NOT NULL,
    deadline DATE NULL,
    status ENUM('DRAFT','PUBLISHED','CLOSED','EXPIRED','REMOVED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_jobs_company_id (company_id),
    KEY idx_jobs_posted_by (posted_by),
    KEY idx_jobs_status_deadline (status, deadline),
    FULLTEXT KEY ft_jobs_title_description (title, description),
    CONSTRAINT fk_jobs_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_posted_by
        FOREIGN KEY (posted_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS job_skills (
    job_id INT UNSIGNED NOT NULL,
    skill_id INT UNSIGNED NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (job_id, skill_id),
    KEY idx_job_skills_skill_id (skill_id),
    CONSTRAINT fk_job_skills_job
        FOREIGN KEY (job_id) REFERENCES jobs(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_job_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS job_majors (
    job_id INT UNSIGNED NOT NULL,
    major_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (job_id, major_id),
    KEY idx_job_majors_major_id (major_id),
    CONSTRAINT fk_job_majors_job
        FOREIGN KEY (job_id) REFERENCES jobs(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_job_majors_major
        FOREIGN KEY (major_id) REFERENCES majors(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS applications (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_id INT UNSIGNED NOT NULL,
    student_id INT UNSIGNED NOT NULL,
    status ENUM('SUBMITTED','IN_REVIEW','INTERVIEW','OFFERED','REJECTED','WITHDRAWN') NOT NULL DEFAULT 'SUBMITTED',
    recruiter_notes TEXT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_applications_job_student (job_id, student_id),
    KEY idx_applications_student_status (student_id, status),
    CONSTRAINT fk_applications_job
        FOREIGN KEY (job_id) REFERENCES jobs(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_applications_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS application_posts (
    application_id INT UNSIGNED NOT NULL,
    post_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (application_id, post_id),
    KEY idx_application_posts_post_id (post_id),
    CONSTRAINT fk_application_posts_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_application_posts_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS invitations (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_id INT UNSIGNED NOT NULL,
    student_id INT UNSIGNED NOT NULL,
    recruiter_id INT UNSIGNED NOT NULL,
    message TEXT NULL,
    status ENUM('SENT','ACCEPTED','DECLINED','EXPIRED') NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_invitations_job_student (job_id, student_id),
    KEY idx_invitations_student_status (student_id, status),
    KEY idx_invitations_recruiter_id (recruiter_id),
    CONSTRAINT fk_invitations_job
        FOREIGN KEY (job_id) REFERENCES jobs(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_invitations_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_invitations_recruiter
        FOREIGN KEY (recruiter_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS messages (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    sender_id INT UNSIGNED NOT NULL,
    receiver_id INT UNSIGNED NOT NULL,
    application_id INT UNSIGNED NULL,
    content TEXT NOT NULL,
    status ENUM('SENT','DELIVERED','READ','DELETED') NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_messages_sender_created (sender_id, created_at),
    KEY idx_messages_receiver_created (receiver_id, created_at),
    KEY idx_messages_application_id (application_id),
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_messages_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    type VARCHAR(100) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id INT UNSIGNED NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read_created (user_id, is_read, created_at),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE
    -- reference_id is polymorphic and intentionally has no single foreign key.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS advisor_students (
    advisor_id INT UNSIGNED NOT NULL,
    student_id INT UNSIGNED NOT NULL,
    access_type ENUM('ASSIGNED','AUTHORIZED') NOT NULL DEFAULT 'ASSIGNED',
    status ENUM('PENDING','ACTIVE','REVOKED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (advisor_id, student_id),
    KEY idx_advisor_students_student_id (student_id),
    CONSTRAINT fk_advisor_students_advisor
        FOREIGN KEY (advisor_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_advisor_students_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS advisor_notes (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    advisor_id INT UNSIGNED NOT NULL,
    student_id INT UNSIGNED NOT NULL,
    post_id INT UNSIGNED NULL,
    job_id INT UNSIGNED NULL,
    kind ENUM('FEEDBACK','OPPORTUNITY','CONTENT') NOT NULL,
    content TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_advisor_notes_student_created (student_id, created_at),
    KEY idx_advisor_notes_advisor_id (advisor_id),
    CONSTRAINT fk_advisor_notes_advisor
        FOREIGN KEY (advisor_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_advisor_notes_student
        FOREIGN KEY (student_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_advisor_notes_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_advisor_notes_job
        FOREIGN KEY (job_id) REFERENCES jobs(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reports (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    reporter_id INT UNSIGNED NOT NULL,
    target_type ENUM('POST','COMMENT','USER','MESSAGE','JOB','COMPANY') NOT NULL,
    target_id INT UNSIGNED NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('OPEN','REVIEWING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'OPEN',
    resolved_by INT UNSIGNED NULL,
    action_taken TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_reports_status_created (status, created_at),
    KEY idx_reports_target (target_type, target_id),
    KEY idx_reports_reporter_id (reporter_id),
    CONSTRAINT fk_reports_reporter
        FOREIGN KEY (reporter_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_reports_resolved_by
        FOREIGN KEY (resolved_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL
    -- target_id is polymorphic and intentionally has no single foreign key.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- Optional starter permissions. Adjust these to match your application authorization design.
INSERT IGNORE INTO role_permissions (role, permission) VALUES
('ADMIN', 'MANAGE_USERS'),
('ADMIN', 'VERIFY_COMPANY'),
('ADMIN', 'MODERATE_CONTENT'),
('ADMIN', 'MANAGE_SETTINGS'),
('RECRUITER', 'MANAGE_JOBS'),
('RECRUITER', 'REVIEW_APPLICATIONS'),
('COMPANY_REP', 'MANAGE_COMPANY_PROFILE'),
('COMPANY_REP', 'VERIFY_COLLABORATION'),
('CAREER_ADVISOR', 'PROVIDE_STUDENT_FEEDBACK'),
('CAREER_ADVISOR', 'RECOMMEND_OPPORTUNITIES'),
('STUDENT', 'MANAGE_OWN_PROFILE'),
('STUDENT', 'CREATE_PORTFOLIO_POSTS'),
('STUDENT', 'APPLY_TO_JOBS');
