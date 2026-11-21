import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Compass,
  Users,
  BriefcaseBusiness,
  MessageSquare,
  Settings,
  ShieldCheck,
  GraduationCap,
  Building2,
  Search,
  ArrowRight,
  ArrowUpRight,
  LogOut,
  ChevronLeft,
  ChevronRight,
  X,
  Check,
  Lock,
  Play,
  Image as ImageIcon,
  LoaderCircle,
  RefreshCw,
} from "lucide-react";
import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/inter/700.css";
import "@fontsource/inter/800.css";
import "./styles.css";
import Sprint5Actions from "./Sprint5Actions.jsx";
import ThemeToggle from "./ThemeToggle.jsx";
import { request } from "./requests.js";

// 1. USER ROLES
const ROLES = {
  STUDENT: "Student",
  RECRUITER: "Recruiter",
  COMPANY_REP: "Company Representative",
  CAREER_ADVISOR: "Career Advisor",
  ADMIN: "Administrator",
};
// 2. ROLE LANDING PAGES AND NAVIGATION
// CHANGE HERE: each role's page heading, description, menu labels and data route.
// title = sidebar label; heading/sub = page wording; endpoint = backend data route.
// future lists the disabled navigation options for each role.
const CONFIG = {
  STUDENT: {
    title: "Discover",
    heading: "Great work deserves to be seen.",
    sub: "Explore projects, ideas and the people building what comes next.",
    endpoint: "/student/feed",
    icon: Compass,
    future: ["My portfolio", "Jobs & applications", "Messages"],
  },
  RECRUITER: {
    title: "Find talent",
    heading: "Meet your next great hire.",
    sub: "Find students by their skills, major and graduation year.",
    endpoint: "/recruiter/students",
    icon: Users,
    future: ["Job postings", "Applications", "Messages", "Company profile"],
  },
  COMPANY_REP: {
    title: "Verification requests",
    heading: "Recognize the work. Build trust.",
    sub: "Student collaboration requests connected to your company.",
    endpoint: "/company/requests",
    icon: ShieldCheck,
    future: ["Company profile", "Partnerships", "Messages"],
  },
  CAREER_ADVISOR: {
    title: "My students",
    heading: "Help their next chapter take shape.",
    sub: "Browse your assigned students and explore their career portfolios.",
    endpoint: "/advisor/students",
    icon: GraduationCap,
    future: ["Recommendations", "Portfolio feedback", "Messages", "Resources"],
  },
  ADMIN: {
    title: "Manage users",
    heading: "A community built on possibility.",
    sub: "Find and review CareerUp accounts across all five roles.",
    endpoint: "/admin/users",
    icon: Users,
    future: ["Companies", "Content moderation", "Reports", "System settings"],
  },
};
// 3. BACKEND CONNECTION AND SIGN-IN TOKEN
let csrf = null;
// Shared request helper: /student/feed becomes /api/student/feed on this same server.
const api = request;
async function token() {
  csrf = await api("/auth/csrf");
  return csrf;
}
// 4. CAREERUP BRANDING AND TAGLINE
// CHANGE HERE: logo/name/tagline shown across the app.
function Brand() {
  return (
    <div className="brand">
      <img src="/logo.png" alt="" />
      <div>
        <strong>
          Career<span>Up</span>
        </strong>
        <small>From Resumes to Reels</small>
      </div>
    </div>
  );
}
// 5. SHARED PROFILE AND DISPLAY COMPONENTS
function Avatar({ name, large = false }) {
  return (
    <span className={"avatar " + (large ? "large" : "")}>
      {(name || "CU")
        .split(" ")
        .map((s) => s[0])
        .slice(0, 2)
        .join("")}
    </span>
  );
}
function Badge({ children }) {
  return <span className="badge">{children}</span>;
}
function Skills({ value }) {
  return (
    <div className="tags">
      {(value || "")
        .split(", ")
        .filter(Boolean)
        .map((s) => (
          <span key={s}>{s}</span>
        ))}
    </div>
  );
}
function ErrorBox({ error, onRetry }) {
  return (
    <div className="error" role="alert">
      <span>{error}</span>
      {onRetry && (
        <button onClick={onRetry}>
          <RefreshCw size={16} />
          Try again
        </button>
      )}
    </div>
  );
}
// 6. SIGN-IN SCREEN AND DEMO ROLE BUTTONS
// CHANGE HERE: login wording, fields and demo-account buttons.
function Login({ onLogin, notice = "" }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  // The Sign in button submits this form, verifies credentials on the backend, then loads the user.
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    if (!email.trim()) {setError("Enter your email address.");return;}
    if (!/^[^\s@]+@[^\s@]+$/.test(email.trim())) {setError("Enter a valid email address.");return;}
    if (!password.trim()) {setError("Enter your password.");return;}
    setBusy(true);
    setError("");
    try {
      const t = await token();
      await api("/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          [t.headerName]: t.token,
        },
        body: new URLSearchParams({ email: email.trim(), password }),
      });
      await token();
      onLogin(await api("/auth/me"));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="login-page">
      <section className="login-story">
        <Brand />
        <div className="story-content">
          <div className="eyebrow">
            <span /> YOUR NEXT CHAPTER STARTS HERE
          </div>
          <h1>
            More than
            <br />a résumé.
            <br />
            <em>Your story.</em>
          </h1>
          <p>
            Show your work. Find your people.
            <br />
            Build a career that feels like you.
          </p>
          <div className="story-cards">
            <img
              src="/media/campus.svg"
              alt="Campus app demo project artwork"
            />
            <img
              src="/media/data.svg"
              alt="Data visualization demo project artwork"
            />
          </div>
        </div>
        <small className="story-footer">Create. Connect. Grow.</small>
      </section>
      <main className="login-main">
        <div className="login-theme-switch"><ThemeToggle /></div>
        <div className="signin-card">
          <div className="eyebrow">WELCOME TO CAREERUP</div>
          {/* SIGN-IN HEADING: edit the visible welcome text here. */}
          <h2>Hay.</h2>
          <p className="muted">Sign in to find your next opportunity.</p>
          <form onSubmit={submit} noValidate>
            {notice && <p className="session-notice" role="status">{notice}</p>}
            <label>
              Email address
              <input
                required
                type="email"
                autoComplete="username"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <label>
              Password
              <input
                required
                type="password"
                autoComplete="current-password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </label>
            {error && <ErrorBox error={error} />}
            {/* SIGN IN BUTTON: primary/login-submit control its CSS; disabled={busy} prevents repeated clicks. */}
            <button className="primary login-submit" disabled={busy}>
              {busy ? (
                <LoaderCircle className="spin" size={18} />
              ) : (
                <>
                  Sign in <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
          <section className="demo">
            <h3>Try a demo account</h3>
            <p>Choose a role to fill in its credentials, then sign in.</p>
            {/* DEMO ROLE BUTTONS: one reusable button is created for each of the five labels below. */}
            <div className="demo-roles">
              {[
                ["student", "Student"],
                ["recruiter", "Recruiter"],
                ["company", "Company Rep"],
                ["advisor", "Career Advisor"],
                ["admin", "Administrator"],
              ].map(([key, label]) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => {
                    setEmail(key + "@careerup.demo");
                    setPassword("CareerUp4!");
                    setError("");
                  }}
                >
                  {label}
                </button>
              ))}
            </div>
            <small>
              Demo password: <code>CareerUp4!</code>
            </small>
          </section>
        </div>
        <footer>CareerUp · From Resumes to Reels</footer>
      </main>
    </div>
  );
}
// 7. PORTFOLIO MEDIA
function Media({ post, compact = false }) {
  const [failed, setFailed] = useState(false);
  const m = post.media?.[0];
  const sampleVideo = m?.media_type === "VIDEO" && m?.url?.includes("media.careerup.io");
  const safe = m?.url?.startsWith("/media/") || m?.url?.startsWith("/uploads/") || /^https?:\/\//.test(m?.url || "");
  if (!m || !safe || failed || m.url.includes("media.careerup.io"))
    return (
      <div className="media-fallback">
        <ImageIcon size={30} />
        <span>{sampleVideo?"Demo video not included":m?.media_type === "VIDEO" ? "Video preview unavailable" : "Project preview unavailable"}</span>
        <small>{sampleVideo?"This earlier sprint sample uses a placeholder video link. Upload your own MP4 or WebM to show a real project video.":m?.media_type === "VIDEO" ? "Try an MP4 with H.264 video or a WebM file. You can still read the project below." : "The picture may be unavailable. You can still read the project below."}</small>
      </div>
    );
  return (
    <div className={"project-media " + (compact ? "compact" : "")}>
      {m.media_type === "VIDEO" ? (
        <video
          controls
          playsInline
          preload="metadata"
          src={m.url}
          onError={() => setFailed(true)}
          aria-label={post.title}
        />
      ) : (
        <img
          src={m.url}
          alt={post.title + " — project artwork"}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      )}
    </div>
  );
}
// 8. STUDENT PORTFOLIO POST
function Post({ post, compact = false }) {
  return (
    <article id={`portfolio-post-${post.id}`} className={"post " + (compact ? "compact-post" : "")}>
      <div className="post-author">
        <Avatar name={post.author} />
        <div>
          <strong>{post.author}</strong>
          <small>{post.major || post.school || "Student portfolio"}</small>
        </div>
        {post.verification_status === "APPROVED" && (
          <Badge>
            <ShieldCheck size={13} />
            Verified collaboration
          </Badge>
        )}
      </div>
      <Media post={post} compact={compact} />
      <div className="post-body">
        <div className="eyebrow">STUDENT PORTFOLIO</div>
        <h3>{post.title}</h3>
        <p>{post.description}</p>
        <Skills value={post.skills} />
        {post.company && (
          <small className="company-label">
            <Building2 size={14} />
            {post.company}
          </small>
        )}
      </div>
    </article>
  );
}
// 9. SIGNED-IN USER AND ROLE-BASED NAVIGATION
// CHANGE HERE: sign-out, sidebar and wrong-role page display.
function App() {
  const [user, setUser] = useState(null);
  const [boot, setBoot] = useState(true);
  const [bootError, setBootError] = useState("");
  const [sessionNotice, setSessionNotice] = useState("");
  const [route, setRoute] = useState(location.hash.slice(1));
  async function restore() {
    setBootError("");
    setBoot(true);
    try {
      setUser(await api("/auth/me"));
    } catch (e) {
      if (e.status !== 401)
        setBootError(
          "CareerUp could not connect. Check that the server is running, then retry.",
        );
    } finally {
      setBoot(false);
    }
  }
  useEffect(() => {
    restore();
    const handle = () => setRoute(location.hash.slice(1));
    window.addEventListener("hashchange", handle);
    return () => window.removeEventListener("hashchange", handle);
  }, []);
  async function logout() {
    try {
      const t = await token();
      await api("/auth/logout", {
        method: "POST",
        headers: { [t.headerName]: t.token },
      });
      setSessionNotice("");
      setUser(null);
      location.hash = "";
    } catch (e) {
      alert(e.message);
    }
  }
  if (boot)
    return (
      <div className="boot">
        <LoaderCircle className="spin" />
        <p>Opening CareerUp…</p>
      </div>
    );
  if (bootError)
    return (
      <div className="boot">
        <Brand />
        <ErrorBox error={bootError} onRetry={restore} />
      </div>
    );
  if (!user)
    return (
      <Login
        notice={sessionNotice}
        onLogin={(u) => {
          setSessionNotice("");
          setUser(u);
          location.hash = CONFIG[u.role] ? u.role : "";
        }}
      />
    );
  const c = CONFIG[user.role];
  if (!c)
    return (
      <div className="boot">
        <p>This account does not have a supported role.</p>
        <button onClick={logout}>Sign out</button>
      </div>
    );
  const allowed = !route || route === user.role;
  return (
    <div className="app">
      <aside className="sidebar">
        <Brand />
        <div className="workspace-label">{ROLES[user.role]} workspace</div>
        <nav aria-label="Main navigation">
          <a
            href={"#" + user.role}
            className={allowed ? "nav-item active" : "nav-item"}
          >
            <c.icon size={20} />
            {c.title}
            <span className="nav-dot" />
          </a>
          <a className="nav-item" href="#sprint5-actions" onClick={e=>{e.preventDefault();window.dispatchEvent(new Event("careerup:open-action"));document.getElementById("sprint5-actions")?.scrollIntoView({behavior:"smooth"});}}><ArrowUpRight size={19} />{({STUDENT:"Create portfolio post",RECRUITER:"Create opportunity",ADMIN:"Account status",COMPANY_REP:"Review verification",CAREER_ADVISOR:"Give feedback"})[user.role]}</a>
          {/* FUTURE NAVIGATION: these buttons are intentionally disabled for this sprint. */}
          {c.future.map((title, i) => (
            <button
              key={title}
              className="nav-item future"
              disabled
              title="Planned for a later sprint"
            >
              <span>
                {i === 0 ? (
                  <BriefcaseBusiness size={19} />
                ) : i === 1 ? (
                  <Compass size={19} />
                ) : (
                  <MessageSquare size={19} />
                )}
              </span>
              {title}
              <Lock size={12} />
            </button>
          ))}
        </nav>
        <div className="later-note">
          Locked options are planned for a later sprint.
        </div>
        <div className="sidebar-bottom">
          <div className="mini-brand">
            <span>↑</span>
            <p>
              Your work.
              <br />
              <strong>Your way forward.</strong>
            </p>
          </div>
          <div className="account">
            <Avatar name={user.name} />
            <div>
              <strong>{user.name}</strong>
              <small>{ROLES[user.role]}</small>
            </div>
          </div>
          {/* SIDEBAR SIGN OUT: calls logout; the top-bar switch-account button calls the same function. */}
          <button onClick={logout} className="signout">
            <LogOut size={17} />
            Sign out
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            CareerUp <span>/</span> <strong>{c.title}</strong>
          </div>
          <div className="account-actions">
            <ThemeToggle />
            <span className="role-pill">
              <c.icon size={15} />
              {ROLES[user.role]}
            </span>
            {/* TOP-BAR SIGN OUT / SWITCH ACCOUNT: returns to the sign-in screen. */}
            <button className="switch-account" onClick={logout}>
              <LogOut size={16} />
              Sign out / Switch account
            </button>
          </div>
        </header>
        <main className="page">
          {allowed ? (
            <Workspace
              key={user.id + user.role}
              user={user}
              config={c}
              onExpired={(reason) => {setSessionNotice(reason || "Your session has ended. Please sign in again.");setUser(null);}}
            />
          ) : (
            <section className="no-access">
              <ShieldCheck size={38} />
              <div className="eyebrow">403 · ACCESS DENIED</div>
              <h1>This page isn’t available for your role.</h1>
              <p>
                You are signed in as {ROLES[user.role]}.
                {ROLES[route]
                  ? ` The ${ROLES[route]} page requires a different role.`
                  : " Return to your own workspace."}
              </p>
              <a className="primary" href={"#" + user.role}>
                Back to {c.title}
              </a>
            </section>
          )}
        </main>
        <footer className="app-footer">
          CareerUp <span>From Resumes to Reels</span>
        </footer>
      </div>
    </div>
  );
}
// 10. ROLE RETRIEVAL SCREENS AND SEARCH FILTERS
// CHANGE HERE: visible search fields, filters and results. SQL changes belong in ApiController.java.
function Workspace({ user, config: c, onExpired }) {
  const [query, setQuery] = useState("");
  const [filters, setFilters] = useState({
    q: "",
    major: "",
    skill: "",
    year: "",
    role: "",
    status: "",
    postId: "",
  });
  const [options, setOptions] = useState({ majors: [], skills: [], years: [] });
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [actionTarget, setActionTarget] = useState(null);
  const [reviewSaving, setReviewSaving] = useState(false);
  const [selected, setSelected] = useState(null);
  const [profile, setProfile] = useState(null);
  const [profileError, setProfileError] = useState("");
  const isStudents = ["RECRUITER", "CAREER_ADVISOR"].includes(user.role),
    isFeed = user.role === "STUDENT",
    isAdmin = user.role === "ADMIN",
    isCompany = user.role === "COMPANY_REP";
  // VIEW SAVED POST: filter by the exact database ID, even when titles are identical.
  useEffect(()=>{
    if(!isFeed||!filters.postId||busy||!data)return;
    document.getElementById(`portfolio-post-${filters.postId}`)?.scrollIntoView({behavior:"smooth",block:"start"});
  },[data,busy,filters.postId,isFeed]);
  useEffect(() => {
    if (isStudents)
      api(c.endpoint.replace("/students", "/filters"))
        .then(setOptions)
        .catch((e) => setError(e.message));
  }, [c.endpoint, isStudents]);
  useEffect(() => {
    let current = true;
    setBusy(true);
    setError("");
    api(
      c.endpoint +
        "?" +
        new URLSearchParams({ ...filters, page, size: isFeed ? 4 : 9 }),
    )
      .then((d) => {
        if (current) setData(d);
      })
      .catch((e) => {
        if (current) {
          setError(e.message);
          if (e.status === 401) onExpired();
        }
      })
      .finally(() => {
        if (current) setBusy(false);
      });
    return () => {
      current = false;
    };
  }, [filters, page, retry, c.endpoint]);
  useEffect(() => {
    if (selected === null) return;
    let current = true;
    setProfile(null);
    setProfileError("");
    api("/advisor/students/" + selected)
      .then((d) => {
        if (current) setProfile(d);
      })
      .catch((e) => {
        if (current) setProfileError(e.message);
      });
    return () => {
      current = false;
    };
  }, [selected]);
  useEffect(() => {
    if (selected === null) return;
    const old = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const handle = (e) => {
      if (e.key === "Escape") setSelected(null);
    };
    document.addEventListener("keydown", handle);
    return () => {
      document.body.style.overflow = old;
      document.removeEventListener("keydown", handle);
    };
  }, [selected]);
  const change = (key, value) => {
    setFilters((f) => ({ ...f, postId:"", [key]: value }));
    setPage(0);
  };
  const reset = () => {
    setQuery("");
    setFilters({ q: "", major: "", skill: "", year: "", role: "", status: "", postId:"" });
    setPage(0);
  };
  return (
    <>
      <section className="page-heading">
        <div>
          <div className="eyebrow">
            {isCompany
              ? user.company || "YOUR COMPANY"
              : isFeed
                ? "SHOW YOUR WORK. BUILD YOUR CAREER."
                : ROLES[user.role].toUpperCase() + " PORTAL"}
          </div>
          <h1>{c.heading}</h1>
          <p>{c.sub}</p>
        </div>
        <div className="heading-symbol">
          <c.icon size={32} />
        </div>
      </section>
      <Sprint5Actions user={user} api={api} token={token} actionTarget={actionTarget} onBusyChange={setReviewSaving} onSaved={() => setRetry(r => r + 1)} onExpired={onExpired}
        onViewPost={id=>{setQuery("");setFilters({q:"",postId:String(id)});setPage(0);setRetry(r=>r+1);}} />
      {isFeed&&filters.postId&&<div className="saved-post-notice" role="status">Showing your published post. <button type="button" onClick={reset}>Show all projects</button></div>}
      <section className="filters" aria-label="Search and filters">
        <form
          className="search-form"
          onSubmit={(e) => {
            e.preventDefault();
            change("q", query);
          }}
        >
          <Search size={19} />
          <input
            aria-label={
              isAdmin
                ? "Search users"
                : isFeed
                  ? "Search projects"
                  : "Search students or projects"
            }
            placeholder={
              isAdmin
                ? "Search by name or email…"
                : isFeed
                  ? "Search projects, students, majors or skills…"
                  : isCompany
                    ? "Search student or project…"
                    : "Search student names or bios…"
            }
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          {/* SEARCH BUTTON: submits the text box above; the backend performs the database search. */}
          <button type="submit">Search</button>
        </form>
        {isStudents && (
          <>
            <select
              aria-label="Major"
              value={filters.major}
              onChange={(e) => change("major", e.target.value)}
            >
              <option value="">All majors</option>
              {options.majors.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.name}
                </option>
              ))}
            </select>
            <select
              aria-label="Skill"
              value={filters.skill}
              onChange={(e) => change("skill", e.target.value)}
            >
              <option value="">All skills</option>
              {options.skills.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.name}
                </option>
              ))}
            </select>
            <select
              aria-label="Graduation year"
              value={filters.year}
              onChange={(e) => change("year", e.target.value)}
            >
              <option value="">All graduation years</option>
              {options.years.map((x) => (
                <option key={x.year} value={x.year}>
                  {x.year}
                </option>
              ))}
            </select>
          </>
        )}
        {isAdmin && (
          <>
            <select
              aria-label="Role"
              value={filters.role}
              onChange={(e) => change("role", e.target.value)}
            >
              <option value="">All roles</option>
              {Object.entries(ROLES).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </select>
            <select
              aria-label="Account status"
              value={filters.status}
              onChange={(e) => change("status", e.target.value)}
            >
              <option value="">All statuses</option>
              {["ACTIVE", "PENDING", "SUSPENDED", "BANNED", "INACTIVE"].map(
                (x) => (
                  <option key={x}>{x}</option>
                ),
              )}
            </select>
          </>
        )}
        {isCompany && (
          <select
            aria-label="Verification status"
            value={filters.status}
            onChange={(e) => change("status", e.target.value)}
          >
            <option value="">All requests</option>
            {["PENDING", "APPROVED", "REJECTED"].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        )}
        {/* RESET BUTTON: clears this page's search/filter selections. */}
        <button className="reset" onClick={reset}>
          Reset
        </button>
      </section>
      {error && (
        <ErrorBox error={error} onRetry={() => setRetry((r) => r + 1)} />
      )}
      <div className="section-line">
        <h2>
          {isFeed
            ? "The discovery feed"
            : isAdmin
              ? "Community directory"
              : isCompany
                ? "Student collaborations"
                : "Student directory"}{" "}
          {!busy && data && <span className="count">{data.total}</span>}
        </h2>
        <small>
          {isStudents && user.role === "CAREER_ADVISOR"
            ? "Assigned & authorized students"
            : isCompany
              ? "Only " + (user.company || "your company") + " requests"
              : "Explore real work. Make meaningful connections."}
        </small>
      </div>
      {busy ? (
        <div className="loading" role="status">
          <LoaderCircle className="spin" />
          Loading {c.title.toLowerCase()}…
        </div>
      ) : data?.items.length === 0 ? (
        <div className="empty">
          <Search size={32} />
          <h3>No results found.</h3>
          <p>
            {isFeed
              ? "No published student projects match this search. Try Campus, Nia, Python or Figma, or show all projects."
              : "Try a different search or reset the filters."}
          </p>
          <button onClick={reset}>
            {isFeed ? "Show all projects" : "Reset filters"}
          </button>
        </div>
      ) : (
        data && (
          <div className={isFeed ? "feed-layout" : ""}>
            {isFeed ? (
              <>
                <div className="feed">
                  {/* 11. STUDENT DISCOVERY FEED */}
                  {data.items.map((p) => (
                    <Post key={p.id} post={p} />
                  ))}
                </div>
                <aside className="feed-aside">
                  <Badge>
                    <Compass size={15} />
                    Made to be discovered
                  </Badge>
                  <h2>
                    Let your work
                    <br />
                    do the talking.
                  </h2>
                  <p>
                    Discover the projects, ideas and skills that make this
                    community unique.
                  </p>
                  <div className="rule" />
                  <h3>A portfolio, beyond paper.</h3>
                  <p>
                    Explore student-created projects and company-verified
                    collaborations.
                  </p>
                  <div className="color-stripe" />
                  <small>
                    Demo project artwork is sample content for this sprint.
                  </small>
                </aside>
              </>
            ) : isAdmin ? (
              <div className="table-wrap">
                {/* 12. ADMINISTRATOR USER DIRECTORY */}
                <table>
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>School / graduation</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((u) => (
                      <tr key={u.id}>
                        <td>
                          <div className="table-user">
                            <Avatar name={u.name} />
                            <div>
                              <strong>{u.name}</strong>
                              <small>{u.email}</small>
                            </div>
                          </div>
                        </td>
                        <td>{ROLES[u.role] || u.role}</td>
                        <td>
                          <span
                            className={
                              "status " +
                              (u.status === "ACTIVE" ? "positive" : "")
                            }
                          >
                            {u.status}
                          </span>
                        </td>
                        <td>
                          {u.school || "—"}
                          {u.grad_year && <small>Class of {u.grad_year}</small>}
                        </td>
                        <td>{Number(u.id)!==Number(user.id)&&<button type="button" className="profile-button" disabled={reviewSaving}
                          aria-label={`Change status for ${u.name} (${u.email})`}
                          onClick={()=>setActionTarget({role:"ADMIN",record:u})}>Change status</button>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : isCompany ? (
              <div className="request-list">
                {/* 13. COMPANY VERIFICATION REQUESTS */}
                {data.items.map((p) => (
                  <article className="request-card" key={p.id}>
                    <div className="request-top">
                      <Avatar name={p.author} />
                      <div>
                        <h3>{p.author}</h3>
                        <small>
                          {p.major || "Student"} · {p.company}
                        </small>
                      </div>
                      <span
                        className={
                          "status " +
                          (p.verification_status === "APPROVED"
                            ? "positive"
                            : "")
                        }
                      >
                        {p.verification_status}
                      </span>
                    </div>
                    <h3>{p.title}</h3>
                    <p>{p.description}</p>
                    <Skills value={p.skills} />
                    {/* REVIEW THIS REQUEST: reuse the existing form and select this exact card. */}
                    {p.verification_status === "PENDING" && (
                      <button className="profile-button" disabled={reviewSaving}
                        aria-label={`Review request: ${p.title} by ${p.author}`}
                        onClick={() => setActionTarget({role:"COMPANY_REP",record:p})}>
                        Review request <ArrowUpRight size={17} />
                      </button>
                    )}
                    <details>
                      <summary>
                        View portfolio project <ArrowUpRight size={15} />
                      </summary>
                      <Media post={p} compact />
                    </details>
                  </article>
                ))}
              </div>
            ) : (
              <div className="student-grid">
                {/* 14. RECRUITER AND CAREER ADVISOR STUDENT DIRECTORY */}
                {data.items.map((s) => (
                  <article key={s.id} className="student-card">
                    <div className="student-top">
                      <Avatar name={s.name} large />
                      <span className="class-year">
                        {s.grad_year
                          ? "Class of " + s.grad_year
                          : "Year not listed"}
                      </span>
                    </div>
                    <h3>{s.name}</h3>
                    <strong className="major-name">
                      {s.major || "Major not listed"}
                    </strong>
                    <p className="school">{s.school || "School not listed"}</p>
                    <p>{s.bio || "This student has not added a bio yet."}</p>
                    <Skills value={s.skills} />
                    {user.role === "CAREER_ADVISOR" && (
                      /* VIEW CAREER PROFILE: selects this student; the profile request loads their data below. */
                      <button
                        className="profile-button"
                        onClick={() => setSelected(s.id)}
                      >
                        View career profile <ArrowUpRight size={17} />
                      </button>
                    )}
                  </article>
                ))}
              </div>
            )}
          </div>
        )
      )}
      {data && data.total > data.size && (
        <div className="pagination">
          {/* 15. RESULT PAGINATION */}
          <span>
            {page * data.size + 1}–
            {Math.min((page + 1) * data.size, data.total)} of {data.total}
          </span>
          {/* PREVIOUS PAGE: disabled while loading or already on the first page. */}
          <button
            aria-label="Previous page"
            disabled={busy || page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={18} />
            Previous
          </button>
          {/* NEXT PAGE: disabled while loading or after the last result page. */}
          <button
            aria-label="Next page"
            disabled={busy || (page + 1) * data.size >= data.total}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
            <ChevronRight size={18} />
          </button>
        </div>
      )}
      {selected !== null && (
        <div
          className="modal-shade"
          onClick={(e) => {
            if (e.target === e.currentTarget) setSelected(null);
          }}
        >
          {/* 16. CAREER ADVISOR PROFILE AND PORTFOLIO */}
          <dialog
            open
            aria-label="Student career profile"
            className="profile-dialog"
          >
            {/* CLOSE PROFILE: dismisses the advisor profile dialog; it does not delete student data. */}
            <button
              autoFocus
              className="close"
              aria-label="Close profile"
              onClick={() => setSelected(null)}
            >
              <X />
            </button>
            {profileError ? (
              <ErrorBox error={profileError} />
            ) : !profile ? (
              <div className="loading">
                <LoaderCircle className="spin" />
                Loading student profile…
              </div>
            ) : (
              <>
                <div className="profile-hero">
                  <Avatar name={profile.student.name} large />
                  <div>
                    <div className="eyebrow">CAREER PROFILE</div>
                    <h2>{profile.student.name}</h2>
                    <p>
                      {profile.student.major || "Student"} ·{" "}
                      {profile.student.school || "School not listed"}
                    </p>
                    <small>
                      {profile.student.grad_year
                        ? "Class of " + profile.student.grad_year
                        : "Graduation year not listed"}
                    </small>
                  </div>
                </div>
                <p>{profile.student.bio || "No bio added yet."}</p>
                <h3>Skills</h3>
                <Skills value={profile.student.skills} />
                <h3>
                  Portfolio{" "}
                  <span className="count">{profile.portfolio.total}</span>
                </h3>
                {profile.portfolio.total === 0 ? (
                  <p>No published projects yet.</p>
                ) : (
                  <div className="portfolio-grid">
                    {profile.portfolio.items.map((p) => (
                      <div className="portfolio-item" key={p.id}>
                        <Post compact post={p} />
                        {/* GIVE FEEDBACK HERE: select both this student and this exact portfolio post. */}
                        <button type="button" className="profile-button" disabled={reviewSaving}
                          aria-label={`Give feedback on ${p.title} by ${profile.student.name}`}
                          onClick={()=>{setSelected(null);setActionTarget({role:"CAREER_ADVISOR",record:profile.student,postId:p.id});}}>Give feedback <ArrowUpRight size={17}/></button>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </dialog>
        </div>
      )}
    </>
  );
}
createRoot(document.getElementById("root")).render(<App />);
