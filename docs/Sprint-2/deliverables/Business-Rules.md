# CareerUp — Enhanced database business rules

Stored constraints and application checks are distinguished below. Open decisions remain marked for team review.

## BR01 — Accounts select a supported role

**Basis:** Functional requirement

**Source:** All Roles: Account Registration; User Roles

**Tables:** users, roles, permissions, role_permissions

**Enforcement:** FK, NOT NULL, PK, UNIQUE + authorization

**Detail:** users.role_id requires one existing role. role_permissions has a composite PK preventing duplicate capability grants. Permission checks, administrator provisioning and role eligibility are application authorization. One role per account is an audited design choice; the FRD does not explicitly prohibit multiple roles.

**Decision:** Confirm whether one account may hold multiple roles.

## BR02 — Secure sign-in uses account credentials and effective access state

**Basis:** Functional requirement

**Source:** All Roles: Sign-in/Sign-out

**Tables:** users

**Enforcement:** NOT NULL, UNIQUE + authorization

**Detail:** Email and password_hash are required; email is unique under the eventual collation. Hashing, credential verification, session termination, account-state restrictions and case-normalized email treatment are application concerns. No sessions table or automatic timestamp default is declared.

**Decision:** Choose email normalization/collation and session policy.

## BR03 — Role-appropriate profiles share account identity

**Basis:** Functional requirement

**Source:** All Roles: Profile Management; Student: Manage Student Profile and Portfolio

**Tables:** users, student_profiles, schools, majors

**Enforcement:** PK/FK, NOT NULL + authorization / transaction

**Detail:** Every student profile references exactly one user via its PK/FK; all-role users have 0..1 student profile. Student-role registration requires a profile transactionally. Major and graduation year are NOT NULL, while school is optional. FRD lists profile information without mandating registration completeness.

**Decision:** Confirm whether incomplete registration needs a draft profile.

## BR04 — Account approval decisions are attributable

**Basis:** Functional requirement

**Source:** All Roles: Account Registration; Administrator: Manage User Accounts

**Tables:** account_reviews, users

**Enforcement:** PK/FK, NOT NULL, CHECK + authorization / transaction

**Detail:** account_reviews excludes PENDING and self-review using CHECKs; subject/reviewer must exist. Reviewer must be an Administrator. Update effective users.status with the decision atomically. Whether every employer/advisor account needs approval is not fixed by the FRD.

**Decision:** Define which roles require approval and allowed state transitions.

## BR05 — Companies have official information and administrative approval

**Basis:** Functional requirement

**Source:** Company Representative: Manage Company Profile; Administrator: Manage Company Accounts

**Tables:** companies, industries, company_locations, users

**Enforcement:** PK/FK, NOT NULL + authorization / transaction

**Detail:** Company industry is required; locations are repeating children. Approver/time are nullable, and there is no state/time CHECK. Only authorized actors may maintain profiles or approve companies. Company names need not be globally unique.

**Decision:** Define approval evidence, lifecycle transitions and whether a single headquarters is required.

## BR06 — Employer access depends on approved company affiliation

**Basis:** Functional requirement

**Source:** Company Representative: Manage Company Recruiting Team; All Roles: Account Registration

**Tables:** company_memberships, companies, users, roles

**Enforcement:** FK, NOT NULL, UNIQUE + authorization

**Detail:** UNIQUE(company_id,user_id) permits one current membership per pair. FK proves affiliation records exist; it does not prove Recruiter/Company Representative role, ACTIVE membership, ACTIVE account or approval authority. These checks must be performed at the action.

**Decision:** Define who approves/revokes memberships and whether membership history is needed.

## BR07 — Students publish portfolio media posts

**Basis:** Functional requirement

**Source:** Student: Create Portfolio Post; Additional 1, 12

**Tables:** posts, users, student_profiles, post_media

**Enforcement:** FK, NOT NULL, CHECK + authorization / transaction

**Detail:** Author, title and post_type are required. Portfolio authors must be Students by authorization, not the users FK. At publication require 1..* media children and commit content/media/tags atomically; ordinary FKs permit a post with no media. Description is nullable because the FRD says may include. Title NOT NULL does not prohibit an empty string.

**Decision:** 

## BR08 — Company content belongs to an authorized publishing company

**Basis:** Functional requirement

**Source:** Company Representative: Publish Company Content

**Tables:** posts, companies, company_memberships, users

**Enforcement:** FK, CHECK + authorization

**Detail:** CHECK requires company_id for COMPANY posts. An ACTIVE same-company Company Representative must publish it; the FK does not check role or membership.

**Decision:** 

## BR09 — Portfolio and discovery views reuse the same content

**Basis:** Functional requirement

**Source:** Student: Manage Student Profile and Portfolio; Create Portfolio Post; Browse and Interact With Discovery Feed

**Tables:** posts

**Enforcement:** NOT NULL/defaults + query / authorization

**Detail:** portfolio_visible and feed_visible are independent booleans. Both default true. Feed and portfolio are views over the same record, with status/privacy eligibility validated by the service; there are no duplicate feed or portfolio tables.

**Decision:** 

## BR10 — Post academic classification avoids conflicting categories

**Basis:** Functional requirement

**Source:** Create Portfolio Post; Additional 2, 4

**Tables:** posts, majors, academic_disciplines

**Enforcement:** FK, CHECK

**Detail:** Both classification columns are optional. CHECK prevents major_id and discipline_id being non-null together. Derive the broad discipline through majors when major_id is supplied. Required student major does not require every post to copy it.

**Decision:** 

## BR11 — Media files have ordered metadata

**Basis:** Functional requirement

**Source:** Create Portfolio Post; Additional 1

**Tables:** post_media, posts

**Enforcement:** FK, NOT NULL, UNIQUE, CHECK + publication validation

**Detail:** Each media belongs to one post. UNIQUE(post_id,display_order) and display_order>=1 prevent duplicate/nonpositive positions. Duration and file size, if supplied, must be positive. Actual file existence, allowed types and video duration require storage/application validation. FRD describes 30â€“60-second video; no range CHECK exists.

**Decision:** Resolve Sprint I up-to-60 wording versus FRD 30â€“60 and configure authoritative limits.

## BR12 — Standardized skills support multiple tags without duplicates

**Basis:** Functional requirement

**Source:** Student: Manage Skill Tags; Additional 5

**Tables:** skills, student_skills, post_skills, student_profiles, posts

**Enforcement:** PK/FK, NOT NULL, UNIQUE + application validation

**Detail:** Composite PKs prevent repeated student/skill and post/skill pairs, not multiple skills. A skill may tag many students/posts; either parent may have no tag links. Active/approved selection is application validation; an FK allows an existing inactive skill.

**Decision:** 

## BR13 — Discovery is independent of applications

**Basis:** Functional requirement

**Source:** Recruiter: Search and Discover Students; Review Student Portfolio; Additional 4, 8

**Tables:** users, student_profiles, majors, academic_disciplines, student_skills, posts, post_skills, collaboration_verifications

**Enforcement:** FK relationships + authorized queries

**Detail:** Recruiter filters join profile, skills, categories and verification without requiring an application. Availability/privacy checks are authorization. No separate search/discovery entity is required.

**Decision:** 

## BR14 — Company tagging is optional and does not prove collaboration

**Basis:** Functional requirement

**Source:** Student: Participate in Verified Collaboration; Additional 6

**Tables:** posts, companies, collaboration_verifications

**Enforcement:** Nullable FK + authorization

**Detail:** A PORTFOLIO post may reference 0..1 tagged company. Creating that tag alone must not display Verified Collaboration. Verification requests must concern student-owned portfolio content.

**Decision:** 

## BR15 — A current verification matches the post and tagged company

**Basis:** Functional requirement

**Source:** Participate in Verified Collaboration; Company Representative: Verify Student Collaboration

**Tables:** collaboration_verifications, posts, companies

**Enforcement:** FK, NOT NULL, UNIQUE

**Detail:** Required composite FK (post_id,company_id) references the matching posts tuple. UNIQUE(post_id) makes each post have 0..1 current request. Each request has exactly one post and one company. One current request per post, rather than request-event history, is an audited assumption. Version B retains the current row and adds append-only collaboration_verification_history; it does not create duplicate current requests.

**Decision:** Confirm whether rejected requests can be resubmitted in place and whether decision history is required.

## BR16 — Only an authorized representative approves a collaboration

**Basis:** Functional requirement

**Source:** Additional 6; Company Representative: Verify Student Collaboration

**Tables:** collaboration_verifications, users, roles, company_memberships

**Enforcement:** FK, CHECK + authorization / transaction

**Detail:** Reviewer FK only proves a user exists. Check Company Representative role, active account and ACTIVE membership in the request company at decision time. CHECK requires reviewer/time for APPROVED or REJECTED; PENDING can technically retain reviewer/time. Legal transitions and request notification are application/transaction logic. Version B history references the seeded verification_transitions pairs. The application must still atomically check the old current status, insert history and update the request.

**Decision:** 

## BR17 — Approved work receives a derived verified indicator

**Basis:** Functional requirement

**Source:** Participate in Verified Collaboration; Additional 6, 7

**Tables:** posts, collaboration_verifications

**Enforcement:** Query / authorization

**Detail:** Derive the badge from the approved current request; do not duplicate a verified flag in posts. Same content can be shown on student/company pages. Claim-changing edits must invalidate/reset approval according to the audit policy, enforced transactionally.

**Decision:** Define which edits require re-verification and how earlier application evidence is retained.

## BR18 — Only approved content can be showcased

**Basis:** Functional requirement

**Source:** Company Representative: Manage Collaborative Showcases; Additional 7

**Tables:** collaboration_verifications, companies, posts, company_memberships

**Enforcement:** CHECK + authorization / application validation

**Detail:** CHECK prevents SHOWCASED unless APPROVED. Representative selection, same-company access, showcase actor/time and valid ordering are application checks; showcase_order has no positive/unique constraint. FRD overview suggests appearing on both pages after approval, while the detailed showcase requirement permits selection.

**Decision:** Confirm automatic company display versus representative selection; distinguish verified-work listing from selected showcase.

## BR19 — Social interactions have attributable users and valid targets

**Basis:** Functional requirement

**Source:** Browse and Interact With Discovery Feed; Manage Professional Network and Content Discovery; Additional 3

**Tables:** user_follows, post_likes, post_comments, post_saves, post_shares, saved_companies

**Enforcement:** PK/FK, NOT NULL, CHECK + authorization

**Detail:** Composite PKs allow one like/save/follow per pair. CHECK forbids self-follow. Shares/comments are repeating events. FKs cannot determine whether content is appropriate or visible to the acting user; validate role, access and moderation status. Required comment body may still be an empty string.

**Decision:** 

## BR20 — Engagement views identify exactly one target

**Basis:** Functional requirement

**Source:** Company Representative: View Company Engagement; Administrator: Generate Analytics and Reports

**Tables:** view_events, users, posts, companies, job_postings

**Enforcement:** FK, NOT NULL, CHECK + application validation

**Detail:** Exactly-one-target CHECK chooses post/company/job. viewer_user_id is required, so only authenticated views are represented. Metrics derive from events and interactions rather than independent counters.

**Decision:** Decide whether anonymous views, repeat-view counting and retention are required.

## BR21 — Jobs describe company opportunities and company-owned locations

**Basis:** Functional requirement

**Source:** Recruiter: Post Job or Internship

**Tables:** job_postings, companies, company_locations, job_skills, skills, job_disciplines, academic_disciplines, users

**Enforcement:** FK, NOT NULL, PK + authorization

**Detail:** A job requires company, posting actor, title, description and employment type. Optional location tuple (company_id,location_id) must match a company location. Skill/discipline junction PKs prevent duplicates; required/preferred is an enum. Authorized Recruiter/company access and publication completeness require application checks.

**Decision:** 

## BR22 — Applications use existing student and job identities

**Basis:** Functional requirement

**Source:** Student: Apply for Job or Internship; Additional 10

**Tables:** applications, student_profiles, job_postings, application_statuses

**Enforcement:** PK/FK, NOT NULL, UNIQUE + authorization / transaction

**Detail:** An application references exactly one student profile, job and status. UNIQUE(job_id,student_user_id) allows one current application per pair; this limit is an audited assumption, not an explicit FRD restriction. Validate actor ownership, open job and deadline when submitting.

**Decision:** Define withdrawal, repeat applications and deadline/time-zone policy.

## BR23 — Applications submit one or more owned portfolio credentials

**Basis:** Functional requirement

**Source:** Apply for Job or Internship; Additional 10

**Tables:** applications, application_portfolio_posts, posts

**Enforcement:** Composite PK/FK, UNIQUE, NOT NULL, CHECK + transaction / application validation

**Detail:** Composite PK(application_id,post_id) prevents duplicate attachments. Shared student_user_id must match both applications(application_id,student_user_id) and posts(post_id,author_user_id). This prevents attaching another studentâ€™s content. UNIQUE(application_id,display_order) and positive order protect sequence. At least one link and PORTFOLIO post_type require submission logic. The post may be reused by many applications; no duplicate media/post copy is created.

**Decision:** 

## BR24 — Verification is not a prerequisite for applying

**Basis:** Functional requirement

**Source:** Apply for Job or Internship; Additional 6 and 10

**Tables:** posts, collaboration_verifications, application_portfolio_posts

**Enforcement:** Requirements boundary; no such DB constraint

**Detail:** The competition deliberately attaches an approved post. FRD does not require every credential to be company-verified. An owned unverified portfolio post is eligible unless a separately approved future employer policy says otherwise. Do not invent an approved-only application FK or CHECK.

**Decision:** 

## BR25 — Additional application materials are distinct from portfolio links

**Basis:** Functional requirement

**Source:** Apply for Job or Internship; Review and Manage Applications

**Tables:** application_materials, applications

**Enforcement:** FK, NOT NULL + application validation

**Detail:** Store additional file metadata and storage references in child records. Optional cover_note remains in applications. FRD does not define arbitrary per-job question/form schemas; required material types and actual file validation remain policy/service concerns.

**Decision:** Define any employer-required extra materials before designing dynamic forms.

## BR26 — Current application status and history remain consistent

**Basis:** Functional requirement

**Source:** Manage Applications; Recruiter: Review and Manage Applications; Additional 11

**Tables:** applications, application_statuses, application_status_history, users

**Enforcement:** FK, NOT NULL + authorization / transaction

**Detail:** Status lookup names are unique. Initial status-history row and later updates must commit with applications.status_id/status_updated_at atomically. FK does not make current status equal the newest history row or make history append-only. Authorized recruiter/student operations and allowed transitions need application logic.

**Decision:** Select statuses, legal transitions and timestamp ordering conventions.

## BR27 — Private employer notes do not imply a status change

**Basis:** Functional requirement

**Source:** Recruiter: Review and Manage Applications

**Tables:** application_notes, applications, users, company_memberships

**Enforcement:** FK, NOT NULL + authorization

**Detail:** Notes belong to one application and an existing author. Active Recruiter membership in the job company and note privacy require authorization. A status-history note belongs to an actual status event; a general note uses application_notes.

**Decision:** 

## BR28 — Invitations identify a specific student and opportunity

**Basis:** Functional requirement

**Source:** Recruiter: Invite Student to Apply; Additional 9

**Tables:** job_invitations, job_postings, student_profiles, users, applications

**Enforcement:** FK, NOT NULL, UNIQUE + authorization / transaction

**Detail:** UNIQUE(job_id,student_user_id) means one current invitation per pair (audited assumption). An application may have 0..1 invitation; an invitation may yield 0..1 application. Composite FK enforces same job/student; optional invitation NULL supports direct applications. Synchronize APPLIED invitation state transactionally. Recruiter authority is not checked by the actor FK.

**Decision:** Define invitation expiration, reinvitation and declined invitation application behavior.

## BR29 — Candidate shortlists do not require an application

**Basis:** Functional requirement

**Source:** Recruiter: Save or Shortlist Candidates

**Tables:** candidate_lists, candidate_list_items, student_profiles, users

**Enforcement:** PK/FK, NOT NULL, UNIQUE + authorization

**Detail:** Named lists are unique per recruiter. Composite list/student PK prevents duplicate members. List ownership and Recruiter role are application checks. List membership neither creates nor requires an application.

**Decision:** 

## BR30 — Messaging connects permitted participants

**Basis:** Functional requirement

**Source:** All Roles: Messaging; Recruiter/Student/Advisor communication requirements

**Tables:** conversations, conversation_participants, messages, users

**Enforcement:** PK/FK, NOT NULL + authorization / transaction

**Detail:** Composite message FK(conversation_id,sender_user_id) requires participant membership. Participant PK prevents duplicates. Logical minimum is 1..* participants; direct conversations require at least two distinct permitted users by workflow. No DB CHECK counts children or limits groups. Access on sending/reading/removal remains authorization.

**Decision:** Decide whether group chat is supported and whether participant removal must retain message references.

## BR31 — Application conversations cannot name a different job

**Basis:** Functional requirement

**Source:** Manage Applications; Communicate With Candidates

**Tables:** conversations, applications, job_postings

**Enforcement:** FK, CHECK

**Detail:** An application-linked conversation requires job_id by CHECK and a matching (application_id,job_id) tuple by composite FK. Job-only and general conversations remain valid. No unique constraint limits conversation count per job/application.

**Decision:** 

## BR32 — Notifications have a recipient and typed navigation context

**Basis:** Functional requirement

**Source:** All Roles: Notifications

**Tables:** notifications, users, posts, post_comments, companies, job_postings, applications, job_invitations, collaboration_verifications, conversations, reports, company_memberships, advisor_student_assignments

**Enforcement:** FK, NOT NULL, CHECK + application / transaction

**Detail:** Target-count CHECK permits zero or one target; FKs validate supplied targets. Zero is valid for system announcements. CHECK requires is_read/read_at consistency. Required target by notification type, recipient choice and reliable event/notification coordination are service logic, not CHECKs.

**Decision:** Define delivery fan-out, preference and reliable delivery/retry policy.

## BR33 — Advisor access requires authorization or assignment

**Basis:** Functional requirement

**Source:** Career Advisor: Review Student Career Profile; Monitor Student Career Activity

**Tables:** advisor_student_assignments, users, student_profiles

**Enforcement:** FK, NOT NULL, UNIQUE + authorization

**Detail:** Assignment source records administrator assignment or student authorization. One current advisor/student pair is unique. Advisor role, ACTIVE assignment and visibility of private career information require authorization at read/write time. Lifecycle times are not tied to status by CHECK.

**Decision:** Define access revocation, assignment renewal and history policy.

## BR34 — Private feedback targets the assigned studentâ€™s portfolio

**Basis:** Functional requirement

**Source:** Career Advisor: Provide Portfolio Feedback; Recommend Portfolio Content

**Tables:** portfolio_feedback, advisor_student_assignments, posts

**Enforcement:** Composite FK, NOT NULL + authorization

**Detail:** Assignment/student tuple must match. If post_id is supplied, post/author tuple must match the same student. Null post is permitted for whole-portfolio feedback or missing-content suggestions. Privacy, ACTIVE assignment and portfolio post type are application checks.

**Decision:** 

## BR35 — Advisors recommend opportunities to assigned students

**Basis:** Functional requirement

**Source:** Career Advisor: Recommend Opportunities

**Tables:** opportunity_recommendations, advisor_student_assignments, job_postings

**Enforcement:** FK, NOT NULL, UNIQUE + authorization

**Detail:** One current assignment/job recommendation is unique, an audited design limit. Assignment supplies the student; do not duplicate student identity in this relation. Active authorized advisor access and recommending eligible opportunities require application validation.

**Decision:** Define repeated recommendations after dismissal or changed job content.

## BR36 — Assistance and analytics are derived from source records

**Basis:** Functional requirement

**Source:** Career Advisor: Identify Students Needing Assistance; Administrator: Generate Analytics and Reports

**Tables:** student_profiles, posts, applications, job_invitations, view_events, advisor_student_assignments

**Enforcement:** Authorized queries

**Detail:** Completeness, activity counts and analytics are computed over existing entities. Advisor queries must stay within permitted assignments; global analytics need administrator authorization. There are no independent dashboard/report-result tables.

**Decision:** Define completeness thresholds and engagement aggregation rules.

## BR37 — Reports target exactly one existing object

**Basis:** Functional requirement

**Source:** Administrator: Manage Reported Activity; Company Representative: Respond to Company-Related Reports

**Tables:** reports, users, posts, post_comments, messages, job_postings, companies

**Enforcement:** FK, NOT NULL, CHECK + authorization

**Detail:** Exactly-one-target CHECK requires one typed report target. Reporter and each supplied target must exist. Confidential report access and company response scope require authorization. Reports target FKs should use RESTRICT in physical MySQL DDL, as recorded in DBML comments.

**Decision:** Choose retention, resolution transitions and response workflow.

## BR38 — Moderation actions preserve accountable decisions

**Basis:** Functional requirement + Version B experimental audit policy

**Source:** Administrator: Moderate Posts and Social Content; Manage Employment Content; Manage Reported Activity

**Tables:** moderation_actions, reports, users, posts, post_comments, job_postings, companies

**Enforcement:** FK, NOT NULL + authorization / transaction

**Detail:** Each action references a report and an existing administrator user; the user FK does not verify Administrator role. Apply effective target status and document action atomically. Required action_type is an enum; context/action compatibility is application logic. Version B triggers reject UPDATE and DELETE of moderation_actions. Effective content removal and insertion of the audit action must still be atomic application logic. An experimental CHECK requires a nonblank notes reason for every action except NO_ACTION.

**Decision:** Define legal sanctions, unmoderation and retention/purge policy.

## BR39 — Category merges preserve references and avoid cycles

**Basis:** Functional requirement

**Source:** Administrator: Manage Skills and Academic Categories

**Tables:** skills, majors, academic_disciplines, industries, student_skills, post_skills, job_skills, job_disciplines

**Enforcement:** FK, UNIQUE, CHECK (source inactive) + transactional validation (self-target, cycle, reassignment)

**Detail:** Merge destination must exist by FK, and a merged source must be inactive by CHECK. MySQL CHECK expressions cannot reference the source AUTO_INCREMENT ID, so self-target rejection, cycle prevention, active destination checks, reassignment and duplicate-junction reconciliation require transactional validation in Sprint III. Major names remain unique within discipline; other named categories are individually unique.

**Decision:** Define cross-discipline major merges and merge/restore policy.

## BR40 — System-wide configurable policies have attributable changes

**Basis:** Functional requirement

**Source:** Administrator: Manage System Configuration

**Tables:** system_settings, application_statuses, users

**Enforcement:** PK/FK, NOT NULL, UNIQUE + authorization / application validation

**Detail:** Setting key is the PK; updater is optional. Setting values are text with no per-key type/range CHECK. Application statuses are configurable records; enum domains in DBML remain declared finite vocabularies, not automatically editable settings.

**Decision:** Define allowed setting keys, value validation and which enum domains need future configuration support.

## BR41 — Referenced evidence is not silently erased

**Basis:** Functional requirement + Version B experimental retention design

**Source:** Schema audit: Enforcement boundary; no explicit FRD retention duration

**Tables:** applications, application_portfolio_posts, posts, reports, moderation_actions, messages

**Enforcement:** Physical FK delete policy / application policy

**Detail:** Version B retains live provenance/ownership links and adds immutable evidence metadata snapshots. Snapshot and moderation-history triggers reject updates/deletes by ordinary DML. Soft-removal timestamps retain content rows. Actual file byte preservation, eventual retention/purge permissions and production adoption require team-approved storage and application policies. Evidence is immutable AFTER sealing: snapshot child inserts/updates/deletes are blocked once sealed_at is set, and application attachment changes are blocked once evidence_sealed_at is set. Sealing an application requires at least one sealed attachment. Staging is confined to the planned submission transaction.

**Decision:** Review experimental snapshot, media retention, restoration and purge policy before production.

## BX01 — Frozen evidence metadata

**Basis:** Version B experimental policy; not team-approved FRD requirement

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** application_post_snapshots, application_snapshot_media, application_snapshot_skills, application_portfolio_posts

**Enforcement:** Matching FKs, UNIQUE, NOT NULL, CHECK + sealing/immutability SQL triggers; planned capture transaction

**Detail:** Each attachment selects one matching snapshot. Snapshot metadata cannot be updated/deleted by ordinary DML after insertion. Capture the header, ordered media and selected skill names before completing submission. Evidence is immutable AFTER sealing: snapshot child inserts/updates/deletes are blocked once sealed_at is set, and application attachment changes are blocked once evidence_sealed_at is set. Sealing an application requires at least one sealed attachment. Staging is confined to the planned submission transaction.

**Decision:** Approve frozen evidence as a Version B policy; keep Version A live references unchanged.

## BX02 — Captured file bytes are retained

**Basis:** Version B experimental policy; not team-approved FRD requirement

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** application_snapshot_media

**Enforcement:** Planned storage/application workflow

**Detail:** Copy or pin bytes to an immutable versioned key and compute the hash before committing evidence. A copied URL and a stored hash alone do not preserve a file. Coordinate cleanup after failed captures.

**Decision:** Approve storage lifecycle and retention. No storage backend is implemented by this package.

## BX03 — Verification changes follow a recorded transition

**Basis:** Version B experimental policy; not team-approved FRD requirement

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** verification_transitions, collaboration_verification_history, collaboration_verifications

**Enforcement:** Composite FK, CHECK, append-only triggers + transactional authorization

**Detail:** Seeded experimental pairs are PENDINGâ†’APPROVED/REJECTED and APPROVED/REJECTEDâ†’PENDING for re-verification. Initial history has null prior status and PENDING outcome. Lock/check the current request, verify authority and commit its new state with the event.

**Decision:** Group approval required for retry/re-verification policy before Sprint III.

## BX04 — Skill aliases and hierarchy reuse canonical skills

**Basis:** Version B experimental policy; not team-approved FRD requirement

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** skills, skill_aliases

**Enforcement:** Self-FK, UNIQUE, NOT NULL + application cycle validation

**Detail:** An alias selects one skill and a skill may have an optional broader skill. Alias spelling uniqueness follows database collation. Reject self-parenting and longer cycles in transactional taxonomy administration; no EAV or polymorphic references introduced.

**Decision:** Approve hierarchy/alias administration; do not equate hierarchy with merge destinations.

## BX05 — Logical removal and audit agree

**Basis:** Functional requirement + Version B experimental audit policy

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** posts, post_comments, messages, moderation_actions

**Enforcement:** CHECK + append-only triggers + transactional authorization

**Detail:** A removed state has a removal timestamp, and a timestamp requires the removed state. Keep the row and record an attributed moderation action atomically. Administrator permissions and target/action compatibility remain application checks. An experimental CHECK requires a nonblank notes reason for every action except NO_ACTION.

**Decision:** Approve restoration and purge policy; append-only SQL is an experimental retention choice.

## BX06 — Discovery indexes are query-specific

**Basis:** Version B experimental policy; not team-approved FRD requirement

**Source:** Teammate original proposal + user authorization for experimental Version B

**Tables:** student_profiles, collaboration_verifications

**Enforcement:** Non-unique indexes; not business validation

**Detail:** Define major/graduation-year and status/post lookup indexes on actual base-table columns. Existing typed skill bridges remain. Measure representative MySQL plans; no view index, cross-table index or speed guarantee is claimed.

**Decision:** Benchmark and approve physical indexes before production.
