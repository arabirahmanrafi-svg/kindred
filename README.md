# Kindred

**Find a licensed therapist who speaks your language and understands your culture.**

Many people who want therapy give up before they start. They can't find someone who speaks their language, understands their background, takes their insurance, and is actually accepting new clients. General directories filter by diagnosis and zip code, not by "a Bengali-speaking therapist who understands South Asian family dynamics and takes my student insurance."

Kindred is a directory built around that search. People looking for help never need an account. Therapists list themselves, and every license is verified by an admin before a profile goes live.

Built with **Java 21 · Spring Boot 3 · MySQL · React**.

---

## Features

**People looking for a therapist (no account, nothing stored)**
- Search by state, in-person or telehealth, language, communities the therapist understands, topics, insurance, and budget
- Results ranked by fit, with plain-language reasons for each match: *"Speaks Bengali · Experience with South Asian clients · Accepts Aetna"*, plus what's missing
- Honest availability: "confirmed 5 days ago" vs. "not confirmed recently"
- A "help me reach out" tool that drafts a first message in the browser, without sending anything to the server
- Save therapists on your device
- Crisis resources (988, 911) on every page

**Therapists**
- Free account and profile editor: languages, communities served, specialties, insurance, fees, sliding scale, contact
- One-click monthly availability confirmation

**Admins**
- License verification queue: approve, or reject with a reason the therapist sees

---

## Running it locally

**You need:** Java 21, Maven (built into IntelliJ IDEA), and Node 18+.

```bash
# Terminal 1: backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# Terminal 2: frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Development mode uses an **in-memory H2 database** loaded with **12 sample therapists**. Every sample is fictional and labeled "Sample profile" in the UI.

| Try | How |
|---|---|
| Search | Open http://localhost:5173 and pick Bengali, Arabic or Spanish |
| Therapist flow | "For therapists" → create an account → fill in your listing |
| Admin flow | Log in at "For therapists" with `admin@kindred.local` / `ChangeMe-DevOnly-2026` → approve the listing → it appears in search |

### With MySQL

```bash
docker compose up -d
cd backend
ADMIN_EMAIL=you@example.com ADMIN_PASSWORD='a-long-secret-password' \
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The `mysql` profile loads **no sample data**, and the admin account comes only from environment variables.

---

## Tests

```bash
cd backend && mvn test          # Java: matching, licensing rules, verification, auth, availability job
cd frontend && npm test         # React: pages rendered in a simulated browser with a fake API
```

| Test | What it proves |
|---|---|
| `MatchScorerTest` | Insurance and budget are hard filters. Language outranks specialty. Every match explains itself. Stale or full listings rank lower. |
| `SearchRulesTest` | Telehealth therapists only appear in states where they're licensed. In-person only in their office's state. Unverified profiles never appear. |
| `ProfileRulesTest` | Changing license details sends a verified profile back to review. Offices must be in a licensed state. Unknown options are rejected. The nightly job pauses stale listings. |
| `ApiFlowTest` | Sign up → profile → admin approval → search, over HTTP. Role checks (401/403). Account lockout after 5 wrong passwords. `javascript:` links rejected. |
| `app.test.jsx` | Crisis bar always present. Search flow. Empty-results suggestions. The reach-out drafter sends nothing to the server. Saving stays on-device. Sign-up flow. |

---

## Design decisions (what to talk about in interviews)

### 1. The licensing rule is enforced in the database query
A therapist can generally only treat a client who is physically located in a state where the therapist is licensed, and that includes telehealth. So the search query is:

> show a therapist if **(in-person and their office is in your state)** or **(telehealth and they are licensed in your state)**

This is a hard rule in SQL (`:state member of t.statesLicensed`), not a UI filter, so it can't be bypassed. The profile editor also blocks listing an office in a state you're not licensed in.

### 2. Hard filters vs. soft preferences, with explanations
- **Hard** (must pass): location and licensing, insurance, budget, verified status.
- **Soft** (scored): language (30 points), community (15), specialty (10), and availability freshness (+5 / −10).

A therapist who matches 2 of 3 preferences is still worth showing, so soft criteria rank results instead of hiding them. Each result lists **why** it matched and **what's missing**, instead of an unexplained ranking. `MatchScorer` is a pure function with no database or Spring, so it's fully unit-tested.

### 3. Verification you can't sneak around
New profiles start `PENDING` and are invisible. If a verified therapist later edits their name or license details, the profile goes **back to PENDING** automatically. Otherwise someone could get verified with a real license and then swap in different details.

### 4. Availability that decays
The biggest complaint about therapist directories is listings that say "accepting" but never reply. Here availability ages: fresh up to 30 days, then a reminder, and after 60 days a nightly `@Scheduled` job flips the listing to "not accepting" until the therapist confirms with one click.

### 5. Privacy by design
People searching for help have no accounts. Searches aren't stored, saved therapists live in `localStorage`, the message drafter runs entirely in the browser, and `<meta name="referrer" content="no-referrer">` keeps search URLs from leaking to sites people click through to. **The safest sensitive data is data you never collect.**

### 6. Authentication, built step by step
- Passwords: **BCrypt**.
- Sessions: random 256-bit tokens. Only their **SHA-256 hash** is stored, so a leaked database can't be used to log in.
- Login takes the same time whether or not the email exists (it compares against a dummy hash), so attackers can't discover which emails are registered.
- 5 wrong passwords lock that email for 15 minutes.
- Role checks: 401 when not logged in, 403 when logged in with the wrong role.

### 7. Smaller details
- **Fixed vocabularies** for languages, communities and so on (`GET /api/options`), so "Bangla" and "Bengali" can't fail to match.
- **`@BatchSize`** on the multi-value fields, so loading 50 profiles takes a handful of queries instead of hundreds (the N+1 problem).
- Website links must start with `http(s)://`, which blocks `javascript:` links (an XSS vector).
- Accessible forms: labels linked to inputs, errors announced, 44px tap targets.

### Trade-offs and what I'd change at scale
- **Tokens in `localStorage`** are simple and avoid CSRF, but can be read by injected scripts. Production hardening would move them to `HttpOnly` cookies with CSRF protection.
- **Scoring in Java** after a database pre-filter is fine for a few thousand therapists. At national scale, search would move to OpenSearch or Elasticsearch.
- **Login throttling in memory** works for one server. With several servers it would move to Redis.
- **The cold-start problem:** a directory is only as good as its listings. A real launch would start with one community, for example partnering with a university counseling center and local professional associations.

---

## Responsible design

- Kindred is a **directory, not a provider**. It doesn't diagnose, give advice, or offer an AI "therapist."
- Crisis resources are on every page. The 988 Suicide & Crisis Lifeline accepts calls and texts.
- Sample data is fictional, clearly labeled, and disabled in production.
- Therapists describe communities they serve from a fixed list. Kindred never infers anyone's identity.

---

## API

| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/api/options` | anyone | Languages, communities, specialties, insurances, states |
| GET | `/api/search?state=&format=&languages=&communities=&specialties=&insurance=&maxFee=` | anyone | Ranked, explained results |
| GET | `/api/therapists/{id}` | anyone | Verified public profile |
| POST | `/api/auth/register`, `/login`, `/logout` · GET `/me` | therapists, admins | Accounts |
| GET / PUT | `/api/me/profile` | therapist | Read or save own listing |
| POST | `/api/me/profile/availability` | therapist | Confirm availability |
| GET | `/api/admin/profiles?status=PENDING` | admin | Review queue |
| POST | `/api/admin/profiles/{id}/approve` · `/reject` | admin | Verify or reject |

Errors use `application/problem+json` with a readable `detail`, and `fields` for form errors.

---

## Project structure

```
backend/src/main/java/com/kindred/
  domain/      Account, AuthSession, TherapistProfile
  repo/        Spring Data repositories, including the licensing-aware search query
  auth/        Login, sessions, throttling
  search/      SearchController, SearchService, MatchScorer (pure, unit-tested)
  therapist/   Profile editing, admin review, availability policy and nightly job
  options/     Fixed vocabularies
  config/      CORS, admin bootstrap, sample data, SPA routing
frontend/src/
  pages/       Home, Search, Therapist, Saved, ForTherapists, Dashboard, Admin
  SearchForm.jsx, components.jsx, api.js, auth.jsx, storage.js
```

## Deploying

```bash
./scripts/build.sh     # bundles the React app into the Spring Boot jar
java -jar backend/target/kindred-1.0.0.jar --spring.profiles.active=mysql
```

Set `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `ADMIN_EMAIL` and `ADMIN_PASSWORD`. Serve it over HTTPS, for example AWS EC2 + RDS with Caddy in front, or Railway or Render.

## Roadmap
- Email reminders for availability confirmation (Spring Mail)
- Waitlist sign-up for full therapists
- Distance search by ZIP code
- Spanish and Bengali translations of the interface itself
- Flyway migrations, HttpOnly cookie sessions
