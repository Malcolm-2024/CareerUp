# CareerUp Database-Related and Application-Enforced Business Rules

## User & Account Rules
* A user email address must be unique across the platform.
* A user can hold only one primary user role (Student, Recruiter, Company Representative, Career Advisor, or Administrator) per account record.
* Employer-related accounts and Career Advisor accounts must complete additional verification or approval before gaining full system access.
* An Administrator account can activate, deactivate, suspend, or ban user accounts based on platform policies.

## Post & Content Rules
* A portfolio post belongs to one student creator.
* A comment must belong to a specific portfolio or company post.
* A portfolio post can have multiple associated standardized skill tags.
* A portfolio post can tag at most one company per submission.
* A portfolio post can display a **Verified Collaboration** indicator only after an authorized Company Representative explicitly approves the verification request.

## Application & Recruitment Rules
* A student can submit multiple job or internship applications.
* An application must include at least one selected portfolio post as a required supporting credential.
* An application must belong to one specific student and one specific job or internship listing.
* An application must maintain a valid status (e.g., Submitted, Under Review, Interview, Rejected, Selected/Offered) at all times.
* A recruiter can save or shortlist multiple student candidates without requiring an active application.

## Company & Representative Rules
* A company profile can be managed by multiple authorized Company Representatives.
* A company representative can manage and approve access only for recruiters officially associated with their specific company.
* A verified collaborative post can appear dually on both the student's personal portfolio and the associated company's official CareerUp page.

## Advisory & Taxonomy Rules
* A career advisor can view and monitor only the profiles and portfolios of students who have explicitly authorized access or been assigned to them.
* Standardized taxonomy categories (such as major, skill tags, academic disciplines, and industries) must be managed centrally by system Administrators.
