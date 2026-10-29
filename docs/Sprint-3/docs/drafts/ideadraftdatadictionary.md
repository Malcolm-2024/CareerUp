# CareerUp — Simplified Data Dictionary

**Scope:** The approved 12-table core ERD.

Data types, lengths, required non-key fields, defaults, and status values below are proposed implementation choices for team confirmation.

## Legend

| Label | Meaning |
| :--- | :--- |
| **PK** | Primary key |
| **FK** | Foreign key |
| **UQ** | Unique value or combination of fields |
| **Yes** | Required; cannot be NULL |
| **No** | Optional; may be NULL |
| **Conditional** | Required at a particular workflow stage |

---

## 1. USERS

Stores accounts and basic profile information.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `user_id` | `INT UNSIGNED` | Yes | PK | Unique account ID. |
| `company_id` | `INT UNSIGNED` | No | FK | References `COMPANIES.company_id`. |
| `name` | `VARCHAR(100)` | Yes | — | User's displayed name. |
| `email` | `VARCHAR(254)` | Yes | UQ | Unique sign-in email address. |
| `password_hash` | `VARCHAR(255)` | Yes | — | Password hash; never a plain-text password. |
| `role` | `VARCHAR(30)` | Yes | — | Student, Recruiter, Company Representative, Career Advisor, or Administrator. |
| `school` | `VARCHAR(150)` | No | — | School affiliation. |
| `major` | `VARCHAR(100)` | No | — | Academic major. |
| `graduation_year` | `SMALLINT UNSIGNED` | No | — | Expected or completed graduation year. |
| `bio` | `TEXT` | No | — | Professional biography. |

**Relationship:** A user has zero or one company affiliation. A company may have many users.

**Design assumption:** Each account has one role and at most one company affiliation.

---

## 2. COMPANIES

Stores employer profiles.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `company_id` | `INT UNSIGNED` | Yes | PK | Unique employer ID. |
| `name` | `VARCHAR(150)` | Yes | — | Company name. |
| `industry` | `VARCHAR(100)` | No | — | Employer's industry. |
| `description` | `TEXT` | No | — | Company introduction and culture information. |
| `website` | `VARCHAR(500)` | No | — | Company website address. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `PENDING`, `APPROVED`, or `INACTIVE`. |

**Relationships:** A company may have many affiliated users, opportunities, and collaboration requests.

---

## 3. POSTS

Stores content used in portfolios and the discovery feed.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `post_id` | `INT UNSIGNED` | Yes | PK | Unique post ID. |
| `author_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`. |
| `title` | `VARCHAR(150)` | Yes | — | Project or accomplishment title. |
| `description` | `TEXT` | No | — | Explanation of the work. |
| `created_at` | `DATETIME` | Yes | — | Creation timestamp used for feed sorting. |
| `category` | `VARCHAR(100)` | No | — | Academic or project category. |
| `visibility` | `VARCHAR(20)` | Yes | — | Proposed: `PUBLIC` or `PRIVATE`. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `DRAFT`, `PUBLISHED`, or `ARCHIVED`. |

**Relationships:**

- Each post has exactly one author.
- A user may create many posts.
- A post may have multiple media attachments and skill tags.
- The portfolio and discovery feed reuse the same post.

---

## 4. POST_MEDIA

Stores image and video references for posts.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `media_id` | `INT UNSIGNED` | Yes | PK | Unique media ID. |
| `post_id` | `INT UNSIGNED` | Yes | FK | References `POSTS.post_id`. |
| `media_url` | `VARCHAR(1000)` | Yes | — | Location of the uploaded media. |
| `media_type` | `VARCHAR(10)` | Yes | — | Proposed: `IMAGE` or `VIDEO`. |
| `display_order` | `SMALLINT UNSIGNED` | Yes | UQ pair | Positive display position within the post. |

**Unique constraint:** `(post_id, display_order)`.

**Relationship:** Each media record belongs to one post. A post may have multiple media records.

**Publication rule:** A draft may have no media. The application must require at least one valid media item before publishing a media post.

**Storage note:** The database stores media references, not the uploaded file bytes.

---

## 5. SKILLS

Stores standardized skill names.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `skill_id` | `INT UNSIGNED` | Yes | PK | Unique skill ID. |
| `name` | `VARCHAR(100)` | Yes | UQ | Unique skill name, such as Java or Graphic Design. |
| `active` | `BOOLEAN` | Yes | — | Whether the skill is available for selection. |

**Relationships:** A skill may be associated with many users and posts.

**Proposed default:** `active = TRUE`.

---

## 6. USER_SKILLS

Connects users to their profile skills.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `user_id` | `INT UNSIGNED` | Yes | PK + FK | References `USERS.user_id`. |
| `skill_id` | `INT UNSIGNED` | Yes | PK + FK | References `SKILLS.skill_id`. |

**Composite primary key:** `(user_id, skill_id)`.

**Relationships:**

- A user may have multiple skills.
- A skill may belong to multiple users.
- Each linking record references exactly one user and one skill.

**Duplicate prevention:** The same user/skill pair cannot appear twice.

---

## 7. POST_SKILLS

Connects posts to their skill tags.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `post_id` | `INT UNSIGNED` | Yes | PK + FK | References `POSTS.post_id`. |
| `skill_id` | `INT UNSIGNED` | Yes | PK + FK | References `SKILLS.skill_id`. |

**Composite primary key:** `(post_id, skill_id)`.

**Relationships:**

- A post may have multiple skill tags.
- A skill may be associated with multiple posts.
- Each linking record references exactly one post and one skill.

**Duplicate prevention:** The same post/skill pair cannot appear twice.

---

## 8. COLLABORATIONS

Stores company verification requests for student-owned portfolio posts.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `collaboration_id` | `INT UNSIGNED` | Yes | PK | Unique verification request ID. |
| `post_id` | `INT UNSIGNED` | Yes | FK + UQ | References `POSTS.post_id`. |
| `student_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`; must match the post's author. |
| `company_id` | `INT UNSIGNED` | Yes | FK | References `COMPANIES.company_id`; identifies the tagged company. |
| `reviewer_id` | `INT UNSIGNED` | Conditional | FK | References `USERS.user_id`; identifies the reviewing company representative. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `PENDING`, `APPROVED`, or `REJECTED`. |
| `reviewed_at` | `DATETIME` | Conditional | — | Date and time of the approval or rejection. |
| `showcase` | `BOOLEAN` | Yes | — | Whether approved work is selected for the company showcase. |

**Verification rules:**

1. The requesting student must own the referenced post: `COLLABORATIONS.student_id = POSTS.author_id`.
2. The request must reference a company.
3. Only an authorized representative of that company may approve or reject.
4. `reviewer_id` and `reviewed_at` are NULL while pending and required after a decision.
5. A company tag alone does not mean the work is verified.
6. Only an approved request may display a verified indicator.
7. `showcase = TRUE` is allowed only for approved work.

**Relationship:** A post has zero or one current collaboration request under the proposed unique `post_id` constraint.

> **Design assumption requiring team approval:** One current verification request per post. This is not an explicit restriction in the original functional requirements.

**Validation note:** Foreign keys alone do not enforce ownership, reviewer roles, or company authorization.

---

## 9. OPPORTUNITIES

Stores jobs and internships offered by employers.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `job_id` | `INT UNSIGNED` | Yes | PK | Unique opportunity ID. |
| `company_id` | `INT UNSIGNED` | Yes | FK | References `COMPANIES.company_id`. |
| `recruiter_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`; identifies the creating recruiter. |
| `title` | `VARCHAR(150)` | Yes | — | Position title. |
| `description` | `TEXT` | Yes | — | Responsibilities and requirements. |
| `location` | `VARCHAR(150)` | No | — | Work location or remote arrangement. |
| `type` | `VARCHAR(30)` | Yes | — | Proposed: `JOB` or `INTERNSHIP`. |
| `deadline` | `DATE` | No | — | Application deadline, if specified. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `DRAFT`, `OPEN`, or `CLOSED`. |

**Relationships:**

- Each opportunity belongs to one company.
- Each opportunity records one creating recruiter.
- An opportunity may receive many applications and invitations.

**Authorization rule:** The recruiter must be authorized to act for the opportunity's company.

---

## 10. INVITATIONS

Stores recruiter invitations asking students to apply to opportunities.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `invitation_id` | `INT UNSIGNED` | Yes | PK | Unique invitation ID. |
| `recruiter_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`; identifies the sender. |
| `student_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`; identifies the recipient. |
| `job_id` | `INT UNSIGNED` | Yes | FK | References `OPPORTUNITIES.job_id`. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `SENT`, `ACCEPTED`, `DECLINED`, or `EXPIRED`. |
| `sent_at` | `DATETIME` | Yes | — | Date and time sent. |

**Unique constraint:** `(student_id, job_id)`.

**Relationships:** Each invitation references one recruiter, one student, and one opportunity.

**Design decision:** One current invitation per student/opportunity pair. Reinvitation policy requires team confirmation.

**Important distinction:** An invitation asks a student to apply. It is not an application, and accepting it does not automatically create an application.

---

## 11. APPLICATIONS

Stores student submissions to opportunities.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `application_id` | `INT UNSIGNED` | Yes | PK | Unique application ID. |
| `student_id` | `INT UNSIGNED` | Yes | FK | References `USERS.user_id`; identifies the applicant. |
| `job_id` | `INT UNSIGNED` | Yes | FK | References `OPPORTUNITIES.job_id`. |
| `status` | `VARCHAR(20)` | Yes | — | Proposed: `SUBMITTED`, `UNDER_REVIEW`, `INTERVIEW`, `REJECTED`, or `OFFERED`. |
| `submitted_at` | `DATETIME` | Yes | — | Date and time submitted. |

**Unique constraint:** `(student_id, job_id)`.

**Relationships:**

- Each application references one student and one opportunity.
- A student may apply to multiple opportunities.
- An opportunity may receive applications from multiple students.
- An application may include multiple selected portfolio posts through `APPLICATION_POSTS`.

**Design decision:** One application per student/opportunity pair. Reapplication policy requires team confirmation.

**Important distinction:** A student may apply without receiving an invitation. The current model does not store a direct invitation-to-application link.

---

## 12. APPLICATION_POSTS

Connects applications to selected portfolio posts.

| Field | Proposed Type | Required | Key | Description |
| :--- | :--- | :---: | :--- | :--- |
| `application_id` | `INT UNSIGNED` | Yes | PK + FK | References `APPLICATIONS.application_id`. |
| `post_id` | `INT UNSIGNED` | Yes | PK + FK | References `POSTS.post_id`. |

**Composite primary key:** `(application_id, post_id)`.

**Relationships:**

- An application may include multiple portfolio posts.
- A post may support multiple applications.
- Each linking record references exactly one application and one post.

**Rules:**

- The same application/post pair cannot appear twice.
- Each selected post must belong to the applicant.
- The original requirements do not require every selected post to have company verification.

---

## Shared Implementation Decisions

- Generate single-column ID primary keys automatically.
- Keep foreign-key data types identical to their referenced primary keys.
- Agree on email and skill-name case handling before implementation.
- Validate active skills, required content, allowed status changes, and role permissions.
- Determine deletion behavior before implementation.
- Foreign keys establish record existence; they do not establish authorization.
- Actual media upload and hosting are separate from storing media references.

## Deferred or Simplified Features

This dictionary matches the simplified 12-table core ERD. It does not claim full support for every original functional requirement.

The following remain deferred or simplified:

- Messaging and notifications.
- Likes, comments, shares, follows, and saves.
- Advisor assignments, feedback, and recommendations.
- Recruiter candidate lists.
- Moderation, reporting, and analytics.
- Configurable permissions and system settings.
- Managed academic-category and industry catalogs.
- Application files other than selected portfolio posts.
- Full verification and application-status history.
- Multiple company affiliations per account.
