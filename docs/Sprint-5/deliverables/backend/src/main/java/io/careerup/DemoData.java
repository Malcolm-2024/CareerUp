package io.careerup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional local demo accounts only. Existing accounts and passwords are never reset. */
@Component
@Profile("demo")
public class DemoData implements CommandLineRunner {
  private final JdbcTemplate db;
  private final PasswordEncoder encoder;

  public DemoData(JdbcTemplate db, PasswordEncoder encoder) {
    this.db = db;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    // The marker below makes seeding run once. Editing these examples will NOT update an existing database.
    if (db.queryForObject(
            "SELECT COUNT(*) FROM settings WHERE setting_key='sprint4_demo_v1'", Integer.class)
        > 0) return;
    // 1. DEMO PASSWORD
    String hash = encoder.encode("CareerUp4!");
    db.update(
        "INSERT IGNORE INTO majors(id,name,discipline) VALUES(5,'Computer Information"
            + " Systems','Science & Technology')");
    // 2. DEMO ACCOUNTS
// CHANGE HERE: FIRST-RUN demo accounts only. For an existing account, edit its MySQL row instead.
    account(
        9101,
        "student@careerup.demo",
        hash,
        "STUDENT",
        "Nia Williams",
        null,
        5,
        2026,
        "Turning everyday problems into thoughtful digital experiences. Interested in enterprise"
            + " applications and accessible design.");
    account(
        9102,
        "recruiter@careerup.demo",
        hash,
        "RECRUITER",
        "Sarah Jenkins",
        1,
        null,
        null,
        "Connecting emerging talent with Delta Air Lines.");
    account(
        9103,
        "company@careerup.demo",
        hash,
        "COMPANY_REP",
        "David Miller",
        1,
        null,
        null,
        "University partnerships at Delta Air Lines.");
    account(
        9104,
        "advisor@careerup.demo",
        hash,
        "CAREER_ADVISOR",
        "Dr. Patricia Hayes",
        null,
        null,
        null,
        "Helping students turn their work into career opportunities.");
    account(
        9105,
        "admin@careerup.demo",
        hash,
        "ADMIN",
        "CareerUp Administrator",
        null,
        null,
        null,
        "CareerUp platform administration.");
    account(
        9106,
        "other-company@careerup.demo",
        hash,
        "COMPANY_REP",
        "Truist Representative",
        2,
        null,
        null,
        "Second-company authorization test account.");
    account(
        9107,
        "malik@careerup.demo",
        hash,
        "STUDENT",
        "Malik Harris",
        null,
        1,
        2027,
        "Building useful software with Python and data. Exploring data visualization and hardware"
            + " projects.");
    account(
        9108,
        "inactive@careerup.demo",
        hash,
        "STUDENT",
        "Inactive Demo",
        null,
        1,
        2026,
        "Inactive account for sign-in testing.");
    db.update("UPDATE users SET status='INACTIVE' WHERE id=9108");
    // 3. STUDENT SKILLS
    db.update(
        "INSERT IGNORE INTO student_skills VALUES(9101,2),(9101,3),(9101,4),(9107,1),(9107,4)");
    // 4. PORTFOLIO POSTS AND COMPANY REQUESTS
// CHANGE HERE: FIRST-RUN sample projects only. For existing sample data, edit MySQL.
    post(
        9201,
        9101,
        null,
        5,
        "Campus Companion",
        "A student-first campus app: accessible navigation, event discovery and a thoughtful"
            + " sign-in flow.",
        "NOT_REQUESTED",
        "/media/campus.svg");
    post(
        9202,
        9107,
        null,
        1,
        "Making data tell a story",
        "An interactive Python dashboard that turns student survey results into clear, actionable"
            + " insights.",
        "NOT_REQUESTED",
        "/media/data.svg");
    post(
        9203,
        9101,
        1,
        5,
        "A smoother journey with Delta",
        "A seat-selection concept focused on accessible controls and clearer travel choices."
            + " Student collaboration verification requested.",
        "PENDING",
        "/media/journey.svg");
    post(
        9204,
        102,
        1,
        2,
        "Designing a better boarding pass",
        "An exploration of visual hierarchy and readable boarding information, created for a"
            + " student company collaboration.",
        "APPROVED",
        "/media/boarding.svg");
    post(
        9205,
        9107,
        2,
        1,
        "Fintech insights prototype",
        "Data visualization concept for a student collaboration with Truist. Verification"
            + " requested.",
        "PENDING",
        "/media/data.svg");
    // 5. POST SKILLS
    db.update(
        "INSERT IGNORE INTO post_skills"
            + " VALUES(9201,2),(9201,3),(9202,1),(9202,4),(9203,2),(9204,3),(9205,4)");
    // 6. CAREER ADVISOR STUDENT ASSIGNMENTS
    db.update(
        "INSERT IGNORE INTO advisor_students(advisor_id,student_id,status)"
            + " VALUES(9104,9101,'ACTIVE'),(9104,102,'ACTIVE')");
    // 7. DEMO SEED COMPLETION
    db.update("INSERT INTO settings(setting_key,setting_value) VALUES('sprint4_demo_v1','Loaded')");
  }

  // 8. USER INSERT QUERY
  // Each account call above supplies id, email, hash, role, name, company, major, year and bio in this order.
  private void account(
      int id,
      String email,
      String hash,
      String role,
      String name,
      Integer company,
      Integer major,
      Integer year,
      String bio) {
    db.update(
        "INSERT INTO"
            + " users(id,email,password_hash,role,status,name,company_id,major_id,grad_year,school,bio)"
            + " VALUES(?,?,?,?,'ACTIVE',?,?,?,?,?,?)",
        id,
        email,
        hash,
        role,
        name,
        company,
        major,
        year,
        role.equals("STUDENT") ? "Florida A&M University" : null,
        bio);
  }

  // 9. POST AND MEDIA INSERT QUERIES
  // Each post call supplies the author/company/project details; this helper inserts the post AND its media row.
  private void post(
      int id,
      int author,
      Integer company,
      int major,
      String title,
      String description,
      String status,
      String media) {
    db.update(
        "INSERT INTO"
            + " posts(id,author_id,company_id,major_id,title,description,verification_status,status)"
            + " VALUES(?,?,?,?,?,?,?,'PUBLISHED')",
        id,
        author,
        company,
        major,
        title,
        description,
        status);
    db.update(
        "INSERT INTO post_media(post_id,media_type,url,sort_order) VALUES(?,'IMAGE',?,0)",
        id,
        media);
  }
}
