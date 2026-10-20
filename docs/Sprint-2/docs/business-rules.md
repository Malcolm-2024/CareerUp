# CareerUp business rules and constraints

41 traced rules · preserved 48-table logical design · October 10, 2026. 

**Functional requirement** identifies behavior in the FRD. **Audited design assumption** identifies a schema/workflow choice beyond the literal requirement. **Proposed policy / unresolved** is not an approved requirement. Some requirement-backed rules contain explicit audited limits; these are identified in the explanation and decisions. “Database” means declared logical constraints, not a deployed or SQL-tested database.

FKs validate identity and matching tuples; they do not authorize users, enforce parent minimum-child counts, or synchronize history. NOT NULL rejects null, not empty text. UNIQUE semantics depend on eventual collation, and nullable tuples use normal SQL null semantics.

## Rule register

## BR01 — Accounts select a supported role

Basis: **Functional requirement**. Source: All Roles: Account Registration; User Roles.

Entities: `users`, `roles`, `permissions`, `role_permissions`.

Enforcement: **FK, NOT NULL, PK, UNIQUE + authorization**.

users.role_id requires one existing role. role_permissions has a composite PK preventing duplicate capability grants. Permission checks, administrator provisioning and role eligibility are application authorization. One role per account is an audited design choice; the FRD does not explicitly prohibit multiple roles.

**Team decision:** Confirm whether one account may hold multiple roles.

## BR02 — Secure sign-in uses account credentials and effective access state

Basis: **Functional requirement**. Source: All Roles: Sign-in/Sign-out.

Entities: `users`.

Enforcement: **NOT NULL, UNIQUE + authorization**.

Email and password_hash are required; email is unique under the eventual collation. Hashing, credential verification, session termination, account-state restrictions and case-normalized email treatment are application concerns. No sessions table or automatic timestamp default is declared.

**Team decision:** Choose email normalization/collation and session policy.

## BR03 — Role-appropriate profiles share account identity

Basis: **Functional requirement**. Source: All Roles: Profile Management; Student: Manage Student Profile and Portfolio.

Entities: `users`, `student_profiles`, `schools`, `majors`.

Enforcement: **PK/FK, NOT NULL + authorization / transaction**.

Every student profile references exactly one user via its PK/FK; all-role users have 0..1 student profile. Student-role registration requires a profile transactionally. Major and graduation year are NOT NULL, while school is optional. FRD lists profile information without mandating registration completeness.

**Team decision:** Confirm whether incomplete registration needs a draft profile.

## BR04 — Account approval decisions are attributable

Basis: **Functional requirement**. Source: All Roles: Account Registration; Administrator: Manage User Accounts.

Entities: `account_reviews`, `users`.

Enforcement: **PK/FK, NOT NULL, CHECK + authorization / transaction**.

account_reviews excludes PENDING and self-review using CHECKs; subject/reviewer must exist. Reviewer must be an Administrator. Update effective users.status with the decision atomically. Whether every employer/advisor account needs approval is not fixed by the FRD.

**Team decision:** Define which roles require approval and allowed state transitions.

## BR05 — Companies have official information and administrative approval

Basis: **Functional requirement**. Source: Company Representative: Manage Company Profile; Administrator: Manage Company Accounts.

Entities: `companies`, `industries`, `company_locations`, `users`.

Enforcement: **PK/FK, NOT NULL + authorization / transaction**.

Company industry is required; locations are repeating children. Approver/time are nullable, and there is no state/time CHECK. Only authorized actors may maintain profiles or approve companies. Company names need not be globally unique.

**Team decision:** Define approval evidence, lifecycle transitions and whether a single headquarters is required.

## BR06 — Employer access depends on approved company affiliation

Basis: **Functional requirement**. Source: Company Representative: Manage Company Recruiting Team; All Roles: Account Registration.

Entities: `company_memberships`, `companies`, `users`, `roles`.

Enforcement: **FK, NOT NULL, UNIQUE + authorization**.

UNIQUE(company_id,user_id) permits one current membership per pair. FK proves affiliation records exist; it does not prove Recruiter/Company Representative role, ACTIVE membership, ACTIVE account or approval authority. These checks must be performed at the action.

**Team decision:** Define who approves/revokes memberships and whether membership history is needed.

## BR07 — Students publish portfolio media posts

Basis: **Functional requirement**. Source: Student: Create Portfolio Post; Additional 1, 12.

Entities: `posts`, `users`, `student_profiles`, `post_media`.

Enforcement: **FK, NOT NULL, CHECK + authorization / transaction**.

Author, title and post_type are required. Portfolio authors must be Students by authorization, not the users FK. At publication require 1..* media children and commit content/media/tags atomically; ordinary FKs permit a post with no media. Description is nullable because the FRD says may include. Title NOT NULL does not prohibit an empty string.

## BR08 — Company content belongs to an authorized publishing company

Basis: **Functional requirement**. Source: Company Representative: Publish Company Content.

Entities: `posts`, `companies`, `company_memberships`, `users`.

Enforcement: **FK, CHECK + authorization**.

CHECK requires company_id for COMPANY posts. An ACTIVE same-company Company Representative must publish it; the FK does not check role or membership.

## BR09 — Portfolio and discovery views reuse the same content

Basis: **Functional requirement**. Source: Student: Manage Student Profile and Portfolio; Create Portfolio Post; Browse and Interact With Discovery Feed.

Entities: `posts`.

Enforcement: **NOT NULL/defaults + query / authorization**.

portfolio_visible and feed_visible are independent booleans. Both default true. Feed and portfolio are views over the same record, with status/privacy eligibility validated by the service; there are no duplicate feed or portfolio tables.

## BR10 — Post academic classification avoids conflicting categories

Basis: **Functional requirement**. Source: Create Portfolio Post; Additional 2, 4.

Entities: `posts`, `majors`, `academic_disciplines`.

Enforcement: **FK, CHECK**.

Both classification columns are optional. CHECK prevents major_id and discipline_id being non-null together. Derive the broad discipline through majors when major_id is supplied. Required student major does not require every post to copy it.

## BR11 — Media files have ordered metadata

Basis: **Functional requirement**. Source: Create Portfolio Post; Additional 1.

Entities: `post_media`, `posts`.

Enforcement: **FK, NOT NULL, UNIQUE, CHECK + publication validation**.

Each media belongs to one post. UNIQUE(post_id,display_order) and display_order>=1 prevent duplicate/nonpositive positions. Duration and file size, if supplied, must be positive. Actual file existence, allowed types and video duration require storage/application validation. FRD describes 30–60-second video; no range CHECK exists.

**Team decision:** Resolve Sprint I up-to-60 wording versus FRD 30–60 and configure authoritative limits.

## BR12 — Standardized skills support multiple tags without duplicates

Basis: **Functional requirement**. Source: Student: Manage Skill Tags; Additional 5.

Entities: `skills`, `student_skills`, `post_skills`, `student_profiles`, `posts`.

Enforcement: **PK/FK, NOT NULL, UNIQUE + application validation**.

Composite PKs prevent repeated student/skill and post/skill pairs, not multiple skills. A skill may tag many students/posts; either parent may have no tag links. Active/approved selection is application validation; an FK allows an existing inactive skill.

## BR13 — Discovery is independent of applications

Basis: **Functional requirement**. Source: Recruiter: Search and Discover Students; Review Student Portfolio; Additional 4, 8.

Entities: `users`, `student_profiles`, `majors`, `academic_disciplines`, `student_skills`, `posts`, `post_skills`, `collaboration_verifications`.

Enforcement: **FK relationships + authorized queries**.

Recruiter filters join profile, skills, categories and verification without requiring an application. Availability/privacy checks are authorization. No separate search/discovery entity is required.

## BR14 — Company tagging is optional and does not prove collaboration

Basis: **Functional requirement**. Source: Student: Participate in Verified Collaboration; Additional 6.

Entities: `posts`, `companies`, `collaboration_verifications`.

Enforcement: **Nullable FK + authorization**.

A PORTFOLIO post may reference 0..1 tagged company. Creating that tag alone must not display Verified Collaboration. Verification requests must concern student-owned portfolio content.

## BR15 — A current verification matches the post and tagged company

Basis: **Functional requirement**. Source: Participate in Verified Collaboration; Company Representative: Verify Student Collaboration.

Entities: `collaboration_verifications`, `posts`, `companies`.

Enforcement: **FK, NOT NULL, UNIQUE**.

Required composite FK (post_id,company_id) references the matching posts tuple. UNIQUE(post_id) makes each post have 0..1 current request. Each request has exactly one post and one company. One current request per post, rather than request-event history, is an audited assumption.

**Team decision:** Confirm whether rejected requests can be resubmitted in place and whether decision history is required.

## BR16 — Only an authorized representative approves a collaboration

Basis: **Functional requirement**. Source: Additional 6; Company Representative: Verify Student Collaboration.

Entities: `collaboration_verifications`, `users`, `roles`, `company_memberships`.

Enforcement: **FK, CHECK + authorization / transaction**.

Reviewer FK only proves a user exists. Check Company Representative role, active account and ACTIVE membership in the request company at decision time. CHECK requires reviewer/time for APPROVED or REJECTED; PENDING can technically retain reviewer/time. Legal transitions and request notification are application/transaction logic.

## BR17 — Approved work receives a derived verified indicator

Basis: **Functional requirement**. Source: Participate in Verified Collaboration; Additional 6, 7.

Entities: `posts`, `collaboration_verifications`.

Enforcement: **Query / authorization**.

Derive the badge from the approved current request; do not duplicate a verified flag in posts. Same content can be shown on student/company pages. Claim-changing edits must invalidate/reset approval according to the audit policy, enforced transactionally.

**Team decision:** Define which edits require re-verification and how earlier application evidence is retained.

## BR18 — Only approved content can be showcased

Basis: **Functional requirement**. Source: Company Representative: Manage Collaborative Showcases; Additional 7.

Entities: `collaboration_verifications`, `companies`, `posts`, `company_memberships`.

Enforcement: **CHECK + authorization / application validation**.

CHECK prevents SHOWCASED unless APPROVED. Representative selection, same-company access, showcase actor/time and valid ordering are application checks; showcase_order has no positive/unique constraint. FRD overview suggests appearing on both pages after approval, while the detailed showcase requirement permits selection.

**Team decision:** Confirm automatic company display versus representative selection; distinguish verified-work listing from selected showcase.

## BR19 — Social interactions have attributable users and valid targets

Basis: **Functional requirement**. Source: Browse and Interact With Discovery Feed; Manage Professional Network and Content Discovery; Additional 3.

Entities: `user_follows`, `post_likes`, `post_comments`, `post_saves`, `post_shares`, `saved_companies`.

Enforcement: **PK/FK, NOT NULL, CHECK + authorization**.

Composite PKs allow one like/save/follow per pair. CHECK forbids self-follow. Shares/comments are repeating events. FKs cannot determine whether content is appropriate or visible to the acting user; validate role, access and moderation status. Required comment body may still be an empty string.

## BR20 — Engagement views identify exactly one target

Basis: **Functional requirement**. Source: Company Representative: View Company Engagement; Administrator: Generate Analytics and Reports.

Entities: `view_events`, `users`, `posts`, `companies`, `job_postings`.

Enforcement: **FK, NOT NULL, CHECK + application validation**.

Exactly-one-target CHECK chooses post/company/job. viewer_user_id is required, so only authenticated views are represented. Metrics derive from events and interactions rather than independent counters.

**Team decision:** Decide whether anonymous views, repeat-view counting and retention are required.

## BR21 — Jobs describe company opportunities and company-owned locations

Basis: **Functional requirement**. Source: Recruiter: Post Job or Internship.

Entities: `job_postings`, `companies`, `company_locations`, `job_skills`, `skills`, `job_disciplines`, `academic_disciplines`, `users`.

Enforcement: **FK, NOT NULL, PK + authorization**.

A job requires company, posting actor, title, description and employment type. Optional location tuple (company_id,location_id) must match a company location. Skill/discipline junction PKs prevent duplicates; required/preferred is an enum. Authorized Recruiter/company access and publication completeness require application checks.

## BR22 — Applications use existing student and job identities

Basis: **Functional requirement**. Source: Student: Apply for Job or Internship; Additional 10.

Entities: `applications`, `student_profiles`, `job_postings`, `application_statuses`.

Enforcement: **PK/FK, NOT NULL, UNIQUE + authorization / transaction**.

An application references exactly one student profile, job and status. UNIQUE(job_id,student_user_id) allows one current application per pair; this limit is an audited assumption, not an explicit FRD restriction. Validate actor ownership, open job and deadline when submitting.

**Team decision:** Define withdrawal, repeat applications and deadline/time-zone policy.

## BR23 — Applications submit one or more owned portfolio credentials

Basis: **Functional requirement**. Source: Apply for Job or Internship; Additional 10.

Entities: `applications`, `application_portfolio_posts`, `posts`.

Enforcement: **Composite PK/FK, UNIQUE, NOT NULL, CHECK + transaction / application validation**.

Composite PK(application_id,post_id) prevents duplicate attachments. Shared student_user_id must match both applications(application_id,student_user_id) and posts(post_id,author_user_id). This prevents attaching another student’s content. UNIQUE(application_id,display_order) and positive order protect sequence. At least one link and PORTFOLIO post_type require submission logic. The post may be reused by many applications; no duplicate media/post copy is created.

## BR24 — Verification is not a prerequisite for applying

Basis: **Functional requirement**. Source: Apply for Job or Internship; Additional 6 and 10.

Entities: `posts`, `collaboration_verifications`, `application_portfolio_posts`.

Enforcement: **Requirements boundary; no such DB constraint**.

The competition deliberately attaches an approved post. FRD does not require every credential to be company-verified. An owned unverified portfolio post is eligible unless a separately approved future employer policy says otherwise. Do not invent an approved-only application FK or CHECK.

## BR25 — Additional application materials are distinct from portfolio links

Basis: **Functional requirement**. Source: Apply for Job or Internship; Review and Manage Applications.

Entities: `application_materials`, `applications`.

Enforcement: **FK, NOT NULL + application validation**.

Store additional file metadata and storage references in child records. Optional cover_note remains in applications. FRD does not define arbitrary per-job question/form schemas; required material types and actual file validation remain policy/service concerns.

**Team decision:** Define any employer-required extra materials before designing dynamic forms.

## BR26 — Current application status and history remain consistent

Basis: **Functional requirement**. Source: Manage Applications; Recruiter: Review and Manage Applications; Additional 11.

Entities: `applications`, `application_statuses`, `application_status_history`, `users`.

Enforcement: **FK, NOT NULL + authorization / transaction**.

Status lookup names are unique. Initial status-history row and later updates must commit with applications.status_id/status_updated_at atomically. FK does not make current status equal the newest history row or make history append-only. Authorized recruiter/student operations and allowed transitions need application logic.

**Team decision:** Select statuses, legal transitions and timestamp ordering conventions.

## BR27 — Private employer notes do not imply a status change

Basis: **Functional requirement**. Source: Recruiter: Review and Manage Applications.

Entities: `application_notes`, `applications`, `users`, `company_memberships`.

Enforcement: **FK, NOT NULL + authorization**.

Notes belong to one application and an existing author. Active Recruiter membership in the job company and note privacy require authorization. A status-history note belongs to an actual status event; a general note uses application_notes.

## BR28 — Invitations identify a specific student and opportunity

Basis: **Functional requirement**. Source: Recruiter: Invite Student to Apply; Additional 9.

Entities: `job_invitations`, `job_postings`, `student_profiles`, `users`, `applications`.

Enforcement: **FK, NOT NULL, UNIQUE + authorization / transaction**.

UNIQUE(job_id,student_user_id) means one current invitation per pair (audited assumption). An application may have 0..1 invitation; an invitation may yield 0..1 application. Composite FK enforces same job/student; optional invitation NULL supports direct applications. Synchronize APPLIED invitation state transactionally. Recruiter authority is not checked by the actor FK.

**Team decision:** Define invitation expiration, reinvitation and declined invitation application behavior.

## BR29 — Candidate shortlists do not require an application

Basis: **Functional requirement**. Source: Recruiter: Save or Shortlist Candidates.

Entities: `candidate_lists`, `candidate_list_items`, `student_profiles`, `users`.

Enforcement: **PK/FK, NOT NULL, UNIQUE + authorization**.

Named lists are unique per recruiter. Composite list/student PK prevents duplicate members. List ownership and Recruiter role are application checks. List membership neither creates nor requires an application.

## BR30 — Messaging connects permitted participants

Basis: **Functional requirement**. Source: All Roles: Messaging; Recruiter/Student/Advisor communication requirements.

Entities: `conversations`, `conversation_participants`, `messages`, `users`.

Enforcement: **PK/FK, NOT NULL + authorization / transaction**.

Composite message FK(conversation_id,sender_user_id) requires participant membership. Participant PK prevents duplicates. Logical minimum is 1..* participants; direct conversations require at least two distinct permitted users by workflow. No DB CHECK counts children or limits groups. Access on sending/reading/removal remains authorization.

**Team decision:** Decide whether group chat is supported and whether participant removal must retain message references.

## BR31 — Application conversations cannot name a different job

Basis: **Functional requirement**. Source: Manage Applications; Communicate With Candidates.

Entities: `conversations`, `applications`, `job_postings`.

Enforcement: **FK, CHECK**.

An application-linked conversation requires job_id by CHECK and a matching (application_id,job_id) tuple by composite FK. Job-only and general conversations remain valid. No unique constraint limits conversation count per job/application.

## BR32 — Notifications have a recipient and typed navigation context

Basis: **Functional requirement**. Source: All Roles: Notifications.

Entities: `notifications`, `users`, `posts`, `post_comments`, `companies`, `job_postings`, `applications`, `job_invitations`, `collaboration_verifications`, `conversations`, `reports`, `company_memberships`, `advisor_student_assignments`.

Enforcement: **FK, NOT NULL, CHECK + application / transaction**.

Target-count CHECK permits zero or one target; FKs validate supplied targets. Zero is valid for system announcements. CHECK requires is_read/read_at consistency. Required target by notification type, recipient choice and reliable event/notification coordination are service logic, not CHECKs.

**Team decision:** Define delivery fan-out, preference and reliable delivery/retry policy.

## BR33 — Advisor access requires authorization or assignment

Basis: **Functional requirement**. Source: Career Advisor: Review Student Career Profile; Monitor Student Career Activity.

Entities: `advisor_student_assignments`, `users`, `student_profiles`.

Enforcement: **FK, NOT NULL, UNIQUE + authorization**.

Assignment source records administrator assignment or student authorization. One current advisor/student pair is unique. Advisor role, ACTIVE assignment and visibility of private career information require authorization at read/write time. Lifecycle times are not tied to status by CHECK.

**Team decision:** Define access revocation, assignment renewal and history policy.

## BR34 — Private feedback targets the assigned student’s portfolio

Basis: **Functional requirement**. Source: Career Advisor: Provide Portfolio Feedback; Recommend Portfolio Content.

Entities: `portfolio_feedback`, `advisor_student_assignments`, `posts`.

Enforcement: **Composite FK, NOT NULL + authorization**.

Assignment/student tuple must match. If post_id is supplied, post/author tuple must match the same student. Null post is permitted for whole-portfolio feedback or missing-content suggestions. Privacy, ACTIVE assignment and portfolio post type are application checks.

## BR35 — Advisors recommend opportunities to assigned students

Basis: **Functional requirement**. Source: Career Advisor: Recommend Opportunities.

Entities: `opportunity_recommendations`, `advisor_student_assignments`, `job_postings`.

Enforcement: **FK, NOT NULL, UNIQUE + authorization**.

One current assignment/job recommendation is unique, an audited design limit. Assignment supplies the student; do not duplicate student identity in this relation. Active authorized advisor access and recommending eligible opportunities require application validation.

**Team decision:** Define repeated recommendations after dismissal or changed job content.

## BR36 — Assistance and analytics are derived from source records

Basis: **Functional requirement**. Source: Career Advisor: Identify Students Needing Assistance; Administrator: Generate Analytics and Reports.

Entities: `student_profiles`, `posts`, `applications`, `job_invitations`, `view_events`, `advisor_student_assignments`.

Enforcement: **Authorized queries**.

Completeness, activity counts and analytics are computed over existing entities. Advisor queries must stay within permitted assignments; global analytics need administrator authorization. There are no independent dashboard/report-result tables.

**Team decision:** Define completeness thresholds and engagement aggregation rules.

## BR37 — Reports target exactly one existing object

Basis: **Functional requirement**. Source: Administrator: Manage Reported Activity; Company Representative: Respond to Company-Related Reports.

Entities: `reports`, `users`, `posts`, `post_comments`, `messages`, `job_postings`, `companies`.

Enforcement: **FK, NOT NULL, CHECK + authorization**.

Exactly-one-target CHECK requires one typed report target. Reporter and each supplied target must exist. Confidential report access and company response scope require authorization. Reports target FKs should use RESTRICT in physical MySQL DDL, as recorded in DBML comments.

**Team decision:** Choose retention, resolution transitions and response workflow.

## BR38 — Moderation actions preserve accountable decisions

Basis: **Functional requirement**. Source: Administrator: Moderate Posts and Social Content; Manage Employment Content; Manage Reported Activity.

Entities: `moderation_actions`, `reports`, `users`, `posts`, `post_comments`, `job_postings`, `companies`.

Enforcement: **FK, NOT NULL + authorization / transaction**.

Each action references a report and an existing administrator user; the user FK does not verify Administrator role. Apply effective target status and document action atomically. Required action_type is an enum; context/action compatibility is application logic.

**Team decision:** Define legal sanctions, unmoderation and retention/purge policy.

## BR39 — Category merges preserve references and avoid cycles

Basis: **Functional requirement**. Source: Administrator: Manage Skills and Academic Categories.

Entities: `skills`, `majors`, `academic_disciplines`, `industries`, `student_skills`, `post_skills`, `job_skills`, `job_disciplines`.

Enforcement: **Self-FK, CHECK, UNIQUE + transaction / authorization**.

Merge destination must exist, differ from source, and source must be inactive by CHECK. Cycles, active destination, reassignment and duplicate-junction reconciliation require transactional validation. Major names are unique within discipline; other named categories are individually unique.

**Team decision:** Define cross-discipline major merges and merge/restore policy.

## BR40 — System-wide configurable policies have attributable changes

Basis: **Functional requirement**. Source: Administrator: Manage System Configuration.

Entities: `system_settings`, `application_statuses`, `users`.

Enforcement: **PK/FK, NOT NULL, UNIQUE + authorization / application validation**.

Setting key is the PK; updater is optional. Setting values are text with no per-key type/range CHECK. Application statuses are configurable records; enum domains in DBML remain declared finite vocabularies, not automatically editable settings.

**Team decision:** Define allowed setting keys, value validation and which enum domains need future configuration support.

## BR41 — Referenced evidence is not silently erased

Basis: **Proposed policy / unresolved**. Source: Schema audit: Enforcement boundary; no explicit FRD retention duration.

Entities: `applications`, `application_portfolio_posts`, `posts`, `reports`, `moderation_actions`, `messages`.

Enforcement: **Physical FK delete policy / application policy**.

DBML defines no universal ON DELETE/UPDATE actions. Prefer existing inactive/removed states for lifecycle. Retention durations, evidence snapshots and purge rules require team approval; no snapshots table has been added. Composite ownership columns are controlled redundancy protected by FKs.

**Team decision:** Decide immutable application evidence, claim edits after application, physical delete actions and retention.

## Complete declared constraint inventory

The data dictionary carries every field-level NOT NULL, default, auto-increment and uniqueness designation. The following catalog lists every table’s PK, UQ, CHECK and exact FK tuples. The relationship register supplies all 109 endpoint cardinalities. Logical 1..* minima in the register require workflow validation (BR07, BR23, BR30).

### roles

- PK: `(role_id)`.
- UNIQUE: `(role_name)`.

### permissions

- PK: `(permission_id)`.
- UNIQUE: `(permission_code)`.

### role_permissions

- PK: `(role_id, permission_id)`.
- R001: FK `(role_id)` → `roles(role_id)`; parent/child counts **1 / 0..***.
- R002: FK `(permission_id)` → `permissions(permission_id)`; parent/child counts **1 / 0..***.

### users

- PK: `(user_id)`.
- UNIQUE: `(email)`.
- R003: FK `(role_id)` → `roles(role_id)`; parent/child counts **1 / 0..***.
- R004: FK `(school_id)` → `schools(school_id)`; parent/child counts **0..1 / 0..***.

### student_profiles

- PK: `(user_id)`.
- R005: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..1**.
- R006: FK `(major_id)` → `majors(major_id)`; parent/child counts **1 / 0..***.

### schools

- PK: `(school_id)`.
- UNIQUE: `(name)`.

### academic_disciplines

- PK: `(discipline_id)`.
- UNIQUE: `(name)`.
- CHECK: `merged_into_discipline_id IS NULL OR (merged_into_discipline_id <> discipline_id AND is_active = false)`.
- R007: FK `(merged_into_discipline_id)` → `academic_disciplines(discipline_id)`; parent/child counts **0..1 / 0..***.

### majors

- PK: `(major_id)`.
- UNIQUE: `(discipline_id, name)`.
- CHECK: `merged_into_major_id IS NULL OR (merged_into_major_id <> major_id AND is_active = false)`.
- R008: FK `(discipline_id)` → `academic_disciplines(discipline_id)`; parent/child counts **1 / 0..***.
- R009: FK `(merged_into_major_id)` → `majors(major_id)`; parent/child counts **0..1 / 0..***.

### industries

- PK: `(industry_id)`.
- UNIQUE: `(name)`.
- CHECK: `merged_into_industry_id IS NULL OR (merged_into_industry_id <> industry_id AND is_active = false)`.
- R010: FK `(merged_into_industry_id)` → `industries(industry_id)`; parent/child counts **0..1 / 0..***.

### skills

- PK: `(skill_id)`.
- UNIQUE: `(name)`.
- CHECK: `merged_into_skill_id IS NULL OR (merged_into_skill_id <> skill_id AND is_active = false)`.
- R011: FK `(merged_into_skill_id)` → `skills(skill_id)`; parent/child counts **0..1 / 0..***.

### student_skills

- PK: `(student_user_id, skill_id)`.
- R012: FK `(student_user_id)` → `student_profiles(user_id)`; parent/child counts **1 / 0..***.
- R013: FK `(skill_id)` → `skills(skill_id)`; parent/child counts **1 / 0..***.

### companies

- PK: `(company_id)`.
- R014: FK `(industry_id)` → `industries(industry_id)`; parent/child counts **1 / 0..***.
- R015: FK `(approved_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.

### company_locations

- PK: `(location_id)`.
- UNIQUE: `(company_id, location_id)`.
- R016: FK `(company_id)` → `companies(company_id)`; parent/child counts **1 / 0..***.

### company_memberships

- PK: `(membership_id)`.
- UNIQUE: `(company_id, user_id)`.
- R017: FK `(company_id)` → `companies(company_id)`; parent/child counts **1 / 0..***.
- R018: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R019: FK `(reviewed_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.

### posts

- PK: `(post_id)`.
- UNIQUE: `(post_id, author_user_id)`.
- UNIQUE: `(post_id, company_id)`.
- CHECK: `major_id IS NULL OR discipline_id IS NULL`.
- CHECK: `post_type <> 'COMPANY' OR company_id IS NOT NULL`.
- R020: FK `(author_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R021: FK `(company_id)` → `companies(company_id)`; parent/child counts **0..1 / 0..***.
- R022: FK `(discipline_id)` → `academic_disciplines(discipline_id)`; parent/child counts **0..1 / 0..***.
- R023: FK `(major_id)` → `majors(major_id)`; parent/child counts **0..1 / 0..***.

### post_media

- PK: `(media_id)`.
- UNIQUE: `(post_id, display_order)`.
- CHECK: `display_order >= 1`.
- CHECK: `duration_seconds IS NULL OR duration_seconds > 0`.
- CHECK: `file_size_bytes IS NULL OR file_size_bytes > 0`.
- R024: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 1..***.

### post_skills

- PK: `(post_id, skill_id)`.
- R025: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 0..***.
- R026: FK `(skill_id)` → `skills(skill_id)`; parent/child counts **1 / 0..***.

### collaboration_verifications

- PK: `(verification_id)`.
- UNIQUE: `(post_id)`.
- CHECK: `showcase_status <> 'SHOWCASED' OR status = 'APPROVED'`.
- CHECK: `status = 'PENDING' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL)`.
- R027: FK `(company_id)` → `companies(company_id)`; parent/child counts **1 / 0..***.
- R028: FK `(reviewed_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.
- R029: FK `(showcased_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.
- R102: FK `(post_id, company_id)` → `posts(post_id, company_id)`; parent/child counts **1 / 0..1**.

### user_follows

- PK: `(follower_user_id, followed_user_id)`.
- CHECK: `follower_user_id <> followed_user_id`.
- R030: FK `(follower_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R031: FK `(followed_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### post_likes

- PK: `(post_id, user_id)`.
- R032: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 0..***.
- R033: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### post_comments

- PK: `(comment_id)`.
- R034: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 0..***.
- R035: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### post_saves

- PK: `(post_id, user_id)`.
- R036: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 0..***.
- R037: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### post_shares

- PK: `(share_id)`.
- R038: FK `(post_id)` → `posts(post_id)`; parent/child counts **1 / 0..***.
- R039: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### saved_companies

- PK: `(user_id, company_id)`.
- R040: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R041: FK `(company_id)` → `companies(company_id)`; parent/child counts **1 / 0..***.

### view_events

- PK: `(view_id)`.
- CHECK: `(post_id IS NOT NULL) + (company_id IS NOT NULL) + (job_id IS NOT NULL) = 1`.
- R042: FK `(viewer_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R043: FK `(post_id)` → `posts(post_id)`; parent/child counts **0..1 / 0..***.
- R044: FK `(company_id)` → `companies(company_id)`; parent/child counts **0..1 / 0..***.
- R045: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **0..1 / 0..***.

### job_postings

- PK: `(job_id)`.
- R046: FK `(company_id)` → `companies(company_id)`; parent/child counts **1 / 0..***.
- R047: FK `(posted_by_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R101: FK `(company_id, location_id)` → `company_locations(company_id, location_id)`; parent/child counts **0..1 / 0..***.

### job_skills

- PK: `(job_id, skill_id)`.
- R048: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **1 / 0..***.
- R049: FK `(skill_id)` → `skills(skill_id)`; parent/child counts **1 / 0..***.

### job_disciplines

- PK: `(job_id, discipline_id)`.
- R050: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **1 / 0..***.
- R051: FK `(discipline_id)` → `academic_disciplines(discipline_id)`; parent/child counts **1 / 0..***.

### application_statuses

- PK: `(status_id)`.
- UNIQUE: `(name)`.

### applications

- PK: `(application_id)`.
- UNIQUE: `(job_id, student_user_id)`.
- UNIQUE: `(application_id, student_user_id)`.
- UNIQUE: `(application_id, job_id)`.
- UNIQUE: `(invitation_id)`.
- R052: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **1 / 0..***.
- R053: FK `(student_user_id)` → `student_profiles(user_id)`; parent/child counts **1 / 0..***.
- R054: FK `(status_id)` → `application_statuses(status_id)`; parent/child counts **1 / 0..***.
- R105: FK `(invitation_id, job_id, student_user_id)` → `job_invitations(invitation_id, job_id, student_user_id)`; parent/child counts **0..1 / 0..1**.

### application_portfolio_posts

- PK: `(application_id, post_id)`.
- UNIQUE: `(application_id, display_order)`.
- CHECK: `display_order >= 1`.
- R103: FK `(application_id, student_user_id)` → `applications(application_id, student_user_id)`; parent/child counts **1 / 1..***.
- R104: FK `(post_id, student_user_id)` → `posts(post_id, author_user_id)`; parent/child counts **1 / 0..***.

### application_status_history

- PK: `(history_id)`.
- R055: FK `(application_id)` → `applications(application_id)`; parent/child counts **1 / 0..***.
- R056: FK `(status_id)` → `application_statuses(status_id)`; parent/child counts **1 / 0..***.
- R057: FK `(changed_by_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### job_invitations

- PK: `(invitation_id)`.
- UNIQUE: `(job_id, student_user_id)`.
- UNIQUE: `(invitation_id, job_id, student_user_id)`.
- R058: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **1 / 0..***.
- R059: FK `(recruiter_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R060: FK `(student_user_id)` → `student_profiles(user_id)`; parent/child counts **1 / 0..***.

### candidate_lists

- PK: `(list_id)`.
- UNIQUE: `(recruiter_user_id, name)`.
- R061: FK `(recruiter_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### candidate_list_items

- PK: `(list_id, student_user_id)`.
- R062: FK `(list_id)` → `candidate_lists(list_id)`; parent/child counts **1 / 0..***.
- R063: FK `(student_user_id)` → `student_profiles(user_id)`; parent/child counts **1 / 0..***.

### conversations

- PK: `(conversation_id)`.
- CHECK: `application_id IS NULL OR job_id IS NOT NULL`.
- R064: FK `(created_by_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R108: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **0..1 / 0..***.
- R109: FK `(application_id, job_id)` → `applications(application_id, job_id)`; parent/child counts **0..1 / 0..***.

### conversation_participants

- PK: `(conversation_id, user_id)`.
- R065: FK `(conversation_id)` → `conversations(conversation_id)`; parent/child counts **1 / 1..***.
- R066: FK `(user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### messages

- PK: `(message_id)`.
- R100: FK `(conversation_id, sender_user_id)` → `conversation_participants(conversation_id, user_id)`; parent/child counts **1 / 0..***.

### notifications

- PK: `(notification_id)`.
- CHECK: `(target_post_id IS NOT NULL) + (target_comment_id IS NOT NULL) + (target_user_id IS NOT NULL) + (target_company_id IS NOT NULL) + (target_job_id IS NOT NULL) + (target_application_id IS NOT NULL) + (target_invitation_id IS NOT NULL) + (target_verification_id IS NOT NULL) + (target_conversation_id IS NOT NULL) + (target_report_id IS NOT NULL) + (target_membership_id IS NOT NULL) + (target_assignment_id IS NOT NULL) <= 1`.
- CHECK: `(is_read = false AND read_at IS NULL) OR (is_read = true AND read_at IS NOT NULL)`.
- R067: FK `(recipient_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R068: FK `(target_post_id)` → `posts(post_id)`; parent/child counts **0..1 / 0..***.
- R069: FK `(target_comment_id)` → `post_comments(comment_id)`; parent/child counts **0..1 / 0..***.
- R070: FK `(target_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.
- R071: FK `(target_company_id)` → `companies(company_id)`; parent/child counts **0..1 / 0..***.
- R072: FK `(target_job_id)` → `job_postings(job_id)`; parent/child counts **0..1 / 0..***.
- R073: FK `(target_application_id)` → `applications(application_id)`; parent/child counts **0..1 / 0..***.
- R074: FK `(target_invitation_id)` → `job_invitations(invitation_id)`; parent/child counts **0..1 / 0..***.
- R075: FK `(target_verification_id)` → `collaboration_verifications(verification_id)`; parent/child counts **0..1 / 0..***.
- R076: FK `(target_conversation_id)` → `conversations(conversation_id)`; parent/child counts **0..1 / 0..***.
- R077: FK `(target_report_id)` → `reports(report_id)`; parent/child counts **0..1 / 0..***.
- R078: FK `(target_membership_id)` → `company_memberships(membership_id)`; parent/child counts **0..1 / 0..***.
- R079: FK `(target_assignment_id)` → `advisor_student_assignments(assignment_id)`; parent/child counts **0..1 / 0..***.

### advisor_student_assignments

- PK: `(assignment_id)`.
- UNIQUE: `(advisor_user_id, student_user_id)`.
- UNIQUE: `(assignment_id, student_user_id)`.
- R080: FK `(advisor_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R081: FK `(student_user_id)` → `student_profiles(user_id)`; parent/child counts **1 / 0..***.
- R082: FK `(assigned_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.

### portfolio_feedback

- PK: `(feedback_id)`.
- R106: FK `(assignment_id, student_user_id)` → `advisor_student_assignments(assignment_id, student_user_id)`; parent/child counts **1 / 0..***.
- R107: FK `(post_id, student_user_id)` → `posts(post_id, author_user_id)`; parent/child counts **0..1 / 0..***.

### opportunity_recommendations

- PK: `(recommendation_id)`.
- UNIQUE: `(assignment_id, job_id)`.
- R083: FK `(assignment_id)` → `advisor_student_assignments(assignment_id)`; parent/child counts **1 / 0..***.
- R084: FK `(job_id)` → `job_postings(job_id)`; parent/child counts **1 / 0..***.

### reports

- PK: `(report_id)`.
- CHECK: `(target_post_id IS NOT NULL) + (target_comment_id IS NOT NULL) + (target_user_id IS NOT NULL) + (target_message_id IS NOT NULL) + (target_job_id IS NOT NULL) + (target_company_id IS NOT NULL) = 1`.
- R085: FK `(reporter_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R086: FK `(target_post_id)` → `posts(post_id)`; parent/child counts **0..1 / 0..***.
- R087: FK `(target_comment_id)` → `post_comments(comment_id)`; parent/child counts **0..1 / 0..***.
- R088: FK `(target_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.
- R089: FK `(target_message_id)` → `messages(message_id)`; parent/child counts **0..1 / 0..***.
- R090: FK `(target_job_id)` → `job_postings(job_id)`; parent/child counts **0..1 / 0..***.
- R091: FK `(target_company_id)` → `companies(company_id)`; parent/child counts **0..1 / 0..***.

### moderation_actions

- PK: `(action_id)`.
- R092: FK `(report_id)` → `reports(report_id)`; parent/child counts **1 / 0..***.
- R093: FK `(admin_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### system_settings

- PK: `(setting_key)`.
- R094: FK `(updated_by_user_id)` → `users(user_id)`; parent/child counts **0..1 / 0..***.

### application_materials

- PK: `(material_id)`.
- R095: FK `(application_id)` → `applications(application_id)`; parent/child counts **1 / 0..***.

### application_notes

- PK: `(note_id)`.
- R096: FK `(application_id)` → `applications(application_id)`; parent/child counts **1 / 0..***.
- R097: FK `(recruiter_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

### account_reviews

- PK: `(review_id)`.
- CHECK: `decision <> 'PENDING'`.
- CHECK: `subject_user_id <> reviewed_by_user_id`.
- R098: FK `(subject_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.
- R099: FK `(reviewed_by_user_id)` → `users(user_id)`; parent/child counts **1 / 0..***.

## Explicit audited assumptions

These retained design choices are not additional promises in the Functional Requirements. They are preserved for this deliverable and require team decisions if changed.

- One sign-in role per account: required users.role_id, not a user-role junction (BR01).
- Major and graduation year are mandatory on a student profile; school remains optional (BR03).
- One tagged company and one current verification record per post; verification event history is not modeled (BR14–BR15).
- One stored application and invitation per student/job pair, across all lifecycle states. Rejection, withdrawal or expiry does not release a UNIQUE tuple; reuse the record unless an approved redesign adds episodes (BR22, BR28).
- One assignment per advisor/student pair and one recommendation per assignment/job pair, across all lifecycle states (BR33, BR35).
- Direct messaging needs two distinct permitted participants; the 1..* logical symbol alone cannot express that count (BR30).
- Claim-changing edits require re-verification under the audit policy; edit scope and evidence retention require agreement (BR17, BR41).
- Views require an authenticated user; anonymous analytics is not represented by the current mandatory viewer FK (BR20).
- Additional application materials use file references; arbitrary employer question/form structures are not specified (BR25).

## Team decision register

- **BR01**: Confirm whether one account may hold multiple roles.
- **BR02**: Choose email normalization/collation and session policy.
- **BR03**: Confirm whether incomplete registration needs a draft profile.
- **BR04**: Define which roles require approval and allowed state transitions.
- **BR05**: Define approval evidence, lifecycle transitions and whether a single headquarters is required.
- **BR06**: Define who approves/revokes memberships and whether membership history is needed.
- **BR11**: Resolve Sprint I up-to-60 wording versus FRD 30–60 and configure authoritative limits.
- **BR15**: Confirm whether rejected requests can be resubmitted in place and whether decision history is required.
- **BR17**: Define which edits require re-verification and how earlier application evidence is retained.
- **BR18**: Confirm automatic company display versus representative selection; distinguish verified-work listing from selected showcase.
- **BR20**: Decide whether anonymous views, repeat-view counting and retention are required.
- **BR22**: Define withdrawal, repeat applications and deadline/time-zone policy.
- **BR25**: Define any employer-required extra materials before designing dynamic forms.
- **BR26**: Select statuses, legal transitions and timestamp ordering conventions.
- **BR28**: Define invitation expiration, reinvitation and declined invitation application behavior.
- **BR30**: Decide whether group chat is supported and whether participant removal must retain message references.
- **BR32**: Define delivery fan-out, preference and reliable delivery/retry policy.
- **BR33**: Define access revocation, assignment renewal and history policy.
- **BR35**: Define repeated recommendations after dismissal or changed job content.
- **BR36**: Define completeness thresholds and engagement aggregation rules.
- **BR37**: Choose retention, resolution transitions and response workflow.
- **BR38**: Define legal sanctions, unmoderation and retention/purge policy.
- **BR39**: Define cross-discipline major merges and merge/restore policy.
- **BR40**: Define allowed setting keys, value validation and which enum domains need future configuration support.
- **BR41**: Decide immutable application evidence, claim edits after application, physical delete actions and retention.

## Requirement coverage

The existing schema-audit requirement coverage maps every named role requirement to persistent support. BR01–BR04 cover shared account/profile functions; BR07–BR20 cover portfolio, discovery, collaboration and social interaction; BR21–BR29 cover recruiting; BR30–BR32 cover communication; BR33–BR36 cover advising; BR37–BR40 cover administration. UI requirements specify interfaces over these same records rather than new entities. BR41 is explicitly unresolved retention policy.
