-- ============================================================
-- CAREERUP SEED DATA (seeds.sql)
-- Pre-populates database for live presentation / demonstration
-- ============================================================

-- Disable foreign key checks for clean seeding (MySQL syntax)
SET FOREIGN_KEY_CHECKS = 0;

-- 1. MAJORS / ACADEMIC DISCIPLINES
TRUNCATE TABLE Major;
INSERT INTO Major (major_id, major_name, department) VALUES
(1, 'Computer Science', 'College of Science & Technology'),
(2, 'Graphic Design', 'School of Journalism & Graphic Communication'),
(3, 'Business Marketing', 'School of Business & Industry'),
(4, 'Biomedical Engineering', 'College of Engineering');

-- 2. SKILLS
TRUNCATE TABLE Skill;
INSERT INTO Skill (skill_id, skill_name, category) VALUES
(1, 'Python', 'Technical'),
(2, 'UI/UX Design', 'Design'),
(3, 'Figma', 'Tools'),
(4, 'SQL', 'Technical'),
(5, 'Blender 3D', 'Design'),
(6, 'Social Media Strategy', 'Marketing');

-- 3. USERS (Students, Recruiters, Company Reps, Career Advisors, Admins)
TRUNCATE TABLE User;
INSERT INTO User (user_id, email, password_hash, first_name, last_name, role, bio) VALUES
-- Students
(101, 'jordan.student@famu.edu', 'hash_pass_101', 'Jordan', 'Taylor', 'Student', 'Computer Science major passionate about mobile app dev and 3D modeling.'),
(102, 'maya.design@famu.edu', 'hash_pass_102', 'Maya', 'Lin', 'Student', 'Visual storyteller and UI/UX designer looking for spring co-ops.'),
(103, 'alex.marketing@famu.edu', 'hash_pass_103', 'Alex', 'Rivera', 'Student', 'Marketing strategist showcasing social campaign analytics.'),
-- Recruiters
(201, 'recruiter@delta.com', 'hash_pass_201', 'Sarah', 'Jenkins', 'Recruiter', 'Tech Recruiter at Delta Air Lines hiring software interns.'),
(202, 'recruiter@truist.com', 'hash_pass_202', 'Marcus', 'Vance', 'Recruiter', 'Early Talent Acquisition Lead at Truist Bank.'),
-- Company Reps
(301, 'rep@delta.com', 'hash_pass_301', 'David', 'Miller', 'Company_Rep', 'Head of University Relations at Delta.'),
-- Career Advisor & Admin
(401, 'advisor@famu.edu', 'hash_pass_401', 'Dr. Patricia', 'Hayes', 'Career_Advisor', 'FAMU STEM Career Counselor.'),
(501, 'admin@careerup.io', 'hash_pass_501', 'System', 'Admin', 'Administrator', 'Platform Administrator.');

-- 4. STUDENT PROFILES
TRUNCATE TABLE Student_Profile;
INSERT INTO Student_Profile (user_id, major_id, graduation_year, gpa, portfolio_summary) VALUES
(101, 1, 2026, 3.40, 'Specializing in full-stack web applications and UI design.'),
(102, 2, 2027, 3.85, 'Brand identity, wireframing, and interactive prototyping.'),
(103, 3, 2026, 3.60, 'Data-driven marketing campaigns and digital growth strategies.');

-- 5. COMPANIES
TRUNCATE TABLE Company;
INSERT INTO Company (company_id, company_name, industry, website, location, description) VALUES
(1, 'Delta Air Lines', 'Aviation / Tech', 'https://delta.com', 'Atlanta, GA', 'Global aviation leader innovating customer tech experiences.'),
(2, 'Truist Financial', 'Banking / Fintech', 'https://truist.com', 'Charlotte, NC', 'Purpose-driven financial services enterprise.');

-- 6. PORTFOLIO POSTS
TRUNCATE TABLE Post;
INSERT INTO Post (post_id, student_id, title, description, media_url, company_id, verification_status, created_at) VALUES
(1001, 101, 'Knock-Activated Lock System', 'Built an Arduino-controlled servo lock with piezo sensors and custom LED indicators.', 'https://media.careerup.io/v/knock_lock.mp4', NULL, 'Not_Requested', '2026-10-15 14:30:00'),
(1002, 101, 'CareerUp Mobile UI Prototype', 'Designed a visual resume feed interface for college students using Figma.', 'https://media.careerup.io/v/careerup_demo.mp4', NULL, 'Not_Requested', '2026-10-20 09:15:00'),
(1003, 102, 'Delta Summer Internship Reel', 'Showcasing the updated seat-selection UI redesign completed during my summer internship.', 'https://media.careerup.io/v/delta_ui.mp4', 1, 'Approved', '2026-09-01 11:00:00'),
-- DEMO TARGET POST (Easy to delete live)
(9999, 103, '[DEMO DELETE] Test Marketing Reel', 'Temporary post created to demonstrate the live deletion functionality.', 'https://media.careerup.io/v/temp.mp4', NULL, 'Not_Requested', '2026-11-01 10:00:00');

-- 7. POST SKILLS (Many-to-Many Mapping)
TRUNCATE TABLE Post_Skill;
INSERT INTO Post_Skill (post_id, skill_id) VALUES
(1001, 1), -- Python / Hardware
(1002, 2), -- UI/UX
(1002, 3), -- Figma
(1003, 2), -- UI/UX
(1003, 3); -- Figma

-- 8. JOB / INTERNSHIP LISTINGS
TRUNCATE TABLE Job_Listing;
INSERT INTO Job_Listing (job_id, company_id, recruiter_id, title, employment_type, location, description, deadline) VALUES
(501, 1, 201, 'Software Engineering Co-Op (Spring 2027)', 'Co-Op', 'Atlanta, GA', 'Join our digital transformation team working on core flight API services.', '2026-12-01'),
(502, 2, 2022, 'Fintech Application Developer Intern', 'Internship', 'Remote / Charlotte', 'Develop banking mobile features with focus on security compliance.', '2026-11-30');

-- 9. JOB APPLICATIONS (Student submitting posts as credentials)
TRUNCATE TABLE Application;
INSERT INTO Application (application_id, job_id, student_id, attached_post_id, status, applied_at) VALUES
(801, 501, 101, 1002, 'Under_Review', '2026-10-25 16:00:00'),
(802, 501, 102, 1003, 'Interview_Scheduled', '2026-09-10 12:30:00');

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;