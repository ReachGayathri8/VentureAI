"""
Synthetic startup-viability dataset generator.

IMPORTANT (see spec section 9/29): this project ships without a licensed
real-world startup outcomes dataset, so we generate a synthetic-but-principled
dataset from a transparent scoring formula plus noise. The Random Forest is
trained on this data, but the whole pipeline (feature schema, training script,
serialized model artifact) is written so a real historical dataset with the
same feature columns can be dropped in and retrained with zero code changes
elsewhere in the system — see README "Retraining with a real dataset".
"""
from __future__ import annotations

import numpy as np
import pandas as pd

FEATURE_COLUMNS = [
    "market_size",
    "competition_level",
    "expected_growth_rate",
    "customer_demand",
    "business_model_strength",
    "scalability",
    "founder_experience",
    "problem_severity",
    "unique_value_prop_score",
    "investment_adequacy",
]

INDUSTRIES = [
    "Technology", "Healthcare", "FinTech", "EdTech", "Food & Beverage",
    "E-commerce", "Agriculture", "Travel", "Logistics", "Entertainment",
    "SaaS", "AI", "Other", "Food Technology / AI", "HealthTech",
]

# Per-industry base multiplier — a mild prior reflecting typical market
# dynamics (e.g. SaaS/AI trend a bit higher on scalability & growth in this
# synthetic prior; this is illustrative, not empirical fact).
INDUSTRY_PRIOR = {
    "Technology": 1.00, "SaaS": 1.05, "AI": 1.06, "Food Technology / AI": 1.03,
    "HealthTech": 1.02, "Healthcare": 0.98, "FinTech": 1.00, "EdTech": 0.97,
    "Food & Beverage": 0.93, "E-commerce": 0.95, "Agriculture": 0.90,
    "Travel": 0.92, "Logistics": 0.94, "Entertainment": 0.91, "Other": 0.90,
}


def _investment_adequacy(investment: np.ndarray, monthly_cost: np.ndarray) -> np.ndarray:
    """Runway-derived adequacy score: more months of runway -> higher score,
    with diminishing returns after ~18 months."""
    months_runway = np.divide(investment, np.maximum(monthly_cost, 1.0))
    scaled = 100 * (1 - np.exp(-months_runway / 9.0))
    return np.clip(scaled, 0, 100)


def generate(n_samples: int = 6000, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    industries = rng.choice(INDUSTRIES, size=n_samples)
    prior = np.array([INDUSTRY_PRIOR[i] for i in industries])

    market_size = rng.uniform(10, 100, n_samples)
    competition_level = rng.uniform(5, 100, n_samples)
    expected_growth_rate = rng.uniform(0, 120, n_samples)
    customer_demand = rng.uniform(10, 100, n_samples)
    business_model_strength = rng.uniform(10, 100, n_samples)
    scalability = rng.uniform(10, 100, n_samples)
    founder_experience = rng.uniform(0, 100, n_samples)
    problem_severity = rng.uniform(10, 100, n_samples)
    unique_value_prop_score = rng.uniform(10, 100, n_samples)

    investment = rng.uniform(50_000, 5_000_000, n_samples)
    monthly_cost = rng.uniform(20_000, 800_000, n_samples)
    investment_adequacy = _investment_adequacy(investment, monthly_cost)

    growth_norm = np.clip(expected_growth_rate, 0, 100)

    raw_score = (
        0.16 * market_size
        + 0.14 * customer_demand
        + 0.12 * business_model_strength
        + 0.12 * scalability
        + 0.12 * unique_value_prop_score
        + 0.09 * problem_severity
        + 0.10 * growth_norm
        + 0.07 * founder_experience
        + 0.08 * investment_adequacy
        - 0.14 * competition_level
    )
    raw_score = raw_score * prior
    noise = rng.normal(0, 6, n_samples)
    score = np.clip(raw_score + noise, 0, 100)

    def classify(s: float) -> str:
        if s >= 80:
            return "HIGH_POTENTIAL"
        if s >= 60:
            return "MODERATE_POTENTIAL"
        if s >= 40:
            return "NEEDS_IMPROVEMENT"
        return "HIGH_RISK"

    classification = np.array([classify(s) for s in score])

    df = pd.DataFrame({
        "industry": industries,
        "market_size": market_size,
        "competition_level": competition_level,
        "expected_growth_rate": expected_growth_rate,
        "customer_demand": customer_demand,
        "business_model_strength": business_model_strength,
        "scalability": scalability,
        "founder_experience": founder_experience,
        "problem_severity": problem_severity,
        "unique_value_prop_score": unique_value_prop_score,
        "investment_adequacy": investment_adequacy,
        "viability_score": score,
        "classification": classification,
    })
    return df


if __name__ == "__main__":
    data = generate()
    data.to_csv("training/synthetic_startups.csv", index=False)
    print(f"Generated {len(data)} rows -> training/synthetic_startups.csv")
