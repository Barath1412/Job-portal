# PROJECT_STATE.md — Job Portal Audit
> Generated: 2026-07-24 | Stack: Spring Boot 3.2 / Java 21 / H2 in-memory / plain HTML+Tailwind CDN
> **Methodology**: every claim below is verified by reading source files directly. README is ignored as a source of truth.

---

## 1. Backend Reality

### 1.1 Controllers & Endpoints

There are exactly **2 `@RestController` classes**. There are **zero `@Service` classes** — controllers call repositories directly.

---

#### `JobController`
**File:** `src/main/java/com/jobportal/controllers/JobController.java`

| Method | Path | Request Body / Params | Response | Calls |
|--------|------|-----------------------|----------|-------|
| `GET` | `/api/jobs` | — | `List<Job>` | `jobRepository.findAll()` |
| `POST` | `/api/jobs` | `Job` JSON body | `Job` | `jobRepository.save(job)` |
| `GET` | `/api/jobs/{id}` | path var `id` (String) | `Job` or `null` | `jobRepository.findById(id)` |
| `PUT` | `/api/jobs/{id}` | path var + `Job` JSON body | `Job` or `null` | `jobRepository.findById` -> `jobRepository.save` |
| `DELETE` | `/api/jobs/{id}` | path var `id` | `void` | `jobRepository.deleteById(id)` |
| `GET` | `/api/jobs/search` | `?title=&location=` (both optional) | `List<Job>` | `findByTitleContaining` / `findByLocationContaining` / `findByTitleContainingAndLocationContaining` |

**Type mismatch bug:** `Job.id` is declared `Long` on the JPA `@Id` field, but the getter/setter signatures say `String`. The controller path variable also receives a `String` then passes it to a `JpaRepository<Job, Long>` whose `findById` expects a `Long`. This causes a runtime `ClassCastException` or serialisation error on every `GET /api/jobs/{id}`, `PUT`, and `DELETE` call. (`JobController.java` lines 28-48, `Job.java` lines 13, 32-38)

---

#### `ApplicationController`
**File:** `src/main/java/com/jobportal/controllers/ApplicationController.java`

| Method | Path | Request Body / Params | Response | Calls |
|--------|------|-----------------------|----------|-------|
| `GET` | `/api/applications` | — | `List<Application>` | `applicationRepository.findAll()` |
| `POST` | `/api/applications` | `Application` JSON body | `Application` | `applicationRepository.save(application)` |
| `GET` | `/api/applications/{id}` | path var `id` | `Application` or `null` | `applicationRepository.findById(id)` |
| `DELETE` | `/api/applications/{id}` | path var `id` | `void` | `applicationRepository.deleteById(id)` |
| `GET` | `/api/applications/job/{jobId}` | path var `jobId` | `List<Application>` | `applicationRepository.findByJobId(jobId)` |

**Critical bug — `findByJobId` is never declared in `ApplicationRepository`:** The interface only has the JPA parent methods plus a commented-out example. `ApplicationController.java` line 39 calls `applicationRepository.findByJobId(jobId)`, but `ApplicationRepository.java` lines 9-10 show this method is commented out. Spring Data JPA will fail to start or throw a `NoSuchMethodError` at runtime. (`ApplicationController.java` line 39, `ApplicationRepository.java` line 10)

---

### 1.2 Entities / Models

There are exactly **2 `@Entity` classes**. There are **no JPA relationship annotations** (`@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne`) anywhere in the codebase.

#### `Job`
**File:** `src/main/java/com/jobportal/models/Job.java`

| Field | Java type | JPA annotation |
|-------|-----------|----------------|
| `id` | `Long` (field) / `String` (getter — mismatch) | `@Id @GeneratedValue(IDENTITY)` |
| `title` | `String` | plain column |
| `company` | `String` | plain column |
| `location` | `String` | plain column |
| `description` | `String` | plain column |
| `deadline` | `LocalDate` | plain column |

No `salary`, `jobType`, `skills`, `postedBy` (recruiter FK), `status`, or `category` fields exist. No relationship to `Application`.

#### `Application`
**File:** `src/main/java/com/jobportal/models/Application.java`

| Field | Java type | JPA annotation |
|-------|-----------|----------------|
| `id` | `Long` | `@Id @GeneratedValue(IDENTITY)` |
| `jobId` | `Long` | plain column, NOT a FK `@ManyToOne` |
| `applicantName` | `String` | plain column |
| `email` | `String` | plain column |
| `resumeUrl` | `String` | plain column — stores a URL string; no file upload mechanism exists |
| `coverLetter` | `String` | plain column |

No `status` field (Pending/Reviewed/Accepted/Rejected). No `@ManyToOne Job` relationship — `jobId` is a raw `Long` with no referential integrity. No `User`/candidate FK. The constructor signature accepts `String jobId` but the field type is `Long` — another type mismatch. (`Application.java` lines 12, 22)

#### Relationship reality
- `Application.jobId` is a manual FK column, not a JPA relationship — it is **never queried via a join**.
- There is no `User`, `Recruiter`, `Candidate`, or `Company` entity anywhere in the codebase.

---

### 1.3 Services

**There are zero `@Service` classes.** Both controllers `@Autowired` their respective repositories directly. There is no service layer at all.

---

### 1.4 Authentication / Authorization

**Spring Security IS present** — the dependency is in `pom.xml` (line 40) and `SecurityConfig.java` exists. However, what is configured is minimal and has significant issues.

**File:** `src/main/java/com/jobportal/security/SecurityConfig.java`

- `.antMatchers("/api/jobs/**").authenticated()` — requires Basic Auth for ALL `/api/jobs/**` including GET
- `.anyRequest().permitAll()` — everything else is open, including `/api/applications/**`
- `.httpBasic()` — HTTP Basic Auth only

**Credentials are hardcoded** in `application.properties` (lines 13-15): `admin / admin123`, role `ADMIN`.

**`/api/applications/**` is completely unprotected** — anyone can read all applications or delete any application with no credentials.

**`GET /api/jobs` requires Basic Auth** — the public job listing page (`jobs.html`) will receive a 401. The `jobs.js` does NOT send any Authorization header, so the job listing silently fails.

`SecurityConfig` uses deprecated `WebSecurityConfigurerAdapter` which was **removed in Spring Boot 3.x**. This class **will not compile** against Spring Boot 3.2 (pom.xml line 9). The app cannot start as-is.

> **Explicit statement:** There is NO user authentication system, NO login/register flow, NO JWT, NO session management, NO role differentiation between recruiters and candidates. The only auth artifact is a single hardcoded admin user via HTTP Basic inside a broken config class that prevents compilation.

---

### 1.5 `application.properties`
**File:** `src/main/resources/application.properties`

| Key | Value | Notes |
|-----|-------|-------|
| `spring.datasource.url` | `jdbc:h2:mem:job_portal` | **In-memory** — data lost on every restart |
| `spring.datasource.driverClassName` | `org.h2.Driver` | — |
| `spring.datasource.username` | `sa` | Default H2 superuser |
| `spring.datasource.password` | *(empty)* | No password |
| `spring.h2.console.enabled` | `true` | H2 web console at `/h2-console` |
| `spring.h2.console.path` | `/h2-console` | — |
| `server.port` | `8080` | — |
| `spring.security.user.name` | `admin` | **Plaintext secret in source** |
| `spring.security.user.password` | `admin123` | **Plaintext secret in source** |
| `spring.security.user.roles` | `ADMIN` | — |
| `spring.data.jpa.repositories.enabled` | `true` | — |

No `spring.jpa.hibernate.ddl-auto` is set — Spring Boot 3 defaults to `create-drop` for embedded databases, meaning the schema is recreated and wiped on every restart.

---

### 1.6 Critical Boot-time Bugs (app does not start)

1. **`@EnableMongoRepositories` on main class** (`JobApplicationPortalApplication.java` line 8): No MongoDB dependency in `pom.xml`, no MongoDB datasource. Causes `ClassNotFoundException` or context-load failure at startup.
2. **`WebSecurityConfigurerAdapter` removed in Spring Boot 3.x**: `SecurityConfig` extends this class which no longer exists. The app **will not compile**.
3. **`findByJobId` not declared** in `ApplicationRepository` — Spring Data proxy will fail to initialise.
4. **`Job.getId()` returns `String` but `@Id` is `Long`** — compiler error or runtime serialisation anomalies.

---

## 2. Frontend Reality

### 2.1 HTML Pages

All pages are in `/public`. There is no build step, no bundler, no framework. Tailwind CSS is loaded from a CDN (`cdn.jsdelivr.net/npm/tailwindcss@2.2.19`).

| File | Purpose | What it renders |
|------|---------|-----------------|
| `public/index.html` | Landing / home page | Full-screen background image, heading, "Find Your Next Opportunity" tagline, one link to `jobs.html`. No JS. |
| `public/jobs.html` | Job listing page | Location search input + empty grid populated by `jobs.js`. |
| `public/apply.html` | Application form | Name, Email, Resume file picker (PDF), Cover Letter textarea. Reads `?jobId` from URL. |
| `public/admin.html` | Admin / recruiter dashboard | "Add New Job" form + applications table. Hardcodes credentials in linked JS. |

There is **no login page, no register page, no candidate dashboard, no recruiter profile page, no company management page, no application status tracking page**.

---

### 2.2 Fetch Calls — Endpoint Match Analysis

#### `public/js/jobs.js` (used by `jobs.html`)

| Call | Endpoint | Auth Headers | Backend exists? | Status |
|------|----------|-------------|-----------------|--------|
| `fetch('/api/jobs')` | `GET /api/jobs` | None | YES | **BROKEN** — SecurityConfig requires Basic Auth for `/api/jobs/**`; no auth header sent, returns 401 |

Search is done **client-side** on the already-fetched array (filtering by location). The backend `GET /api/jobs/search` is never called from any frontend page.

#### `public/js/apply.js` (used by `apply.html`)

| Call | Endpoint | Auth Headers | Backend exists? | Status |
|------|----------|-------------|-----------------|--------|
| `fetch('/api/applications', {method:'POST'})` | `POST /api/applications` | None | YES | **BROKEN** — form inputs have no `name` attributes so `formData.get('name')` etc. all return `null`; resume file is silently ignored |

#### `public/js/admin.js` (used by `admin.html`)

| Call | Endpoint | Auth Headers | Backend exists? | Status |
|------|----------|-------------|-----------------|--------|
| `fetch('/api/applications')` | `GET /api/applications` | Basic `admin:admin123` | YES | **PARTIAL** — works if app compiles; renders applicantName and email but "Job Title" column is always empty (no join to Job) |
| `fetch('/api/jobs', {method:'POST'})` | `POST /api/jobs` | Basic + Content-Type | YES | **PARTIAL** — form inputs read by index (fragile); works if app compiles |
| `fetch('/api/applications/${id}', {method:'DELETE'})` | `DELETE /api/applications/{id}` | Basic | YES | **PARTIAL** — endpoint exists and is unprotected enough; works if app compiles |

---

### 2.3 Client-Side Routing

**None.** All pages are standalone `.html` files linked via plain `<a href>` anchors. No SPA router, no hash routing, no History API.

---

### 2.4 User Authentication State on the Frontend

**None whatsoever.**

- No `localStorage` / `sessionStorage` usage for tokens or user identity.
- No cookie handling.
- No login form or registration form.
- `admin.js` hardcodes `btoa('admin:admin123')` directly in JS source — credentials visible to anyone viewing page source.
- Any visitor can navigate directly to `admin.html` — there is no access gate.

---

## 3. Core Flows — Status

| Flow | Status | Evidence |
|------|--------|----------|
| Recruiter registers | MISSING ENTIRELY | No `/register` endpoint, no User entity, no registration page |
| Recruiter logs in | MISSING ENTIRELY | No `/login` endpoint, no session/token; only HTTP Basic hardcoded |
| Recruiter creates company | MISSING ENTIRELY | No Company entity, no controller, no page |
| Recruiter posts a job | PARTIAL | `POST /api/jobs` + `admin.html` form exist, BUT app won't compile; form reads inputs by index; no recruiter identity attached |
| Candidate registers | MISSING ENTIRELY | No user system of any kind |
| Candidate logs in | MISSING ENTIRELY | Same as above |
| Candidate browses jobs | PARTIAL | `GET /api/jobs` + `jobs.html` exist, BUT SecurityConfig returns 401 (no auth header in `jobs.js`) |
| Candidate applies | PARTIAL | `POST /api/applications` + `apply.html` exist, BUT form inputs have no `name` attributes so all fields POST as `null`; resume file is not uploaded |
| Recruiter views applicants | PARTIAL | `GET /api/applications` + admin table exist, BUT "Job Title" column is always empty; "View Resume" goes nowhere |
| Recruiter changes application status | MISSING ENTIRELY | No `status` field on `Application`, no PATCH endpoint, no UI control |
| Candidate sees status update | MISSING ENTIRELY | No status field, no candidate dashboard, no notification mechanism |

---

## 4. Data Persistence Reality

**Confirmed in-memory.**

`application.properties` line 2: `spring.datasource.url=jdbc:h2:mem:job_portal`

The `mem:` prefix means the H2 database lives entirely in the JVM heap. There is no file path and no persistent store. `spring.jpa.hibernate.ddl-auto` is not set, defaulting to `create-drop` for embedded databases — the schema is dropped and recreated on every boot.

**Practical consequences for anyone using the app today:**
- Every server restart wipes all jobs and applications.
- No seed data exists — first run gives an empty database.
- Two browser windows on the same running server share data (it is not per-session), but nothing survives a server bounce.
- The H2 web console at `http://localhost:8080/h2-console` is accessible to anyone on the network (SecurityConfig's `anyRequest().permitAll()` covers it), allowing raw database access.

---

## 5. Gap List vs LinkedIn / Naukri / Unstop-style Platform

### (a) Backend-only gaps

| Gap | Why it matters |
|-----|----------------|
| Fix `WebSecurityConfigurerAdapter` compile error | App does not start |
| Remove `@EnableMongoRepositories` | App does not start |
| Declare `findByJobId` in `ApplicationRepository` | Spring context fails to initialise |
| Fix `Job.getId()` type mismatch (`Long` field / `String` getter) | Runtime serialisation errors |
| Fix `Application` constructor type mismatch | Silent null/0 bugs on jobId |
| Persistent database (H2 file-mode or PostgreSQL) | Data survives restarts |
| `User` entity with roles (RECRUITER, CANDIDATE, ADMIN) | Foundation for all per-user data |
| JWT or session-based authentication | Secure per-user operations |
| `Company` entity linked to User(RECRUITER) | Recruiter context for jobs |
| `Application.status` field + `PATCH /api/applications/{id}/status` | Status change flow |
| Resume file upload endpoint (multipart/form-data to storage) | Actual resume capture |
| `Job` fields: `salary`, `type`, `skills`, `category`, `postedBy` FK | Rich job data |
| Pagination on `GET /api/jobs` and `GET /api/applications` | Scale beyond toy data |
| Server-side search with salary/type/skills filters | Real job discovery |
| Input validation (`@Valid`, `@NotBlank`) throughout | Basic data integrity |
| Saved/bookmarked jobs (join table User x Job) | Core candidate feature |
| Candidate profile / resume entity | Reusable application data |
| Notification system (email or in-app) | Application status communication |
| Rate limiting / CORS configuration | Security hygiene |

### (b) Frontend-only gaps

| Gap | Why it matters |
|-----|----------------|
| Login page | Any per-user flow |
| Registration page for both roles | User onboarding |
| `name` attributes on all form inputs in `apply.html` | Fields currently POST as null |
| `name` attributes on form inputs in `admin.html` | Fragile index-based access |
| Job detail page (`/jobs/{id}`) with description + Apply button | User experience |
| Candidate dashboard (my applications + statuses) | Candidate value proposition |
| Recruiter dashboard with per-job applicant views | Recruiter value proposition |
| Application status selector in admin UI | Status change UX |
| Token/session storage and auth header injection on all fetch calls | Any authenticated call |
| Resume file upload UI (progress, validation feedback) | Resume capture |
| Pagination controls on job listing | Scale |
| Advanced search filters (salary, type, skills, location) | Job discovery |
| Saved jobs / bookmarks UI | Candidate engagement |
| Notification badge / inbox UI | Status updates |

### (c) Both missing entirely

| Feature | Notes |
|---------|-------|
| User registration and login with JWT | Foundation for everything |
| Role-based access control (Recruiter vs Candidate) | Must gate all sensitive endpoints |
| Company management (create, edit, logo) | Recruiter workflow |
| Resume storage (upload to disk or cloud) | File currently ignored |
| Application status lifecycle (Pending -> Reviewed -> Interview -> Offer/Reject) | Core recruiter tool |
| Candidate profile (skills, experience, education) | Reduces cold-start on apply |
| Saved/bookmarked jobs | Candidate engagement |
| Email notifications (application received, status changed) | Communication |
| Job search with filters and pagination | Production job discovery |
| Admin panel with real role enforcement | Security |
| Analytics / dashboard stats | Recruiter insight |
| Recommendations (jobs for candidate, candidates for job) | Advanced feature |

---

## 6. Recommended Build Order

### Tier 0 — Fix Compile/Start Blockers (nothing runs until these are done)
1. Remove `@EnableMongoRepositories` from `JobApplicationPortalApplication.java`
2. Rewrite `SecurityConfig` using Spring Boot 3.x `SecurityFilterChain` bean pattern (drop `WebSecurityConfigurerAdapter`)
3. Declare `findByJobId(Long jobId)` in `ApplicationRepository`
4. Fix `Job` getter/setter types to consistently use `Long`
5. Fix `Application` constructor parameter type from `String jobId` to `Long jobId`

### Tier 1 — Auth (everything per-user depends on this)
6. `User` entity with fields: `id`, `name`, `email`, `passwordHash`, `role (RECRUITER|CANDIDATE)`, `createdAt`
7. `POST /api/auth/register` + `POST /api/auth/login` returning JWT
8. JWT filter added to `SecurityFilterChain` — protect write endpoints behind role checks
9. Login + Register HTML pages + localStorage token storage + auth header injected on all fetch calls

*Without auth, saved jobs, per-user applications, recruiter dashboards, and status tracking are logically meaningless — any user can see anyone else's data.*

### Tier 2 — Core Data Model
10. `Company` entity (`id`, `name`, `logo`, `description`, `ownerId -> User`)
11. Enrich `Job` with `salary`, `type`, `skills[]`, `category`, `postedBy -> User`, `status (OPEN|CLOSED)`
12. Add `Application.status` field (`PENDING|REVIEWED|INTERVIEW|OFFERED|REJECTED`)
13. `PATCH /api/applications/{id}/status` endpoint with RECRUITER role check
14. Switch to persistent database (H2 file-mode `jdbc:h2:file:./data/job_portal` minimum; PostgreSQL for production)

### Tier 3 — Working Core UI
15. Fix `apply.html` form `name` attributes so POST body is not all-null
16. Candidate dashboard — "My Applications" list with status badges
17. Recruiter dashboard — per-job applicant list with status dropdown
18. Job detail page — full description, company info, Apply button

### Tier 4 — Resume and Files
19. `POST /api/upload/resume` multipart endpoint -> local disk or cloud storage -> return URL
20. Update `apply.js` to upload file first, get URL, then POST application with `resumeUrl`
21. Wire "View Resume" link in admin table to real `resumeUrl`

### Tier 5 — Discovery and Scale
22. Server-side search with filters (title, location, salary range, job type, skills)
23. Pagination on job listing (`?page=0&size=20`) + frontend page controls
24. Saved/bookmarked jobs (join table + UI heart icon)

### Tier 6 — Notifications and Polish
25. Email notifications on application submit + status change (Spring Mail / SendGrid)
26. In-app notification badge (polling or WebSocket)
27. Admin role enforcement — replace hardcoded `admin:admin123` in JS with real role check

### Tier 7 — Advanced Features
28. Candidate profile (skills, experience, education)
29. Job recommendations based on candidate profile
30. Candidate recommendations to recruiter based on job requirements

---

## Quick-reference: File Inventory

| File | Type | Lines | Key Issues |
|------|------|-------|------------|
| `src/main/java/com/jobportal/JobApplicationPortalApplication.java` | Main class | 13 | `@EnableMongoRepositories` boot-blocker |
| `src/main/java/com/jobportal/controllers/JobController.java` | REST controller | 65 | Type mismatch on id |
| `src/main/java/com/jobportal/controllers/ApplicationController.java` | REST controller | 41 | Calls undeclared `findByJobId` |
| `src/main/java/com/jobportal/models/Job.java` | JPA entity | 79 | Long field / String getter mismatch |
| `src/main/java/com/jobportal/models/Application.java` | JPA entity | 78 | String jobId param / Long field mismatch |
| `src/main/java/com/jobportal/repositories/JobRepository.java` | Spring Data repo | 13 | OK |
| `src/main/java/com/jobportal/repositories/ApplicationRepository.java` | Spring Data repo | 12 | Missing `findByJobId` method |
| `src/main/java/com/jobportal/security/SecurityConfig.java` | Security config | 22 | `WebSecurityConfigurerAdapter` removed in SB3 |
| `src/main/resources/application.properties` | Config | 19 | H2 in-memory, hardcoded credentials |
| `pom.xml` | Maven build | 72 | Java 21, Spring Boot 3.2 |
| `public/index.html` | Landing page | 18 | No JS, no API calls |
| `public/jobs.html` | Job listing | 18 | Jobs fail to load (401) |
| `public/apply.html` | Application form | 39 | Inputs missing `name` attributes |
| `public/admin.html` | Admin dashboard | 66 | No access gate |
| `public/js/jobs.js` | Jobs list logic | 38 | No auth header sent |
| `public/js/apply.js` | Apply form logic | 37 | All fields POST as null |
| `public/js/admin.js` | Admin logic | 81 | Hardcoded credentials in source |
