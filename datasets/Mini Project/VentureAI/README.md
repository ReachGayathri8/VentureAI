# VentureAI

**AI Startup Idea Validation and Business Intelligence Platform**

> *Validate your idea. Understand your market. Build with confidence.*

VentureAI is a full-stack platform that takes a founder's raw startup idea
and turns it into a data-backed validation report: an NLP-extracted feature
set, a Random Forest viability score, an AI-generated SWOT / competitor /
market-opportunity / recommendations / risk analysis, a complete financial
model (P&L, cash flow, balance sheet, break-even, revenue projection), a
BI dashboard, and a downloadable PDF report.

Mini Project Review-1, Dept. of AI, Anurag University (AY 2026-27) — Team 9
(Gayathri, Saifur Rehmaan, Neel Karan), guided by Mr. L. Sridhara Rao.

---

## 1. Architecture

Three independent services plus a database:

```
                     ┌─────────────────────┐
                     │   React Frontend     │
                     │ (Vite, Tailwind)      │  http://localhost:5173
                     └──────────┬───────────┘
                                │ REST + JWT
                     ┌──────────▼───────────┐
                     │  Spring Boot Backend  │  http://localhost:8080
                     │ (Java 17, Spring      │
                     │  Security, JPA)       │
                     └───┬───────────────┬───┘
                         │               │
             REST (internal)      Gemini API (optional)
                         │               │
              ┌──────────▼──────────┐    │
              │  Python ML Service   │    │  generativelanguage.googleapis.com
              │  (FastAPI, NLTK,     │    │  - only called if GEMINI_ENABLED=true
              │   scikit-learn RF)   │    │  - otherwise a rule-based engine
              └──────────────────────┘    │    inside the backend produces the
                         │                │    same AI sections from real data
              ┌──────────▼──────────┐
              │       MySQL          │
              └──────────────────────┘
```

- **Frontend** — React 18 + Vite + Tailwind CSS. Landing page, auth flow,
  a 5-step "New Startup Validation" wizard, a live (non-fake) AI processing
  screen, and 24 dashboard pages covering every view in the spec (SWOT,
  competitor analysis, market intelligence, financial dashboard, P&L, cash
  flow, balance sheet, break-even, revenue projection, AI recommendations,
  risk analysis, validation history, compare, reports, profile, settings).
- **Backend** — Spring Boot 3 REST API. Owns auth (JWT), persistence
  (MySQL via Spring Data JPA/Hibernate), orchestrates the validation
  pipeline (calls the ML service, then the AI analysis engine, then the
  financial engine), and generates PDF reports.
- **ML service** — a separate Python/FastAPI microservice that does NLP
  feature extraction (NLTK) and viability scoring with a real, trained
  **Random Forest** model (scikit-learn) — not hard-coded scores.
- **AI Business Intelligence Engine** — lives inside the backend
  (`service/ai`). It calls the Gemini API when `GEMINI_ENABLED=true` and a
  key is configured; otherwise it falls back to a deterministic,
  input-driven rule-based engine that builds SWOT/competitor/market/
  recommendation/risk content from the *actual* startup data (never
  generic static text), so the whole platform works end-to-end with zero
  external API keys.

## 2. Repository layout

```
VentureAI/
├── frontend/          React + Vite + Tailwind SPA
├── backend/           Spring Boot REST API (Java 17)
├── ml-service/        FastAPI + NLTK + scikit-learn microservice
├── docs/
│   ├── API.md               full REST endpoint reference
│   └── DATABASE_SCHEMA.md   JPA-derived MySQL schema reference
└── README.md          this file
```

## 3. Prerequisites

| Tool | Version | Used by |
|---|---|---|
| Java (JDK) | 17+ | backend |
| Maven | 3.9+ (or use the bundled `mvnw` if you add one) | backend |
| MySQL | 8.x | backend |
| Node.js | 18+ | frontend |
| Python | 3.10+ | ml-service |

> **Note on this build's verification status.** The sandbox this project
> was built in blocks outbound access to Maven Central
> (`repo.maven.apache.org`) at the network/proxy level, so the backend
> could **not** be compiled with `mvn compile` inside that sandbox — only
> reviewed manually (imports, type-correctness, a Java naming-collision bug
> in the PDF report service, and a stray full-width-comma typo were found
> and fixed this way). The **frontend was fully installed and built**
> (`npm install && npm run build` succeeded) and the **ML service was
> fully installed, trained, and exercised end-to-end** (`pip install`,
> trained the Random Forest, ran the FastAPI server, and hit `/predict`
> and `/nlp/extract` live). On a normal machine or CI runner with regular
> internet access, Maven Central is not blocked, and `mvn spring-boot:run`
> is expected to work — this is a note about the build sandbox, not a
> known defect in the code. Please run `mvn clean compile` first thing
> after downloading and report back if anything doesn't build.

## 4. Setup & run (in order)

Services must start in this order because each one depends on the one
before it being reachable.

### 4.1 MySQL

Create nothing by hand — the backend is configured with
`createDatabaseIfNotExist=true` and `ddl-auto=update`, so it will create
the `ventureai` database and all tables on first boot. Just make sure a
MySQL 8 server is running and you know a username/password that can create
databases.

### 4.2 ML service (Python)

```bash
cd ml-service
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env             # defaults are fine for local dev

# Train the Random Forest model (generates a documented synthetic
# training set, trains, and saves artifacts to ml-service/model/).
# The API also auto-trains on first boot if no artifacts exist, but
# running it explicitly first lets you see the metrics.
python -m training.train_model

# Start the service
uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload
```

Verify it's up: `curl http://localhost:8001/health`

### 4.3 Backend (Spring Boot)

```bash
cd backend
cp .env.example .env             # edit DB_USERNAME / DB_PASSWORD to match your MySQL
# export the vars in .env into your shell, or pass them as -D system
# properties / configure them in your IDE run configuration

mvn clean compile                # verify it builds
mvn spring-boot:run
```

Verify it's up: `curl http://localhost:8080/api/auth/login` (should return
a 400/405, not a connection error — confirms the server is listening).

### 4.4 Frontend (React)

```bash
cd frontend
cp .env.example .env             # VITE_API_BASE_URL defaults to http://localhost:8080/api
npm install
npm run dev                      # dev server at http://localhost:5173
# or: npm run build && npm run preview   for a production build
```

Open `http://localhost:5173` — you should see the VentureAI landing page.
Register an account, then run through **New Startup → 5-step wizard →
Validate** to see the full pipeline (NLP → Random Forest → AI analysis →
financial engine → dashboard) execute against your own idea.

## 5. Environment variables

Each service has its own `.env.example` with inline comments
(`backend/.env.example`, `ml-service/.env.example`, `frontend/.env.example`).
The one setting that connects two services deliberately is
`ML_SERVICE_BASE_URL` (backend → ml-service) and `VITE_API_BASE_URL`
(frontend → backend); everything else is local to its own service.

**Running without a Gemini API key (the default):** leave
`GEMINI_ENABLED=false` in `backend/.env`. The AI Business Intelligence
Engine (SWOT, competitor analysis, market opportunity, recommendations,
risk analysis) runs entirely on `RuleBasedAnalysisEngine`, which builds
every section from the startup's actual submitted data (industry, problem,
solution, target market, NLP-extracted keywords, Random Forest score
breakdown) — never generic placeholder text. Flip `GEMINI_ENABLED=true`
and set `GEMINI_API_KEY` once you have one; no other code changes are
needed, since `GeminiAiService` already calls the real Gemini client first
and only falls back to the rule-based engine on failure/absence.

## 6. The validation pipeline, end to end

1. **NLP processing** (`ml-service/preprocessing/nlp_pipeline.py`) —
   cleans and lemmatizes the startup's text fields, extracts keywords,
   classifies the industry from keyword banks, and scores four qualitative
   signals (problem severity, unique-value-proposition strength,
   scalability signal, business-model clarity).
2. **Random Forest viability model** (`ml-service/model/`) — a
   `RandomForestRegressor` + `RandomForestClassifier` pair trained on a
   documented synthetic dataset (`training/generate_dataset.py`) built from
   an explicit weighted formula plus Gaussian noise, not hand-picked
   numbers. Retraining on real historical data later is a drop-in
   replacement as long as the same feature-column contract
   (`FEATURE_COLUMNS` in `generate_dataset.py`) is preserved.
3. **AI Business Intelligence Engine** (`backend/.../service/ai`) — SWOT,
   competitor discovery/analysis, market opportunity, personalized
   recommendations, and risk analysis, either via live Gemini or the
   rule-based fallback described above.
4. **Financial engine** (`backend/.../service/finance/FinanceEngineService.java`) —
   revenue projection, P&L, cash flow, balance sheet (constructed so
   Assets = Liabilities + Equity by solving cash as the plug), and
   break-even analysis, from the founder-supplied financial assumptions.
5. **BI dashboard** — the React frontend renders all of the above as KPI
   cards, gauges, and Recharts visualizations across the 20+ dashboard
   pages.
6. **PDF report** (`backend/.../service/report/ReportPdfService.java`,
   built on OpenPDF) — a downloadable "AI Startup Validation Report" that
   summarizes the full result set.

See `docs/API.md` for the exact request/response shape of every step, and
`docs/DATABASE_SCHEMA.md` for how it's all persisted.

## 7. Deployment notes

Per the spec's deployment-readiness guidance: the frontend is a static
Vite build and deploys cleanly to **Vercel** (or Netlify); the backend and
ML service are both standard JVM/Python services and deploy to
**Render**, **Railway**, or **Google Cloud Run/App Engine**. Point
`VITE_API_BASE_URL` at the deployed backend URL, `ML_SERVICE_BASE_URL` at
the deployed ML service URL, and provision a managed MySQL instance
(PlanetScale, Cloud SQL, RDS, etc.) for `DB_HOST`/`DB_USERNAME`/
`DB_PASSWORD`. No code changes are required to move from local dev to a
cloud deployment — everything is environment-variable driven.

## 8. Known limitations / honest status

- Backend Java code was **not compiler-verified** in the environment this
  was built in (Maven Central was blocked at the network level there) —
  see the note in section 3. It was reviewed manually end-to-end, and two
  real bugs were found and fixed this way (a `java.util.List` vs.
  `com.lowagie.text.List` naming collision in the PDF report service, and
  a stray non-ASCII comma typo in the ML client's fallback path). Run
  `mvn clean compile` as your first step.
- `forgot-password` runs in **dev mode**: since no SMTP/email provider is
  configured, the reset token is returned directly in the API response
  (clearly labeled as such) rather than emailed. Wire up a mail sender in
  `AuthService` for production use.
- The Random Forest model is trained on a **documented synthetic
  dataset** (see `ml-service/training/generate_dataset.py`) rather than
  real historical startup outcomes, since no such dataset was provided.
  The formula and feature weights are explicit and commented so the model
  can be retrained on real data later without changing the API contract.
- Gemini integration is real and pluggable but was built and tested
  against its **rule-based fallback only** (no API key was available
  during development) — the live-Gemini code path has not been exercised
  against the actual Gemini API.
