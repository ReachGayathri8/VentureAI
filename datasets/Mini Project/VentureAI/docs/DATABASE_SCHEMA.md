# VentureAI Database Schema

MySQL 8, managed by Hibernate/JPA (`ddl-auto=update` — tables are created
automatically on first boot from the entity classes below; no manual DDL
or migration files are required for local development). This documents
the schema as actually implemented in
`backend/src/main/java/com/ventureai/entity/`, which extends the spec's
"such as ..." schema sketch with two additional tables
(`ai_insights`, `financial_projections`) and a few extra columns needed to
make the platform fully functional (password reset tokens, competitor
star ratings). Every table has a surrogate auto-increment `id` primary key
in addition to what's listed.

## Entity-relationship overview

```
users 1───* startups 1───* competitors
                  │
                  ├──1───1 financial_data          (raw inputs)
                  ├──1───* financial_projections    (computed month-by-month rows)
                  │
                  └──1───* validations
                              ├──1───1 swot_analysis
                              ├──1───1 ai_insights
                              └──1───* reports
```

## `users`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(150) NOT NULL | |
| email | VARCHAR(200) NOT NULL, UNIQUE | login identifier |
| password | VARCHAR NOT NULL | BCrypt hash, never serialized to JSON (`@JsonIgnore`) |
| user_type | VARCHAR NOT NULL | `ENTREPRENEUR` \| `STUDENT` \| `STARTUP_FOUNDER` |
| role | VARCHAR NOT NULL | `ROLE_USER` \| `ROLE_ADMIN` (Spring Security authority) |
| created_at | DATETIME NOT NULL | |
| reset_token | VARCHAR(100) NULL | dev-mode forgot-password flow; `@JsonIgnore` |
| reset_token_expiry | DATETIME NULL | `@JsonIgnore` |

## `startups`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT FK → users.id NOT NULL | owner |
| name | VARCHAR(200) NOT NULL | |
| industry | VARCHAR(100) NOT NULL | free-text at input; the NLP pipeline also independently *detects* an industry per-validation (stored on `validations.detected_industry`), which can differ from this founder-declared value |
| business_category | VARCHAR(150) | |
| startup_idea | TEXT | one-line pitch |
| description | TEXT | |
| problem | TEXT | |
| solution | TEXT | |
| target_market | VARCHAR(200) | |
| business_model | VARCHAR(150) | e.g. B2B, B2C, marketplace |
| revenue_model | VARCHAR(150) | e.g. subscription, one-time, commission |
| unique_value_proposition | TEXT | |
| marketing_strategy | TEXT | |
| location | VARCHAR(150) | |
| founder_experience | DOUBLE NULL | optional 0–100 signal fed into the Random Forest model |
| created_at | DATETIME NOT NULL | |

Relationships: `competitors` (1:*), `validations` (1:*), `financial_data`
(1:1), `financial_projections` (1:*) — all cascade delete with the parent
startup.

## `competitors`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| startup_id | BIGINT FK → startups.id NOT NULL | |
| name | VARCHAR(200) NOT NULL | |
| website | VARCHAR(300) | |
| description | TEXT | |
| pricing | VARCHAR(100) | |
| strengths | TEXT | |
| weaknesses | TEXT | |
| market_position | VARCHAR(150) | |
| source | VARCHAR | `MANUAL` \| `AUTO_DISCOVERED` (extension beyond the spec sketch, so the UI can distinguish founder-entered vs. AI-discovered competitors) |
| price_rating | INT (1–5, default 3) | extension: powers the competitor comparison chart |
| feature_rating | INT (1–5, default 3) | " |
| market_reach_rating | INT (1–5, default 3) | " |
| innovation_rating | INT (1–5, default 3) | " |

## `financial_data`

Raw founder-supplied inputs (one row per startup) that the financial
engine reads to compute everything in `financial_projections`.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| startup_id | BIGINT FK → startups.id, UNIQUE NOT NULL | 1:1 with startup |
| initial_investment | DECIMAL | |
| founder_investment | DECIMAL | |
| external_funding | DECIMAL | |
| monthly_operating_cost | DECIMAL | |
| employee_cost | DECIMAL | |
| marketing_cost | DECIMAL | |
| infrastructure_cost | DECIMAL | |
| product_development_cost | DECIMAL | |
| expected_monthly_customers | INT | starting customer count |
| average_customer_revenue | DECIMAL | ARPU |
| expected_growth_rate | DOUBLE | annual %, compounded down to a monthly rate by the engine |
| projection_period_years | INT (default 3) | |
| fixed_costs | DECIMAL | break-even input |
| variable_cost_per_unit | DECIMAL | break-even input |
| selling_price_per_unit | DECIMAL | break-even input |

## `financial_projections`

**Extension table** (beyond the spec's sketch) holding one row per
projected month, so the P&L/cash-flow/revenue-projection pages can render
real time-series charts instead of recomputing on every request.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| startup_id | BIGINT FK → startups.id NOT NULL | |
| period | INT NOT NULL | month number, 1-based |
| customers | INT | |
| revenue | DECIMAL | |
| cogs | DECIMAL | |
| gross_profit | DECIMAL | |
| operating_expenses | DECIMAL | |
| net_profit | DECIMAL | |
| opening_cash | DECIMAL | |
| closing_cash | DECIMAL | |
| net_cash_flow | DECIMAL | |

## `validations`

One row per validation run (a startup can be re-validated after edits,
producing multiple rows — this is what powers History and Compare).

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| startup_id | BIGINT FK → startups.id NOT NULL | |
| viability_score | DOUBLE | 0–100, from the Random Forest regressor |
| confidence | DOUBLE | 0–1, blended tree-agreement + classifier confidence |
| risk_score | DOUBLE | |
| market_score | DOUBLE | score breakdown component |
| business_model_score | DOUBLE | " |
| financial_score | DOUBLE | " |
| scalability_score | DOUBLE | " |
| competition_score | DOUBLE | " |
| classification | VARCHAR | `HIGH_POTENTIAL` \| `MODERATE_POTENTIAL` \| `NEEDS_IMPROVEMENT` \| `HIGH_RISK` |
| risk_level | VARCHAR | `LOW` \| `MEDIUM` \| `HIGH` |
| detected_industry | TEXT | NLP-detected, independent of the founder-declared `startups.industry` |
| extracted_keywords | TEXT | JSON array, stored as text |
| created_at | DATETIME NOT NULL | |

Relationships: `swot_analysis` (1:1), `ai_insight` (1:1), `reports` (1:*).

## `swot_analysis`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| validation_id | BIGINT FK → validations.id, UNIQUE NOT NULL | |
| strengths | TEXT | JSON array of strings |
| weaknesses | TEXT | JSON array of strings |
| opportunities | TEXT | JSON array of strings |
| threats | TEXT | JSON array of strings |
| ai_generated | BOOLEAN NOT NULL (default true) | true whether produced by live Gemini or the rule-based fallback — both are "AI generated" from the platform's point of view; they differ only in which engine ran |

## `ai_insights` (extension table)

Holds every AI-generated section beyond SWOT, so the schema doesn't force
unrelated JSON blobs onto the `validations` row itself. Each `*_json`
column is parsed into a structured DTO by the service layer.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| validation_id | BIGINT FK → validations.id, UNIQUE NOT NULL | |
| competitive_advantage | TEXT | narrative |
| market_gap | TEXT | narrative |
| differentiation_strategy | TEXT | narrative |
| market_opportunities_json | TEXT | JSON array |
| recommendations_json | TEXT | JSON object keyed by category |
| risks_json | TEXT | JSON array of risk objects |
| final_verdict | TEXT | one-paragraph AI summary shown on the Validation Result page |
| ai_generated | BOOLEAN (default true) | |

## `reports`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| validation_id | BIGINT FK → validations.id NOT NULL | |
| file_path | VARCHAR(500) NOT NULL | path under `REPORTS_STORAGE_DIR` where the generated PDF is cached |
| created_at | DATETIME NOT NULL | |

Reports are generated lazily on first request (`GET /reports/{id}` or
`/download`) and cached on disk — subsequent requests for the same
validation reuse the file rather than regenerating it.

## Enums (Java-side, stored as `VARCHAR` via `@Enumerated(EnumType.STRING)`)

- **UserType**: `ENTREPRENEUR`, `STUDENT`, `STARTUP_FOUNDER`
- **Role**: `ROLE_USER`, `ROLE_ADMIN`
- **Classification**: `HIGH_POTENTIAL` (≥80), `MODERATE_POTENTIAL` (≥60),
  `NEEDS_IMPROVEMENT` (≥40), `HIGH_RISK` (<40) — thresholds on
  `viability_score`, see `Classification.fromScore`.
- **RiskLevel**: `LOW`, `MEDIUM`, `HIGH` — from the average of competition
  level and market volatility signals, see `RiskLevel.fromScore`.
- **CompetitorSource**: `MANUAL`, `AUTO_DISCOVERED`
