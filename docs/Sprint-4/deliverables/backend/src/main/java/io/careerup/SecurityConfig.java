package io.careerup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  // 1. DATABASE USER AND ROLE
  public static class Account extends User {
    public final long id;

    Account(long id, String email, String hash, String role, boolean active) {
      super(
          email,
          hash,
          active,
          true,
          true,
          true,
          User.withUsername(email).password(hash).roles(role).build().getAuthorities());
      this.id = id;
    }
  }

  // 2. PASSWORD HASHING
// FIND HERE: BCrypt password verification. Never replace hashes with plain passwords.
  @Bean
  // BCrypt verifies hashes; passwords are not compared as plain database text.
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  // 3. SIGN-IN DATABASE QUERY
  @Bean
  // This lookup runs during sign-in. ACTIVE accounts are enabled; other statuses cannot sign in.
  UserDetailsService accounts(JdbcTemplate db) {
    return email ->
        db
            .query(
                "SELECT id,email,password_hash,role,status FROM users WHERE email=?",
                (r, n) ->
                    new Account(
                        r.getLong("id"),
                        r.getString("email"),
                        r.getString("password_hash"),
                        r.getString("role"),
                        "ACTIVE".equals(r.getString("status"))),
                email.trim())
            .stream()
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  // 4. ROLE-BASED BACKEND AUTHORIZATION
// CHANGE HERE: which role may call each API route. Frontend menu changes do not grant permission.
  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    // These checks protect the actual data routes even if someone changes a browser URL. hasRole names match users.role.
    http.authorizeHttpRequests(a ->
        a.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/health")
            .permitAll()
            .requestMatchers("/api/admin/**")
            .hasRole("ADMIN")
            .requestMatchers("/api/student/**")
            .hasRole("STUDENT")
            .requestMatchers("/api/recruiter/**")
            .hasRole("RECRUITER")
            .requestMatchers("/api/company/**")
            .hasRole("COMPANY_REP")
            .requestMatchers("/api/advisor/**")
            .hasRole("CAREER_ADVISOR")
            .requestMatchers("/api/**")
            .authenticated()
            .anyRequest()
            .permitAll());
    // 5. VALID AND INVALID SIGN-IN
    // The visible Sign in button POSTs here; success/failure handlers return JSON to React.
    http.formLogin(f ->
        f.loginProcessingUrl("/api/auth/login")
            .usernameParameter("email")
            .successHandler((q, r, a) -> {
              r.setContentType("application/json");
              r.getWriter().write("{\"ok\":true}");
            })
            .failureHandler((q, r, e) -> {
              r.setStatus(401);
              r.setContentType("application/json");
              r.getWriter()
                  .write(
                      "{\"message\":\"Email or password is incorrect, or this account is"
                          + " inactive.\"}");
            }));
    // 6. SIGN-OUT
    // Both visible sign-out buttons call this route; it ends the session and removes its cookie.
    http.logout(l ->
        l.logoutUrl("/api/auth/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((q, r, a) -> r.setStatus(204)));
    // 7. UNAUTHORIZED AND FORBIDDEN ACCESS
// FIND HERE: 401 = sign in; 403 = signed in but wrong permission.
    // 401 means sign-in is needed; 403 means the signed-in role is not permitted.
    http.exceptionHandling(e ->
        e.authenticationEntryPoint(
                (q, r, x) -> {
                  r.setStatus(401);
                  r.setContentType("application/json");
                  r.getWriter().write("{\"message\":\"Please sign in to continue.\"}");
                })
            .accessDeniedHandler((q, r, x) -> {
              r.setStatus(403);
              r.setContentType("application/json");
              r.getWriter().write("{\"message\":\"You do not have permission for this action.\"}");
            }));
    // CSRF stays enabled. The React client obtains a session token before login/logout.
    return http.build();
  }
}
