# VentureAI Backend API Reference

Base URL (local dev): `http://localhost:8080/api`

All endpoints except `POST /auth/register` and `POST /auth/login` (and the
password-reset pair) require a JWT bearer token:

```
Authorization: Bearer <token>
```

The token is returned by `/auth/login` and `/auth/register` and is
attached automatically by the frontend's axios interceptor
(`frontend/src/api/client.js`). A `401` response anywhere causes the
frontend to clear the stored token and redirect to `/login`.

Every ownership-scoped resource (`startups`, `validations`, `finance`,
`reports`, `competitors`) is filtered by the authenticated user server-side
— you can only ever read or modify data you created.

---

## Auth — `/api/auth`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `RegisterRequest { name, email, password, userType }` | `userType` ∈ `ENTREPRENEUR`, `STUDENT`, `STARTUP_FOUNDER`. Returns `AuthResponse { token, user }`. |
| POST | `/auth/login` | `LoginRequest { email, password }` | Returns `AuthResponse { token, user }`. |
| POST | `/auth/forgot-password` | `ForgotPasswordRequest { email }` | **Dev mode**: no SMTP is configured, so the response includes the reset token directly (`MessageResponse`) instead of emailing it. Swap in a real mail sender for production. |
| POST | `/auth/reset-password` | `ResetPasswordRequest { token, newPassword }` | Consumes the token issued above. |

## Startups — `/api/startups`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/startups` | `StartupRequest` (all 5 wizard steps — see below) | Creates a startup owned by the current user. Returns `StartupResponse`. |
| GET | `/startups` | – | Lists all startups owned by the current user. |
| GET | `/startups/{id}` | – | Single startup, 404 if not owned. |
| PUT | `/startups/{id}` | `StartupRequest` | Full update. |
| DELETE | `/startups/{id}` | – | Cascades to its competitors, validations, and financial data. |

`StartupRequest` fields (populated across the 5-step wizard):
`name`, `industry`, `businessCategory`, `startupIdea`, `description`,
`problem`, `solution`, `targetMarket`, `businessModel`, `revenueModel`,
`uniqueValueProposition`, `marketingStrategy`, `location`,
`founderExperience` (0–100, optional — an extra signal for the Random
Forest model), plus the initial financial assumption fields consumed by
step 3 of the wizard (see `FinancialDataRequest` under Finance below) and
an optional list of initial competitors (step 4).

## Competitors — `/api/startups/{startupId}/competitors`

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/startups/{startupId}/competitors` | – | List. |
| POST | `/startups/{startupId}/competitors` | `CompetitorRequest { name, website, description }` | Adds a manually-entered competitor (`source = MANUAL`, ratings default to 3/5). |
| POST | `/startups/{startupId}/competitors/discover` | – | Calls the AI engine (Gemini or rule-based fallback) to auto-discover up to 3 competitors (`source = AUTO_DISCOVERED`) from the startup's industry/description, persists and returns them. |
| DELETE | `/startups/{startupId}/competitors/{competitorId}` | – | Removes one competitor. |

`CompetitorDto` returned fields: `id, name, website, description, pricing,
strengths, weaknesses, marketPosition, source, priceRating,
featureRating, marketReachRating, innovationRating` (ratings are 1–5,
used for the competitor comparison radar/bar chart).

## Validation pipeline — `/api`

| Method | Path | Notes |
|---|---|---|
| POST | `/validation/{startupId}` | **Runs the full pipeline**: NLP extraction → Random Forest prediction → AI analysis engine (SWOT/competitors/market/recommendations/risk) → persists a `Validation` + `SwotAnalysis` + `AiInsight` row. Synchronous — the frontend's processing screen polls this promise while animating stage progress, and navigates on real completion (not a fixed timer). Returns `ValidationResultResponse`. |
| GET | `/validation/{id}` | Re-fetch a previously computed result. |
| GET | `/validation/{id}/swot` | `SwotDto { strengths[], weaknesses[], opportunities[], threats[], aiGenerated }`. |
| GET | `/validation/{id}/competitors` | `CompetitorAnalysisResponse` — competitors + AI competitive-advantage / market-gap / differentiation narrative. |
| GET | `/validation/{id}/market-opportunity` | `MarketOpportunityResponse`. |
| GET | `/validation/{id}/recommendations` | `RecommendationsResponse` — categorized action items. |
| GET | `/validation/{id}/risks` | `RiskAnalysisResponse { risks[], riskLevel, riskScore }`. |
| GET | `/validation/{id}/verdict` | `{ "verdict": "<final AI verdict sentence>" }`. |
| GET | `/validation-history` | `ValidationHistoryItemDto[]` for every validation the user has run (used by History, Reports, and Compare pages). |

`ValidationResultResponse` shape (top-level result screen):
```json
{
  "validationId": 12,
  "startupName": "SmartFood AI",
  "industry": "FoodTech",
  "viabilityScore": 74.2,
  "classification": "MODERATE_POTENTIAL",
  "confidencePercent": 81,
  "riskLevel": "MEDIUM",
  "scoreBreakdown": {
    "marketPotential": 78,
    "businessModel": 70,
    "competition": 65,
    "financialFeasibility": 72,
    "scalability": 80
  },
  "detectedIndustry": "FoodTech",
  "extractedKeywords": ["meal planning", "ai recommendation", "..."],
  "createdAt": "2026-09-06T10:15:00"
}
```
`classification` ∈ `HIGH_POTENTIAL | MODERATE_POTENTIAL | NEEDS_IMPROVEMENT
| HIGH_RISK` (thresholds: ≥80 / ≥60 / ≥40 / below, from
`Classification.fromScore`). `riskLevel` ∈ `LOW | MEDIUM | HIGH`.

## Finance — `/api/finance`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/finance/{startupId}` | `FinancialDataRequest` (investment, operating costs, expected customers/revenue, growth rate, break-even unit economics) | Creates/updates the startup's raw financial inputs. |
| GET | `/finance/{startupId}` | – | `FinancialDashboardResponse`: full computed P&L, cash flow, balance sheet, break-even point, and month-by-month revenue projection, derived from the stored inputs. |
| POST | `/finance/{startupId}/recalculate` | `FinancialAssumptionsRequest` | Re-runs the financial engine with adjusted assumptions without persisting them as the startup's permanent inputs (used by "what-if" sliders on the Financial Dashboard). |

Modeling methodology (also documented in code comments on
`FinanceEngineService`): monthly customer growth is compounded down from
the user's supplied *annual* growth rate; COGS is computed from supplied
unit economics when available, otherwise defaults to 25% of revenue;
opening cash for month 1 is the initial investment; the balance sheet is
constructed so `Assets = Liabilities + Equity` always holds, by solving
cash as the plug figure.

## Reports — `/api/reports`

| Method | Path | Notes |
|---|---|---|
| GET | `/reports/{validationId}` | Generates (if not already generated) and streams the PDF **inline** (`Content-Disposition: inline`). |
| GET | `/reports/{validationId}/download` | Same PDF, but forces a **download** (`Content-Disposition: attachment; filename="VentureAI-Validation-Report-{id}.pdf"`). |

Both return `application/pdf` bytes directly (not JSON). The PDF is built
with OpenPDF and includes the viability score, classification, SWOT,
competitor summary, financial highlights, and the AI-generated final
verdict.

## Dashboard — `/api/dashboard`

| Method | Path | Notes |
|---|---|---|
| GET | `/dashboard` | Aggregate view for the main Dashboard page: total startups, average viability score, latest validations, classification breakdown — everything the landing dashboard's KPI cards and charts need in one call. |

---

## Internal service: ML microservice (not called by the frontend directly)

Base URL (local dev): `http://localhost:8001` — called only by the Spring
Boot backend (`MlClientService`), which also has a local fallback
implementation of both endpoints if this service is unreachable, so the
platform degrades gracefully rather than failing the whole pipeline.

| Method | Path | Notes |
|---|---|---|
| POST | `/nlp/extract` | Runs the NLTK pipeline over the startup's text fields; returns detected industry, extracted keywords, and the four qualitative feature scores. |
| POST | `/predict` | Feeds the NLP output (plus numeric fields like founder experience) into the trained Random Forest regressor + classifier; returns `viabilityScore`, `classification`, and a blended `confidence`. |
| GET | `/health` | Liveness check. |
| GET | `/metrics` | Training metrics (R², accuracy, etc.) from the last training run, for transparency about model quality. |

## Error format

All errors (validation failures, not-found, auth failures) are returned by
`GlobalExceptionHandler` as:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Startup not found", "path": "/api/startups/99" }
```

The frontend's `extractErrorMessage` helper (`frontend/src/api/client.js`)
reads the `message` field for toast notifications.
