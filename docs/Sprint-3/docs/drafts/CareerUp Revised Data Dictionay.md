# CareerUp Data Dictionary and ERD

Oct 29, 2026 · @m

The CareerUp MySQL 8 schema has 24 tables: 5 core entities (users, companies, posts, jobs, applications), 9 supporting tables for skills, media, comments and messaging, and 10 lookup, junction and admin tables. Key column: PK = primary key, FK = foreign key (→ parent table), PK, FK = both, None = neither. Required = Yes means NOT NULL; fields the database fills by default are marked "Yes (default)".

## Entity relationship diagram

The full ERD is a separate page: [CareerUp ERD](https://claude.ai/artifact/5GCPQUhECLSfE1QpR724wj). It draws all 24 tables with every column, PK and FK markers, and crow's-foot relationships. The Relationships table below lists the same 40 foreign keys in text form.

## Relationships

The schema declares 40 foreign keys, all with ON UPDATE CASCADE. Every row below is many-to-one from the child column to its parent; junction tables turn those into many-to-many links.

| Child table | Column | Parent table | ON DELETE |
| --- | --- | --- | --- |
| companies | industry\_id | industries | SET NULL |
| users | company\_id | companies | SET NULL |
| users | major\_id | majors | SET NULL |
| student\_skills | student\_id | users | CASCADE |
| student\_skills | skill\_id | skills | CASCADE |
| posts | author\_id | users | RESTRICT |
| posts | company\_id | companies | SET NULL |
| posts | major\_id | majors | SET NULL |
| posts | verified\_by | users | SET NULL |
| post\_media | post\_id | posts | CASCADE |
| post\_skills | post\_id | posts | CASCADE |
| post\_skills | skill\_id | skills | CASCADE |
| comments | post\_id | posts | CASCADE |
| comments | user\_id | users | RESTRICT |
| interactions | user\_id | users | RESTRICT |
| jobs | company\_id | companies | RESTRICT |
| jobs | posted\_by | users | RESTRICT |
| job\_skills | job\_id | jobs | CASCADE |
| job\_skills | skill\_id | skills | CASCADE |
| job\_majors | job\_id | jobs | CASCADE |
| job\_majors | major\_id | majors | CASCADE |
| applications | job\_id | jobs | RESTRICT |
| applications | student\_id | users | RESTRICT |
| application\_posts | application\_id | applications | CASCADE |
| application\_posts | post\_id | posts | RESTRICT |
| invitations | job\_id | jobs | RESTRICT |
| invitations | student\_id | users | RESTRICT |
| invitations | recruiter\_id | users | RESTRICT |
| messages | sender\_id | users | RESTRICT |
| messages | receiver\_id | users | RESTRICT |
| messages | application\_id | applications | SET NULL |
| notifications | user\_id | users | CASCADE |
| advisor\_students | advisor\_id | users | RESTRICT |
| advisor\_students | student\_id | users | RESTRICT |
| advisor\_notes | advisor\_id | users | RESTRICT |
| advisor\_notes | student\_id | users | RESTRICT |
| advisor\_notes | post\_id | posts | SET NULL |
| advisor\_notes | job\_id | jobs | SET NULL |
| reports | reporter\_id | users | RESTRICT |
| reports | resolved\_by | users | SET NULL |

## Data dictionary: lookup tables

### Entity: industries

Industry categories a company can belong to.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the industry | INT UNSIGNED, auto-increment | PK | Yes |
| name | Industry name (unique) | VARCHAR(150) | None | Yes |
| is\_active | Whether the industry can still be selected | BOOLEAN | None | Yes (default TRUE) |

### Entity: majors

Academic majors used for student profiles, posts and job targeting.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the major | INT UNSIGNED, auto-increment | PK | Yes |
| name | Major name (unique) | VARCHAR(150) | None | Yes |
| discipline | Broader field the major belongs to | VARCHAR(150) | None | No |
| is\_active | Whether the major can still be selected | BOOLEAN | None | Yes (default TRUE) |

### Entity: skills

Master list of skills attached to students, posts and jobs.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the skill | INT UNSIGNED, auto-increment | PK | Yes |
| name | Skill name (unique) | VARCHAR(100) | None | Yes |
| is\_active | Whether the skill can still be selected | BOOLEAN | None | Yes (default TRUE) |
| created\_at | When the skill was added | TIMESTAMP | None | Yes (default current time) |

### Entity: role\_permissions

Maps each user role to the permissions it holds. Composite primary key; `role` is not a foreign key because roles are an enum on `users`.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| role | User role: STUDENT, RECRUITER, COMPANY\_REP, CAREER\_ADVISOR or ADMIN | ENUM | PK (composite) | Yes |
| permission | Permission code, e.g. MANAGE\_JOBS | VARCHAR(100) | PK (composite) | Yes |

### Entity: settings

Key-value store for platform-wide configuration.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| setting\_key | Unique name of the setting | VARCHAR(100) | PK | Yes |
| setting\_value | Value of the setting | TEXT | None | Yes |
| updated\_at | When the setting last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

## Data dictionary: users and companies

### Entity: companies

Employers that post jobs and company content.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the company | INT UNSIGNED, auto-increment | PK | Yes |
| name | Company name | VARCHAR(255) | None | Yes |
| industry\_id | Industry the company operates in | INT UNSIGNED | FK → industries.id | No |
| description | About the company | TEXT | None | No |
| logo\_url | Link to the company logo | VARCHAR(2048) | None | No |
| website | Company website | VARCHAR(2048) | None | No |
| locations | Office locations | TEXT | None | No |
| culture | Description of company culture | TEXT | None | No |
| status | Approval state: PENDING, ACTIVE, REJECTED, SUSPENDED | ENUM | None | Yes (default PENDING) |
| is\_verified | Whether an admin has verified the company | BOOLEAN | None | Yes (default FALSE) |
| created\_at | When the record was created | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the record last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: users

Every account on the platform. The `role` column decides which optional fields apply: students use major, school and grad\_year; recruiters and company reps use company\_id.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the user | INT UNSIGNED, auto-increment | PK | Yes |
| email | Login email address (unique) | VARCHAR(255) | None | Yes |
| password\_hash | Hashed password | VARCHAR(255) | None | Yes |
| role | STUDENT, RECRUITER, COMPANY\_REP, CAREER\_ADVISOR or ADMIN | ENUM | None | Yes |
| status | Account state: PENDING, ACTIVE, SUSPENDED, BANNED, INACTIVE | ENUM | None | Yes (default PENDING) |
| name | Full name | VARCHAR(150) | None | Yes |
| photo\_url | Link to profile photo | VARCHAR(2048) | None | No |
| phone | Phone number | VARCHAR(30) | None | No |
| organization | Organization name (for advisors and other staff) | VARCHAR(255) | None | No |
| company\_id | Employer the user works for | INT UNSIGNED | FK → companies.id | No |
| major\_id | Student's major | INT UNSIGNED | FK → majors.id | No |
| school | School or university | VARCHAR(255) | None | No |
| grad\_year | Expected or actual graduation year | SMALLINT UNSIGNED | None | No |
| bio | Profile biography | TEXT | None | No |
| interests | Career interests | TEXT | None | No |
| created\_at | When the account was created | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the account last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: student\_skills

Junction table: which skills each student has.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| student\_id | The student (a user with role STUDENT) | INT UNSIGNED | PK, FK → users.id | Yes |
| skill\_id | The skill the student has | INT UNSIGNED | PK, FK → skills.id | Yes |

### Entity: advisor\_students

Junction table linking career advisors to the students they can see.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| advisor\_id | The career advisor (a user with role CAREER\_ADVISOR) | INT UNSIGNED | PK, FK → users.id | Yes |
| student\_id | The student the advisor works with | INT UNSIGNED | PK, FK → users.id | Yes |
| access\_type | How access was granted: ASSIGNED or AUTHORIZED | ENUM | None | Yes (default ASSIGNED) |
| status | Access state: PENDING, ACTIVE or REVOKED | ENUM | None | Yes (default PENDING) |
| created\_at | When the link was created | TIMESTAMP | None | Yes (default current time) |

## Data dictionary: portfolio content

### Entity: posts

Portfolio posts by students and content posts by companies, with an optional verification step.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the post | INT UNSIGNED, auto-increment | PK | Yes |
| author\_id | User who wrote the post | INT UNSIGNED | FK → users.id | Yes |
| company\_id | Company the post is tied to (collaboration or company post) | INT UNSIGNED | FK → companies.id | No |
| major\_id | Major the post is categorized under | INT UNSIGNED | FK → majors.id | No |
| post\_type | PORTFOLIO or COMPANY | ENUM | None | Yes (default PORTFOLIO) |
| title | Post title | VARCHAR(255) | None | Yes |
| description | Post body | TEXT | None | No |
| verification\_status | NOT\_REQUESTED, PENDING, APPROVED or REJECTED | ENUM | None | Yes (default NOT\_REQUESTED) |
| verified\_by | User who approved or rejected the verification | INT UNSIGNED | FK → users.id | No |
| verified\_at | When verification was decided | DATETIME | None | No |
| is\_showcased | Whether the post is featured | BOOLEAN | None | Yes (default FALSE) |
| status | DRAFT, PUBLISHED, HIDDEN or REMOVED | ENUM | None | Yes (default DRAFT) |
| created\_at | When the post was created | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the post last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: post\_media

Images and videos attached to a post.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the media item | INT UNSIGNED, auto-increment | PK | Yes |
| post\_id | Post the media belongs to | INT UNSIGNED | FK → posts.id | Yes |
| media\_type | VIDEO or IMAGE | ENUM | None | Yes |
| url | Link to the file | VARCHAR(2048) | None | Yes |
| duration\_seconds | Video length in seconds | INT UNSIGNED | None | No |
| sort\_order | Display order within the post | INT | None | Yes (default 0) |
| created\_at | When the media was added | TIMESTAMP | None | Yes (default current time) |

### Entity: post\_skills

Junction table: skills demonstrated in a post.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| post\_id | The post | INT UNSIGNED | PK, FK → posts.id | Yes |
| skill\_id | The skill shown | INT UNSIGNED | PK, FK → skills.id | Yes |

### Entity: comments

Comments left on posts.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the comment | INT UNSIGNED, auto-increment | PK | Yes |
| post\_id | Post being commented on | INT UNSIGNED | FK → posts.id | Yes |
| user\_id | User who wrote the comment | INT UNSIGNED | FK → users.id | Yes |
| content | Comment text | TEXT | None | Yes |
| status | VISIBLE, HIDDEN or REMOVED | ENUM | None | Yes (default VISIBLE) |
| created\_at | When the comment was created | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the comment last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: interactions

Views, likes, saves, shares and follows on posts, companies, students and jobs. `target_id` points to a different table depending on `target_type`, so it has no foreign key.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the interaction | INT UNSIGNED, auto-increment | PK | Yes |
| user\_id | User who performed the action | INT UNSIGNED | FK → users.id | Yes |
| target\_type | What was acted on: POST, COMPANY, STUDENT or JOB | ENUM | None | Yes |
| target\_id | Id of the target row in the table named by target\_type | INT UNSIGNED | None (polymorphic) | Yes |
| type | Action: VIEW, LIKE, SAVE, SHARE or FOLLOW | ENUM | None | Yes |
| list\_name | Name of the saved list, when type is SAVE | VARCHAR(100) | None | No |
| created\_at | When the action happened | TIMESTAMP | None | Yes (default current time) |

## Data dictionary: jobs and applications

### Entity: jobs

Job and internship listings posted by recruiters for a company.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the job | INT UNSIGNED, auto-increment | PK | Yes |
| company\_id | Company offering the job | INT UNSIGNED | FK → companies.id | Yes |
| posted\_by | User who created the listing | INT UNSIGNED | FK → users.id | Yes |
| title | Job title | VARCHAR(255) | None | Yes |
| description | Job description | TEXT | None | Yes |
| location | Job location | VARCHAR(255) | None | No |
| employment\_type | FULL\_TIME, PART\_TIME, INTERNSHIP, CONTRACT or OTHER | ENUM | None | Yes |
| deadline | Application deadline | DATE | None | No |
| status | DRAFT, PUBLISHED, CLOSED, EXPIRED or REMOVED | ENUM | None | Yes (default DRAFT) |
| created\_at | When the listing was created | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the listing last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: job\_skills

Junction table: skills a job asks for.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| job\_id | The job | INT UNSIGNED | PK, FK → jobs.id | Yes |
| skill\_id | The skill requested | INT UNSIGNED | PK, FK → skills.id | Yes |
| is\_required | TRUE if required, FALSE if only preferred | BOOLEAN | None | Yes (default TRUE) |

### Entity: job\_majors

Junction table: majors a job targets.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| job\_id | The job | INT UNSIGNED | PK, FK → jobs.id | Yes |
| major\_id | The targeted major | INT UNSIGNED | PK, FK → majors.id | Yes |

### Entity: applications

A student's application to a job. One application per student per job (unique on job\_id + student\_id).

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the application | INT UNSIGNED, auto-increment | PK | Yes |
| job\_id | Job applied to | INT UNSIGNED | FK → jobs.id | Yes |
| student\_id | Student who applied | INT UNSIGNED | FK → users.id | Yes |
| status | SUBMITTED, IN\_REVIEW, INTERVIEW, OFFERED, REJECTED or WITHDRAWN | ENUM | None | Yes (default SUBMITTED) |
| recruiter\_notes | Private notes from the recruiter | TEXT | None | No |
| submitted\_at | When the application was submitted | TIMESTAMP | None | Yes (default current time) |
| updated\_at | When the application last changed | TIMESTAMP, auto-updates | None | Yes (default current time) |

### Entity: application\_posts

Junction table: portfolio posts a student attached to an application.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| application\_id | The application | INT UNSIGNED | PK, FK → applications.id | Yes |
| post\_id | The portfolio post attached | INT UNSIGNED | PK, FK → posts.id | Yes |

### Entity: invitations

Invitations from a recruiter asking a student to apply. One invitation per student per job.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the invitation | INT UNSIGNED, auto-increment | PK | Yes |
| job\_id | Job the student is invited to | INT UNSIGNED | FK → jobs.id | Yes |
| student\_id | Student being invited | INT UNSIGNED | FK → users.id | Yes |
| recruiter\_id | Recruiter who sent the invitation | INT UNSIGNED | FK → users.id | Yes |
| message | Personal note to the student | TEXT | None | No |
| status | SENT, ACCEPTED, DECLINED or EXPIRED | ENUM | None | Yes (default SENT) |
| created\_at | When the invitation was sent | TIMESTAMP | None | Yes (default current time) |

## Data dictionary: messaging

### Entity: messages

Direct messages between two users, optionally tied to an application.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the message | INT UNSIGNED, auto-increment | PK | Yes |
| sender\_id | User who sent the message | INT UNSIGNED | FK → users.id | Yes |
| receiver\_id | User who receives the message | INT UNSIGNED | FK → users.id | Yes |
| application\_id | Application the conversation is about | INT UNSIGNED | FK → applications.id | No |
| content | Message text | TEXT | None | Yes |
| status | SENT, DELIVERED, READ or DELETED | ENUM | None | Yes (default SENT) |
| created\_at | When the message was sent | TIMESTAMP | None | Yes (default current time) |
| read\_at | When the receiver read it | DATETIME | None | No |

### Entity: notifications

In-app alerts for a user. `reference_id` points to a different table depending on `reference_type`, so it has no foreign key.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the notification | INT UNSIGNED, auto-increment | PK | Yes |
| user\_id | User who receives the notification | INT UNSIGNED | FK → users.id | Yes |
| type | Notification category, e.g. new message or application update | VARCHAR(100) | None | Yes |
| reference\_type | Kind of record the notification links to | VARCHAR(50) | None | No |
| reference\_id | Id of the linked record | INT UNSIGNED | None (polymorphic) | No |
| is\_read | Whether the user has seen it | BOOLEAN | None | Yes (default FALSE) |
| created\_at | When the notification was created | TIMESTAMP | None | Yes (default current time) |

## Data dictionary: advisor feedback and moderation

### Entity: advisor\_notes

Feedback, job recommendations and content suggestions from an advisor to a student.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the note | INT UNSIGNED, auto-increment | PK | Yes |
| advisor\_id | Advisor who wrote the note | INT UNSIGNED | FK → users.id | Yes |
| student\_id | Student the note is for | INT UNSIGNED | FK → users.id | Yes |
| post\_id | Post the note refers to | INT UNSIGNED | FK → posts.id | No |
| job\_id | Job the note recommends | INT UNSIGNED | FK → jobs.id | No |
| kind | FEEDBACK, OPPORTUNITY or CONTENT | ENUM | None | Yes |
| content | Note text | TEXT | None | No |
| created\_at | When the note was written | TIMESTAMP | None | Yes (default current time) |

### Entity: reports

User reports of posts, comments, users, messages, jobs or companies, handled by admins. `target_id` points to a different table depending on `target_type`, so it has no foreign key.

| Field | Description | Data Type | Key | Required |
| --- | --- | --- | --- | --- |
| id | Unique identifier for the report | INT UNSIGNED, auto-increment | PK | Yes |
| reporter\_id | User who filed the report | INT UNSIGNED | FK → users.id | Yes |
| target\_type | What is reported: POST, COMMENT, USER, MESSAGE, JOB or COMPANY | ENUM | None | Yes |
| target\_id | Id of the reported row in the table named by target\_type | INT UNSIGNED | None (polymorphic) | Yes |
| reason | Why it was reported | TEXT | None | Yes |
| status | OPEN, REVIEWING, RESOLVED or DISMISSED | ENUM | None | Yes (default OPEN) |
| resolved\_by | Admin who closed the report | INT UNSIGNED | FK → users.id | No |
| action\_taken | What the admin did | TEXT | None | No |
| created\_at | When the report was filed | TIMESTAMP | None | Yes (default current time) |
| resolved\_at | When the report was closed | DATETIME | None | No |

## Design notes

- **Many-to-many links:** student\_skills (students and skills), post\_skills (posts and skills), job\_skills (jobs and skills), job\_majors (jobs and majors), application\_posts (applications and posts) and advisor\_students (advisors and students). Each uses a composite primary key made of its two foreign keys.
- **One row per pair:** applications and invitations are unique on (job\_id, student\_id), so a student applies to, and is invited to, a job at most once.
- **Polymorphic references:** interactions.target\_id, notifications.reference\_id and reports.target\_id point to different tables depending on a type column, so the database cannot enforce them. The application must check them.
- **Roles:** users.role is an enum, not a table. role\_permissions repeats the same enum and links to users only by that value.
- **Self-links on users:** posts.verified\_by, reports.resolved\_by and advisor\_students use users more than once under different roles.
