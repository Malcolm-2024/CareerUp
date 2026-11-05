-- ============================================================
-- CAREERUP SEED DATA (seeds.sql) - matches careerup_schema.sql
-- Run AFTER the schema file, against the same database:
USE CareerUpDatabase;
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Clear tables this script fills (safe to re-run)
TRUNCATE TABLE application_posts;
TRUNCATE TABLE applications;
TRUNCATE TABLE job_skills;
TRUNCATE TABLE job_majors;
TRUNCATE TABLE jobs;
TRUNCATE TABLE post_skills;
TRUNCATE TABLE post_media;
TRUNCATE TABLE posts;
TRUNCATE TABLE student_skills;
TRUNCATE TABLE users;
TRUNCATE TABLE companies;
TRUNCATE TABLE skills;
TRUNCATE TABLE majors;
TRUNCATE TABLE industries;

-- 1. INDUSTRIES (new table in the schema; companies reference it)
INSERT INTO industries (id, name) VALUES
                                      (1, 'Aviation / Tech'),
                                      (2, 'Banking / Fintech');

-- 2. MAJORS  (schema columns: id, name, discipline)
INSERT INTO majors (id, name, discipline) VALUES
                                              (1, 'Computer Science',      'College of Science & Technology'),
                                              (2, 'Graphic Design',        'School of Journalism & Graphic Communication'),
                                              (3, 'Business Marketing',    'School of Business & Industry'),
                                              (4, 'Biomedical Engineering','College of Engineering');

-- 3. SKILLS  (schema has no "category" column)
INSERT INTO skills (id, name) VALUES
                                  (1, 'Python'),
                                  (2, 'UI/UX Design'),
                                  (3, 'Figma'),
                                  (4, 'SQL'),
                                  (5, 'Blender 3D'),
                                  (6, 'Social Media Strategy');

-- 4. COMPANIES  (must come before users, since users.company_id references it)
INSERT INTO companies (id, name, industry_id, description, website, locations, status, is_verified) VALUES
                                                                                                        (1, 'Delta Air Lines',  1, 'Global aviation leader innovating customer tech experiences.', 'https://delta.com',  'Atlanta, GA',   'ACTIVE', TRUE),
                                                                                                        (2, 'Truist Financial', 2, 'Purpose-driven financial services enterprise.',                'https://truist.com', 'Charlotte, NC', 'ACTIVE', TRUE);

-- 5. USERS  (single "name" column, UPPERCASE enums, student fields live here)
INSERT INTO users (id, email, password_hash, role, status, name, school, grad_year, major_id, company_id, bio, interests) VALUES
-- Students
(101, 'jordan.student@famu.edu',   'hash_pass_101', 'ADMIN', 'ACTIVE', 'Jordan Monts', 'Florida A&M University', 2026, 1, NULL,
 'Computer Science major passionate about mobile app dev and 3D modeling.',
 'Full-stack web applications and UI design.'),
(102, 'maya.design@famu.edu',      'hash_pass_102', 'STUDENT', 'ACTIVE', 'Maya Lin',      'Florida A&M University', 2027, 2, NULL,
 'Visual storyteller and UI/UX designer looking for spring co-ops.',
 'Brand identity, wireframing, and interactive prototyping.'),
(103, 'alex.marketing@famu.edu',   'hash_pass_103', 'STUDENT', 'ACTIVE', 'Alex Rivera',   'Florida A&M University', 2026, 3, NULL,
 'Marketing strategist showcasing social campaign analytics.',
 'Data-driven marketing campaigns and digital growth strategies.'),
-- Recruiters (tied to their company)
(201, 'recruiter@delta.com',       'hash_pass_201', 'RECRUITER',      'ACTIVE', 'Sarah Jenkins',  NULL, NULL, NULL, 1,
 'Tech Recruiter at Delta Air Lines hiring software interns.', NULL),
(202, 'recruiter@truist.com',      'hash_pass_202', 'RECRUITER',      'ACTIVE', 'Marcus Vance',   NULL, NULL, NULL, 2,
 'Early Talent Acquisition Lead at Truist Bank.', NULL),
-- Company rep
(301, 'rep@delta.com',             'hash_pass_301', 'COMPANY_REP',    'ACTIVE', 'David Miller',   NULL, NULL, NULL, 1,
 'Head of University Relations at Delta.', NULL),
-- Career advisor & admin
(401, 'advisor@famu.edu',          'hash_pass_401', 'CAREER_ADVISOR', 'ACTIVE', 'Dr. Patricia Hayes', 'Florida A&M University', NULL, NULL, NULL,
 'FAMU STEM Career Counselor.', NULL),
(501, 'admin@careerup.io',         'hash_pass_501', 'ADMIN',          'ACTIVE', 'System Admin',   NULL, NULL, NULL, NULL,
 'Platform Administrator.', NULL);

-- 6. STUDENT SKILLS (replaces the old Student_Profile table)
INSERT INTO student_skills (student_id, skill_id) VALUES
                                                      (101, 1), (101, 2), (101, 3), (101, 5),
                                                      (102, 2), (102, 3),
                                                      (103, 6);

-- 7. POSTS  (author_id, post_type, status; media moved to post_media)
INSERT INTO posts (id, author_id, company_id, major_id, post_type, title, description, verification_status, verified_by, verified_at, status, created_at) VALUES
                                                                                                                                                              (1001, 101, NULL, 1, 'PORTFOLIO', 'Knock-Activated Lock System',
                                                                                                                                                               'Built an Arduino-controlled servo lock with piezo sensors and custom LED indicators.',
                                                                                                                                                               'NOT_REQUESTED', NULL, NULL, 'PUBLISHED', '2026-10-15 14:30:00'),
                                                                                                                                                              (1002, 101, NULL, 1, 'PORTFOLIO', 'CareerUp Mobile UI Prototype',
                                                                                                                                                               'Designed a visual resume feed interface for college students using Figma.',
                                                                                                                                                               'NOT_REQUESTED', NULL, NULL, 'PUBLISHED', '2026-10-20 09:15:00'),
                                                                                                                                                              (1003, 102, 1,    2, 'COMPANY',   'Delta Summer Internship Reel',
                                                                                                                                                               'Showcasing the updated seat-selection UI redesign completed during my summer internship.',
                                                                                                                                                               'APPROVED', 301, '2026-09-03 10:00:00', 'PUBLISHED', '2026-09-01 11:00:00'),
-- DEMO TARGET POST (easy to delete live)
                                                                                                                                                              (9999, 103, NULL, 3, 'PORTFOLIO', '[DEMO DELETE] Test Marketing Reel',
                                                                                                                                                               'Temporary post created to demonstrate the live deletion functionality.',
                                                                                                                                                               'NOT_REQUESTED', NULL, NULL, 'PUBLISHED', '2026-11-01 10:00:00');

-- 8. POST MEDIA (was posts.media_url)
INSERT INTO post_media (post_id, media_type, url, sort_order) VALUES
                                                                  (1001, 'VIDEO', 'https://media.careerup.io/v/knock_lock.mp4',     0),
                                                                  (1002, 'VIDEO', 'https://media.careerup.io/v/careerup_demo.mp4',  0),
                                                                  (1003, 'VIDEO', 'https://media.careerup.io/v/delta_ui.mp4',       0),
                                                                  (9999, 'VIDEO', 'https://media.careerup.io/v/temp.mp4',           0);

-- 9. POST SKILLS
INSERT INTO post_skills (post_id, skill_id) VALUES
                                                (1001, 1),  -- Python / hardware
                                                (1002, 2),  -- UI/UX
                                                (1002, 3),  -- Figma
                                                (1003, 2),
                                                (1003, 3);

-- 10. JOBS  (table is "jobs", recruiter column is "posted_by",
--     employment_type enum has no 'Co-Op' so INTERNSHIP is used)
INSERT INTO jobs (id, company_id, posted_by, title, employment_type, location, description, deadline, status) VALUES
                                                                                                                  (501, 1, 201, 'Software Engineering Co-Op (Spring 2027)', 'INTERNSHIP', 'Atlanta, GA',
                                                                                                                   'Join our digital transformation team working on core flight API services.', '2026-12-01', 'PUBLISHED'),
                                                                                                                  (502, 2, 202, 'Fintech Application Developer Intern',     'INTERNSHIP', 'Remote / Charlotte',
                                                                                                                   'Develop banking mobile features with focus on security compliance.',        '2026-11-30', 'PUBLISHED');

INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
                                                           (501, 1, TRUE), (501, 4, TRUE),
                                                           (502, 1, TRUE), (502, 4, FALSE);

INSERT INTO job_majors (job_id, major_id) VALUES
                                              (501, 1), (502, 1);

-- 11. APPLICATIONS  (no attached_post_id column; use application_posts)
INSERT INTO applications (id, job_id, student_id, status, submitted_at) VALUES
                                                                            (801, 501, 101, 'IN_REVIEW', '2026-10-25 16:00:00'),
                                                                            (802, 501, 102, 'INTERVIEW', '2026-09-10 12:30:00');

INSERT INTO application_posts (application_id, post_id) VALUES
                                                            (801, 1002),
                                                            (802, 1003);

SET FOREIGN_KEY_CHECKS = 1;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

USE CareerUpDatabase;


INSERT INTO users (id, email, password_hash, role, status, name)
VALUES (802, 'fdouglass@famu.edu', 'hash_pass_802', 'STUDENT', 'ACTIVE', 'Frederick Douglass');


SELECT * FROM users WHERE id = 802;