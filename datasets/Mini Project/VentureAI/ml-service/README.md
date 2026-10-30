# VentureAI ML Service

FastAPI microservice for the VentureAI platform:

```
React  ->  Spring Boot  ->  FastAPI (this service)  ->  BART  ->  score / classification / confidence
```

BART (`facebook/bart-base`, `BartForSequenceClassification`, `num_labels=2`) is the only
model behind `POST /predict`. XGBoost has been removed from the active prediction path -
its old `.joblib` artifacts under `model/` are left on disk for backup but are never
loaded or called by this service.

## Endpoints

- `POST /predict` - startup viability prediction (BART). Request/response contract is
  unchanged from before: `PredictRequest` in, `{"score": float, "classification": "Viable"|"Not Viable", "confidence": float}` out.
- `POST /nlp/extract` - unchanged.
- `GET /health` - reports BART's status: `{"status", "service", "model": "BART", "bart_loaded", "bart_trained", "device"}`.
- `GET /metrics` - real BART training metrics if a fine-tuned checkpoint exists, otherwise an honest "not trained yet" message. Never reports old XGBoost numbers as BART's.

## Project layout

```
ml-service/
├── api/
│   ├── main.py          FastAPI app, all 4 endpoints
│   └── schemas.py        PredictRequest/PredictResponse + NLP schemas (unchanged)
├── model/
│   ├── bart_model.py     Loads BART once at startup, runs inference
│   ├── bart_text.py       THE shared text-construction function used by
│   │                      both training and POST /predict
│   ├── predictor.py       Pre-existing, unused dead code (not touched)
│   └── *.joblib, metrics.json   Old XGBoost artifacts - kept for backup, unused
├── preprocessing/
│   └── nlp_pipeline.py    Unchanged
├── training/
│   ├── generate_dataset.py   Old XGBoost synthetic-data generator - unchanged,
│   │                          no longer used by BART training
│   └── train_model.py        Old XGBoost training script - unchanged
├── datasets/
│   └── raw/               Original datasets, byte-for-byte unmodified
├── train_bart.py           BART fine-tuning script (see below)
└── requirements.txt
```

## Why BART's input is only 3 fields

`PredictRequest` still accepts all 12 of its existing fields (Spring Boot/React need no
changes). But only 3 of them have a genuine, unit-compatible counterpart in the real
datasets under `datasets/raw/`:

| PredictRequest field      | Real-dataset counterpart                              |
|----------------------------|--------------------------------------------------------|
| `industry`                 | `category_list` / `category_code` / `sector`           |
| `initial_investment` (USD) | `funding_total_usd`                                     |
| `monthly_operating_cost`   | `burn_rate_million` (unit-converted to USD)             |

The other 9 fields (`market_size`, `competition_level`, `expected_growth_rate`,
`customer_demand`, `business_model_strength`, `scalability`, `founder_experience`,
`problem_severity`, `unique_value_prop_score`) are 0-100 subjective founder
self-ratings. No dataset available to this project - real company records, all of
them - contains a founder's own rating of their market or business model, so there is
nothing genuine to map those 9 fields to. Rather than fabricate a mapping (e.g.
rescaling real "years of founder experience" into a fake 0-100 "confidence score"),
those 9 fields are simply not used to build BART's text. See `model/bart_text.py`'s
module docstring for the full reasoning, and the delivered report for the recommended
follow-up (a `PredictRequest` contract change) that would let BART use the project's
richer real fields instead.

`model/bart_text.py`'s `build_bart_text()` is called by both `train_bart.py` (training)
and `model/bart_model.py` (inference, via `api/main.py`) - the same function, same
arguments, same output shape, every time.

## Datasets used for training, and why

| Dataset | Rows | Real outcome column | Used? |
|---|---|---|---|
| `startup_data.csv` | 66,369 | `status` (operating/acquired/ipo/closed) | Yes |
| `startup_success_prediction_dataset.csv` | 923 | `labels` (1=acquired/0=closed) | Yes |
| `startup_success_dataset.csv` | 100,000 | `outcome` (Failure/Acquisition/IPO) | Yes |
| `big_startup_secsees_dataset.csv` | 66,369 | same as `startup_data.csv` | No - byte-identical duplicate (verified by hash) |
| `Listofstartups.csv` | 241 | none | No - no outcome column, no reliable link to a labeled record |
| `startup_funding.csv` | 3,044 | none | No - no outcome column, no reliable link to a labeled record |

Target mapping (never included in BART's input text - only used to compute the label):

- `startup_data.csv`: `operating`/`acquired`/`ipo` -> 1 (Viable), `closed` -> 0 (Not Viable)
- `startup_success_prediction_dataset.csv`: `labels == 1` -> 1, `labels == 0` -> 0
- `startup_success_dataset.csv`: `outcome` in `{Acquisition, IPO}` -> 1, `Failure` -> 0

## Running it (Windows)

```cmd
cd ml-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python -m uvicorn api.main:app --reload --port 8001
```

Test `/health`:
```cmd
curl http://localhost:8001/health
```

Test `/predict`:
```cmd
curl -X POST http://localhost:8001/predict -H "Content-Type: application/json" -d "{\"industry\": \"Technology\", \"initial_investment\": 500000, \"monthly_operating_cost\": 100000, \"market_size\": 80, \"competition_level\": 30, \"expected_growth_rate\": 35, \"customer_demand\": 85, \"business_model_strength\": 80, \"scalability\": 90, \"founder_experience\": 70, \"problem_severity\": 85, \"unique_value_prop_score\": 90}"
```

Smoke-test training (fast - verifies the pipeline, not a real model):
```cmd
set BART_SMOKE_TEST=1 && python train_bart.py
```

Full training (slow on CPU, ready for CUDA once available):
```cmd
python train_bart.py
```

## Model state

With no checkpoint at `startup_bart_model/`, `/predict` runs on raw `facebook/bart-base`
- architecturally correct (`bart_loaded: true`) but with an untrained classification
head (`bart_trained: false`). Run `train_bart.py` to produce a real checkpoint; restart
the service afterward to pick it up automatically.
