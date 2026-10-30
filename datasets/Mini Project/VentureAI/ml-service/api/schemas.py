from typing import List, Optional

from pydantic import BaseModel, Field


class NlpExtractRequest(BaseModel):
    startup_name: str = ""
    description: str = ""
    problem: str = ""
    solution: str = ""
    target_market: str = ""
    business_model: str = ""
    revenue_model: str = ""
    unique_value_proposition: str = ""


class NlpFeatures(BaseModel):
    problem_severity: float
    unique_value_prop_score: float
    scalability_signal: float
    business_model_clarity: float


class NlpExtractResponse(BaseModel):
    startup_name: str
    industry: str
    keywords: List[str]
    problem_summary: str
    target_customer_summary: str
    solution_summary: str
    tokens_processed: int
    features: NlpFeatures


class PredictRequest(BaseModel):
    industry: str = "Other"
    initial_investment: float = Field(default=500000, ge=0)
    monthly_operating_cost: float = Field(default=100000, ge=1)
    market_size: float = Field(default=50, ge=0, le=100)
    competition_level: float = Field(default=50, ge=0, le=100)
    expected_growth_rate: float = Field(default=20, ge=0)
    customer_demand: float = Field(default=50, ge=0, le=100)
    business_model_strength: Optional[float] = Field(default=None, ge=0, le=100)
    scalability: Optional[float] = Field(default=None, ge=0, le=100)
    founder_experience: float = Field(default=50, ge=0, le=100)
    problem_severity: Optional[float] = Field(default=None, ge=0, le=100)
    unique_value_prop_score: Optional[float] = Field(default=None, ge=0, le=100)


class PredictResponse(BaseModel):
    score: float
    classification: str
    confidence: float


# NOTE: PredictRequest/PredictResponse above are unchanged - this is
# the exact contract Spring Boot's MlClientService/MlPredictionResult
# already expects. POST /predict now runs BART instead of XGBoost
# (see api/main.py + model/bart_model.py), but the request/response
# shape callers see is identical. There is no separate Bart*
# request/response schema anymore - the previous BartPredictRequest/
# BartPredictResponse (used by a now-removed /predict/bart endpoint)
# have been removed since BART is no longer an optional side model.
