package io.careerup;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Active-session enforcement and durable, atomic retry protection for Sprint 5 writes. */
public class WriteSafetyFilter extends OncePerRequestFilter {
  private final JdbcTemplate db;
  private final TransactionTemplate transaction;
  public WriteSafetyFilter(JdbcTemplate db, PlatformTransactionManager manager) {
    this.db=db; this.transaction=new TransactionTemplate(manager);
  }
  private static void error(HttpServletResponse response,int status,String message) throws IOException {
    response.setStatus(status);response.setContentType("application/json");
    response.getWriter().write("{\"message\":\""+message+"\"}");
  }
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
      throws ServletException,IOException {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth==null || !(auth.getPrincipal() instanceof SecurityConfig.Account account)) {chain.doFilter(request,response);return;}
    // 1. ACTIVE ACCOUNT / DATABASE CONNECTION — covers uploaded media as well as API data.
    if((request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/uploads/")) && !request.getRequestURI().startsWith("/api/auth/")) {
      String state;
      try { state=db.queryForObject("SELECT status FROM users WHERE id=?",String.class,account.id); }
      catch(org.springframework.dao.DataAccessException unavailable) {error(response,503,"CareerUp cannot reach its database right now. Keep the application running and try again.");return;}
      if(!"ACTIVE".equals(state)) {request.getSession().invalidate();error(response,401,"Your account is inactive. Please sign in again.");return;}
    }
    String path=request.getRequestURI();
    boolean write="POST".equals(request.getMethod()) && (path.matches("/api/admin/users/\\d+/status") || path.equals("/api/student/posts") || path.equals("/api/recruiter/jobs") || path.matches("/api/company/requests/\\d+/decision") || path.matches("/api/advisor/students/\\d+/feedback"));
    if(!write) {chain.doFilter(request,response);return;}
    String key=request.getHeader("Idempotency-Key");
    if(key==null || !key.matches("[a-zA-Z0-9-]{16,80}")) {error(response,400,"A valid submission key is required. Refresh and try again.");return;}
    // 2. JSON SUBMISSIONS ONLY — file uploads stream to disk in MediaController, not into this buffer.
    byte[] body=request.getInputStream().readNBytes(65537);
    if(body.length>65536) {error(response,413,"Submission is too large.");return;}
    String hash;
    try {hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));}
    catch(Exception e){throw new ServletException(e);}
    var replayable=new HttpServletRequestWrapper(request) {
      @Override public ServletInputStream getInputStream() {
        var input=new ByteArrayInputStream(body);
        return new ServletInputStream() {
          public int read(){return input.read();}
          public boolean isFinished(){return input.available()==0;}
          public boolean isReady(){return true;}
          public void setReadListener(ReadListener listener){}
        };
      }
      @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
    };
    var buffered=new ContentCachingResponseWrapper(response);
    try {
      transaction.executeWithoutResult(status -> {
        try {
          try {db.update("INSERT INTO submission_keys(user_id,request_key,path,body_hash) VALUES(?,?,?,?)",account.id,key,path,hash);}
          catch(DuplicateKeyException duplicate) {
            var old=db.queryForMap("SELECT path,body_hash,response_body,response_status FROM submission_keys WHERE user_id=? AND request_key=?",account.id,key);
            if(!path.equals(old.get("path")) || !hash.equals(old.get("body_hash"))) {error(buffered,409,"This submission key was used for different data. Start a new submission.");return;}
            buffered.setStatus(((Number)old.get("response_status")).intValue());buffered.setContentType("application/json");buffered.getWriter().write((String)old.get("response_body"));return;
          }
          chain.doFilter(replayable,buffered);
          if(buffered.getStatus()>=400) {status.setRollbackOnly();return;}
          buffered.getWriter().flush();
          db.update("UPDATE submission_keys SET response_body=?,response_status=? WHERE user_id=? AND request_key=?",new String(buffered.getContentAsByteArray(),StandardCharsets.UTF_8),buffered.getStatus(),account.id,key);
        } catch(IOException|ServletException e){throw new RuntimeException(e);}
      });
    } catch(RuntimeException failure) {
      // Never send a cached success body if the transaction or commit failed.
      buffered.resetBuffer();
      error(buffered,503,"Unable to confirm this save. Retry the same submission safely.");
      org.slf4j.LoggerFactory.getLogger(WriteSafetyFilter.class).warn("Sprint 5 submission transaction failed",failure);
    } finally {buffered.copyBodyToResponse();}
  }
}
