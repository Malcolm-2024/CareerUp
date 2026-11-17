package io.careerup;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final NamedParameterJdbcTemplate db;

  public ApiController(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  // 1. DATABASE HEALTH
  @GetMapping("/health")
  Map<String, Object> health() {
    return Map.of(
        "status",
        "UP",
        "database",
        db.queryForObject("SELECT 1", Map.of(), Integer.class) == 1 ? "UP" : "DOWN");
  }

  // 2. SIGN-IN TOKEN AND AUTHENTICATED USER
  @GetMapping("/auth/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/auth/me")
  Map<String, Object> me(@AuthenticationPrincipal SecurityConfig.Account a) {
    return db.queryForMap(
        "SELECT u.id,u.name,u.email,u.role,u.company_id,c.name company FROM users u LEFT JOIN"
            + " companies c ON c.id=u.company_id WHERE u.id=:id",
        Map.of("id", a.id));
  }

  // 3. SHARED SEARCH AND PAGINATION
// CHANGE HERE: shared partial-text matching and page size. LIKE is not typo-tolerant fuzzy search.
  // Prepare a literal partial search: % surrounds the text; typed %/_ are escaped rather than treated as wildcards.
  private static String term(String s) {
    return "%"
        + (s == null ? "" : s.strip().substring(0, Math.min(150, s.strip().length())))
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
        + "%";
  }

  // Shared result paging: items are the current page; total counts all matching records. page starts at 0.
  private Map<String, Object> page(
      String select, String from, Map<String, Object> args, int page, int size, String order) {
    page = Math.max(0, page);
    size = Math.max(1, Math.min(50, size));
    var p = new HashMap<>(args);
    p.put("limit", size);
    p.put("offset", (long) page * size);
    return Map.of(
        "items",
        db.queryForList(select + from + order + " LIMIT :limit OFFSET :offset", p),
        "total",
        db.queryForObject("SELECT COUNT(*) " + from, args, Long.class),
        "page",
        page,
        "size",
        size);
  }

  // 4. ADMINISTRATOR USER SEARCH
// CHANGE HERE: which users the admin search returns.
  // Frontend requests /api/admin/users. q searches name/email; role/status are optional filters.
  @GetMapping("/admin/users")
  Map<String, Object> users(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String role,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return page(
        "SELECT id,name,email,role,status,school,grad_year ",
        "FROM users WHERE (name LIKE :q ESCAPE '!' OR email LIKE :q ESCAPE '!') AND (:role='' OR"
            + " role=:role) AND (:status='' OR status=:status)",
        Map.of("q", term(q), "role", role, "status", status),
        page,
        size,
        " ORDER BY name,id");
  }

  // 5. STUDENT SEARCH QUERY
// CHANGE HERE: student filters shared by recruiter/advisor; keep advisor assignment checks.
  private static final String STUDENT_COLUMNS =
      "SELECT u.id,u.name,u.school,u.grad_year,u.bio,m.name major,COALESCE((SELECT"
          + " GROUP_CONCAT(s.name ORDER BY s.name SEPARATOR ', ') FROM student_skills ss JOIN"
          + " skills s ON s.id=ss.skill_id WHERE ss.student_id=u.id),'') skills ";

  // Shared student query: recruiter passes no advisor; advisor passes their own ID to restrict access.
  private Map<String, Object> students(
      String q, String major, String skill, String year, int page, int size, Long advisor) {
    var p = new HashMap<String, Object>();
    p.put("q", term(q));
    p.put("major", major);
    p.put("skill", skill);
    p.put("year", year);
    String scope = "";
    // Only students with an ACTIVE assignment/authorization belong in an advisor's results.
    if (advisor != null) {
      scope =
          " AND EXISTS (SELECT 1 FROM advisor_students a WHERE a.student_id=u.id AND"
              + " a.advisor_id=:advisor AND a.status='ACTIVE')";
      p.put("advisor", advisor);
    }
    return page(
        STUDENT_COLUMNS,
        "FROM users u LEFT JOIN majors m ON m.id=u.major_id WHERE u.role='STUDENT' AND"
            + " u.status='ACTIVE' AND (u.name LIKE :q ESCAPE '!' OR COALESCE(u.bio,'') LIKE :q"
            + " ESCAPE '!') AND (:major='' OR u.major_id=:major) AND (:year='' OR CAST(u.grad_year"
            + " AS CHAR)=:year) AND (:skill='' OR EXISTS (SELECT 1 FROM student_skills ss WHERE"
            + " ss.student_id=u.id AND ss.skill_id=:skill))"
            + scope,
        p,
        page,
        size,
        " ORDER BY u.name,u.id");
  }

  // 6. RECRUITER STUDENT SEARCH
  @GetMapping("/recruiter/students")
  Map<String, Object> recruiter(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String major,
      @RequestParam(defaultValue = "") String skill,
      @RequestParam(defaultValue = "") String year,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return students(q, major, skill, year, page, size, null);
  }

  // 7. CAREER ADVISOR STUDENT SEARCH
  @GetMapping("/advisor/students")
  Map<String, Object> advisor(
      @AuthenticationPrincipal SecurityConfig.Account a,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String major,
      @RequestParam(defaultValue = "") String skill,
      @RequestParam(defaultValue = "") String year,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return students(q, major, skill, year, page, size, a.id);
  }

  // 8. MAJOR SKILL AND GRADUATION FILTER OPTIONS
  // Dropdown choices come from MySQL, not a hardcoded list in the page.
  @GetMapping({"/recruiter/filters", "/advisor/filters"})
  Map<String, Object> filters() {
    return Map.of(
        "majors",
        db.queryForList("SELECT id,name FROM majors WHERE is_active=1 ORDER BY name", Map.of()),
        "skills",
        db.queryForList("SELECT id,name FROM skills WHERE is_active=1 ORDER BY name", Map.of()),
        "years",
        db.queryForList(
            "SELECT DISTINCT grad_year year FROM users WHERE role='STUDENT' AND status='ACTIVE' AND"
                + " grad_year IS NOT NULL ORDER BY grad_year",
            Map.of()));
  }

  // 9. PORTFOLIO POST AND MEDIA QUERIES
  private static final String POST_COLUMNS =
      "SELECT p.id,p.author_id,p.title,p.description,p.verification_status,p.created_at,u.name"
          + " author,u.school,m.name major,c.name company,COALESCE((SELECT GROUP_CONCAT(s.name"
          + " ORDER BY s.name SEPARATOR ', ') FROM post_skills ps JOIN skills s ON s.id=ps.skill_id"
          + " WHERE ps.post_id=p.id),'') skills ";
  private static final String POST_FROM =
      "FROM posts p JOIN users u ON u.id=p.author_id LEFT JOIN majors m ON m.id=p.major_id LEFT"
          + " JOIN companies c ON c.id=p.company_id ";

  private void media(Map<String, Object> result) {
    @SuppressWarnings("unchecked")
    var items = (List<Map<String, Object>>) result.get("items");
    for (var p : items)
      p.put(
          "media",
          db.queryForList(
              "SELECT media_type,url,duration_seconds FROM post_media WHERE post_id=:id ORDER BY"
                  + " sort_order,id",
              Map.of("id", p.get("id"))));
  }

  // 10. STUDENT DISCOVERY FEED
// CHANGE HERE: which posts and fields the student search returns.
  // Discovery search checks the project title/description, student name, major and tagged skills.
  @GetMapping("/student/feed")
  Map<String, Object> feed(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "6") int size) {
    var r =
        page(
            POST_COLUMNS,
            POST_FROM
                + "WHERE p.status='PUBLISHED' AND u.role='STUDENT' AND u.status='ACTIVE' AND"
                + " (p.title LIKE :q ESCAPE '!' OR COALESCE(p.description,'') LIKE :q ESCAPE '!'"
                + " OR u.name LIKE :q ESCAPE '!' OR COALESCE(m.name,'') LIKE :q ESCAPE '!'"
                + " OR EXISTS (SELECT 1 FROM post_skills ps JOIN skills s ON s.id=ps.skill_id"
                + " WHERE ps.post_id=p.id AND s.name LIKE :q ESCAPE '!'))",
            Map.of("q", term(q)),
            page,
            size,
            " ORDER BY p.created_at DESC,p.id DESC");
    media(r);
    return r;
  }

  // 11. COMPANY COLLABORATION AND VERIFICATION REQUESTS
// CHANGE HERE: company request search; keep the signed-in company restriction.
  @GetMapping("/company/requests")
  Map<String, Object> requests(
      @AuthenticationPrincipal SecurityConfig.Account a,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    // Company is obtained from the signed-in database account, never a client-supplied company ID.
    var r =
        page(
            POST_COLUMNS,
            POST_FROM
                + "WHERE p.company_id=(SELECT company_id FROM users WHERE id=:user) AND"
                + " u.role='STUDENT' AND u.status='ACTIVE' AND p.status='PUBLISHED' AND"
                + " p.verification_status IN ('PENDING','APPROVED','REJECTED') AND (:status='' OR"
                + " p.verification_status=:status) AND (u.name LIKE :q ESCAPE '!' OR p.title LIKE"
                + " :q ESCAPE '!')",
            Map.of("user", a.id, "q", term(q), "status", status),
            page,
            size,
            " ORDER BY p.created_at DESC,p.id DESC");
    media(r);
    return r;
  }

  // 12. CAREER ADVISOR PROFILE AND PORTFOLIO
  @GetMapping("/advisor/students/{id}")
  Map<String, Object> profile(
      @PathVariable long id, @AuthenticationPrincipal SecurityConfig.Account a) {
    var access =
        db.queryForObject(
            "SELECT COUNT(*) FROM advisor_students a JOIN users u ON u.id=a.student_id WHERE"
                + " a.advisor_id=:advisor AND a.student_id=:id AND a.status='ACTIVE' AND"
                + " u.role='STUDENT' AND u.status='ACTIVE'",
            Map.of("advisor", a.id, "id", id),
            Integer.class);
    // Check permission BEFORE returning the student profile or portfolio.
    if (access == null || access == 0)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Student is not assigned or authorized for this advisor.");
    var student =
        db.queryForMap(
            STUDENT_COLUMNS + "FROM users u LEFT JOIN majors m ON m.id=u.major_id WHERE u.id=:id",
            Map.of("id", id));
    var portfolio =
        page(
            POST_COLUMNS,
            POST_FROM + "WHERE p.author_id=:id AND p.status='PUBLISHED'",
            Map.of("id", id),
            0,
            50,
            " ORDER BY p.created_at DESC,p.id DESC");
    media(portfolio);
    return Map.of("student", student, "portfolio", portfolio);
  }
}
