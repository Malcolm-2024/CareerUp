package io.careerup;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Sprint 5 writes reuse the Sprint 3 tables. Identity always comes from the session. */
@RestController
@RequestMapping("/api")
public class WriteController {
  private final NamedParameterJdbcTemplate db;
  public WriteController(NamedParameterJdbcTemplate db) { this.db = db; }
  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
  private static String text(Map<String, String> body, String key, int max, boolean required) {
    String value = Objects.toString(body.get(key), "").strip();
    if ((required && value.isEmpty()) || value.length() > max)
      throw bad(Map.of("title","Title","description","Description","url","Media link","mediaType","Media type","postId","Portfolio post","content","Feedback","location","Location","employmentType","Opportunity type","expectedStatus","Current account status").getOrDefault(key,key)
          + (value.isEmpty() ? " is required." : " is too long (maximum " + max + " characters)."));
    return value;
  }
  private void active(SecurityConfig.Account a) {
    if (!"ACTIVE".equals(db.queryForObject("SELECT status FROM users WHERE id=:id", Map.of("id", a.id), String.class)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is inactive. Please sign out.");
  }
  private long insert(String sql, Map<String, ?> args) {
    var keys = new GeneratedKeyHolder();
    db.update(sql, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(args), keys, new String[]{"id"});
    return Objects.requireNonNull(keys.getKey()).longValue();
  }

  // ADMIN: optimistic update prevents a stale page from overwriting another administrator.
  @PostMapping("/admin/users/{id}/status")
  @Transactional
  Map<String, Object> status(@PathVariable long id, @RequestBody Map<String,String> body,
      @AuthenticationPrincipal SecurityConfig.Account a) {
    active(a);
    String status = text(body,"status",20,true), expected = text(body,"expectedStatus",20,true);
    if (!Set.of("ACTIVE","INACTIVE").contains(status)) throw bad("Choose ACTIVE or INACTIVE.");
    if (status.equals(expected)) throw bad("Choose a different account status.");
    if (id == a.id) throw bad("You cannot change your own account status.");
    int updated = db.update("UPDATE users SET status=:status WHERE id=:id AND status=:expected",
        Map.of("id",id,"status",status,"expected",expected));
    if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Account changed or no longer exists. Refresh and try again.");
    return Map.of("id",id,"status",status);
  }

  // STUDENT: a portfolio post and its media are committed together, never partially.
  @PostMapping("/student/posts")
  @Transactional
  Map<String,Object> post(@RequestBody Map<String,String> body, @AuthenticationPrincipal SecurityConfig.Account a) {
    active(a);
    String title=text(body,"title",255,true), description=text(body,"description",10000,true);
    String url=text(body,"url",2048,true), type=text(body,"mediaType",10,true);
    if (!Set.of("IMAGE","VIDEO").contains(type)) throw bad("Choose image or video.");
    try { URI u=URI.create(url); if (!Set.of("http","https").contains(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null) throw bad("Enter a valid public http(s) media URL."); }
    catch (IllegalArgumentException e) { throw bad("Enter a valid public http(s) media URL."); }
    long id=insert("INSERT INTO posts(author_id,major_id,post_type,title,description,status) SELECT id,major_id,'PORTFOLIO',:title,:description,'PUBLISHED' FROM users WHERE id=:user",
        Map.of("user",a.id,"title",title,"description",description));
    db.update("INSERT INTO post_media(post_id,media_type,url) VALUES(:id,:type,:url)",Map.of("id",id,"type",type,"url",url));
    return Map.of("id",id,"message","Portfolio post published.");
  }

  @GetMapping("/recruiter/jobs")
  List<Map<String,Object>> jobs(@AuthenticationPrincipal SecurityConfig.Account a) {
    active(a);
    return db.queryForList("SELECT id,title,description,location,employment_type,deadline,status FROM jobs WHERE posted_by=:user ORDER BY id DESC LIMIT 100",Map.of("user",a.id));
  }

  @GetMapping("/student/feedback")
  List<Map<String,Object>> received(@AuthenticationPrincipal SecurityConfig.Account a) {
    active(a);
    return db.queryForList("SELECT n.id,n.content,n.post_id,n.created_at,u.name advisor,p.title FROM advisor_notes n JOIN users u ON u.id=n.advisor_id LEFT JOIN posts p ON p.id=n.post_id WHERE n.student_id=:user AND n.kind='FEEDBACK' ORDER BY n.id DESC LIMIT 100",Map.of("user",a.id));
  }

  // RECRUITER: company ownership is taken from the database, not supplied by the browser.
  @PostMapping("/recruiter/jobs")
  @Transactional
  Map<String,Object> job(@RequestBody Map<String,String> body, @AuthenticationPrincipal SecurityConfig.Account a) {
    active(a);
    String title=text(body,"title",255,true),description=text(body,"description",10000,true),location=text(body,"location",255,true);
    String type=text(body,"employmentType",20,true),date=text(body,"deadline",10,false);
    if (!Set.of("FULL_TIME","PART_TIME","INTERNSHIP","CONTRACT","OTHER").contains(type)) throw bad("Choose a valid opportunity type.");
    LocalDate deadline=null;
    if (!date.isEmpty()) { try { deadline=LocalDate.parse(date); } catch(Exception e) { throw bad("Enter a valid deadline."); }
      if (deadline.isBefore(LocalDate.now())) throw bad("Deadline cannot be in the past."); }
    Integer company=db.queryForObject("SELECT company_id FROM users WHERE id=:id",Map.of("id",a.id),Integer.class);
    if (company==null) throw bad("Your account must be linked to a company before posting an opportunity.");
    var args=new HashMap<String,Object>(); args.put("user",a.id);args.put("company",company);args.put("title",title);args.put("description",description);args.put("location",location);args.put("type",type);args.put("deadline",deadline);
    long id=insert("INSERT INTO jobs(company_id,posted_by,title,description,location,employment_type,deadline,status) VALUES(:company,:user,:title,:description,:location,:type,:deadline,'PUBLISHED')",args);
    return Map.of("id",id,"message","Opportunity published.");
  }

  // COMPANY: only a pending request for the representative's company can be decided.
  @PostMapping("/company/requests/{id}/decision")
  @Transactional
  Map<String,Object> decision(@PathVariable long id,@RequestBody Map<String,String> body,@AuthenticationPrincipal SecurityConfig.Account a) {
    active(a); String decision=text(body,"decision",10,true);
    if (!Set.of("APPROVED","REJECTED").contains(decision)) throw bad("Choose APPROVED or REJECTED.");
    int changed=db.update("UPDATE posts SET verification_status=:decision,verified_by=:user,verified_at=CURRENT_TIMESTAMP WHERE id=:id AND company_id=(SELECT company_id FROM users WHERE id=:user) AND verification_status='PENDING' AND post_type='PORTFOLIO' AND status='PUBLISHED'",
        Map.of("id",id,"user",a.id,"decision",decision));
    if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Request is unavailable or has already been reviewed. Refresh the list.");
    return Map.of("id",id,"verification_status",decision);
  }

  private void assigned(long student, long advisor) {
    if(db.queryForObject("SELECT COUNT(*) FROM advisor_students a JOIN users u ON u.id=a.student_id WHERE a.advisor_id=:advisor AND a.student_id=:student AND a.status='ACTIVE' AND u.role='STUDENT' AND u.status='ACTIVE'",Map.of("advisor",advisor,"student",student),Integer.class)!=1)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Student is not assigned or authorized for you.");
  }
  @GetMapping("/advisor/students/{id}/feedback")
  List<Map<String,Object>> feedback(@PathVariable long id,@AuthenticationPrincipal SecurityConfig.Account a) {
    active(a); assigned(id,a.id);
    return db.queryForList("SELECT n.id,n.content,n.post_id,n.created_at,u.name advisor FROM advisor_notes n JOIN users u ON u.id=n.advisor_id WHERE n.student_id=:id AND n.kind='FEEDBACK' AND n.advisor_id=:advisor ORDER BY n.id DESC LIMIT 100",Map.of("id",id,"advisor",a.id));
  }
  // ADVISOR: feedback is tied to an authorized student's actual portfolio post.
  @PostMapping("/advisor/students/{id}/feedback")
  @Transactional
  Map<String,Object> feedback(@PathVariable long id,@RequestBody Map<String,String> body,@AuthenticationPrincipal SecurityConfig.Account a) {
    active(a); assigned(id,a.id); String content=text(body,"content",5000,true);
    long post;
    try { post=Long.parseLong(text(body,"postId",20,true)); } catch(NumberFormatException e){throw bad("Choose a portfolio post.");}
    if(db.queryForObject("SELECT COUNT(*) FROM posts WHERE id=:post AND author_id=:student AND post_type='PORTFOLIO' AND status='PUBLISHED'",Map.of("post",post,"student",id),Integer.class)!=1) throw bad("Choose a published portfolio post belonging to this student.");
    long note=insert("INSERT INTO advisor_notes(advisor_id,student_id,post_id,kind,content) VALUES(:advisor,:student,:post,'FEEDBACK',:content)",Map.of("advisor",a.id,"student",id,"post",post,"content",content));
    return Map.of("id",note,"message","Portfolio feedback saved.");
  }
}
