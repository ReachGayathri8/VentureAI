"""
NLP preprocessing pipeline for VentureAI.

Takes the free-text business inputs a founder provides (startup description,
problem statement, proposed solution, target market, business model) and
turns them into:
  1. Cleaned / tokenized / lemmatized text (classic NLP pipeline)
  2. Extracted keywords
  3. An inferred industry / category label
  4. Structured "soft" features (problem severity, UVP strength, business
     model clarity, scalability signal) that feed the Random Forest model
     as numeric inputs, since the RF itself cannot consume raw text.

This is intentionally dependency-light (NLTK only) so it runs anywhere
without downloading large language models, while still being a genuine
NLP pipeline rather than a hardcoded lookup table.
"""
from __future__ import annotations

import re
from collections import Counter
from functools import lru_cache
from typing import Dict, List

import nltk
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from nltk.tokenize import word_tokenize

_NLTK_RESOURCES = [
    ("tokenizers/punkt", "punkt"),
    ("tokenizers/punkt_tab", "punkt_tab"),
    ("corpora/stopwords", "stopwords"),
    ("corpora/wordnet", "wordnet"),
    ("corpora/omw-1.4", "omw-1.4"),
]


def ensure_nltk_data() -> None:
    """Download required NLTK corpora on first run (idempotent)."""
    for path, name in _NLTK_RESOURCES:
        try:
            nltk.data.find(path)
        except LookupError:
            try:
                nltk.download(name, quiet=True)
            except Exception:
                # offline / no network — pipeline degrades gracefully below
                pass


ensure_nltk_data()

_LEMMATIZER = WordNetLemmatizer()

try:
    _STOPWORDS = set(stopwords.words("english"))
except LookupError:
    _STOPWORDS = {
        "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "for",
        "to", "of", "in", "on", "with", "that", "this", "it", "as", "by", "be",
        "will", "we", "our", "their", "they", "i", "you", "your",
    }

# Domain keyword banks used for lightweight industry classification and
# feature scoring. This acts as the "feature extraction" layer described in
# the spec — mapping free text into the structured signals the Random
# Forest model needs.
INDUSTRY_KEYWORDS: Dict[str, List[str]] = {
    "Food Technology / AI": ["restaurant", "food", "waste", "kitchen", "menu", "demand forecast", "grocery", "recipe"],
    "FinTech": ["payment", "bank", "loan", "credit", "wallet", "invoice", "insurance", "finance", "trading", "stock"],
    "HealthTech": ["health", "patient", "hospital", "clinic", "doctor", "medicine", "diagnosis", "wellness", "fitness tracker", "therapy"],
    "EdTech": ["student", "learning", "course", "school", "university", "tutor", "exam", "curriculum", "classroom"],
    "E-commerce": ["ecommerce", "e-commerce", "marketplace", "retail", "shopping", "cart", "seller", "buyer", "product listing"],
    "Agriculture": ["farm", "crop", "agriculture", "farmer", "soil", "irrigation", "livestock", "harvest"],
    "Travel": ["travel", "hotel", "flight", "trip", "tourism", "booking", "itinerary", "vacation"],
    "Logistics": ["logistics", "delivery", "shipment", "fleet", "warehouse", "supply chain", "freight", "courier"],
    "Entertainment": ["game", "gaming", "music", "video", "streaming", "content creator", "media"],
    "SaaS": ["saas", "subscription", "dashboard", "workflow", "productivity", "b2b software", "enterprise tool"],
    "AI": ["artificial intelligence", "machine learning", "ai-powered", "ai powered", "predictive model", "neural network", "nlp", "computer vision"],
    "Technology": ["app", "platform", "software", "technology", "api", "cloud", "automation"],
}

POSITIVE_SIGNAL_WORDS = [
    "unique", "innovative", "scalable", "proprietary", "first-of-its-kind", "patented",
    "affordable", "efficient", "personalized", "automated", "seamless", "real-time",
    "data-driven", "growing", "underserved", "recurring",
]
NEGATIVE_SIGNAL_WORDS = [
    "unclear", "uncertain", "difficult", "expensive", "complex", "risky",
    "unregulated", "saturated", "crowded", "commodity",
]

SCALABILITY_WORDS = ["platform", "cloud", "subscription", "network effect", "marketplace", "api", "automated", "software", "app"]


def clean_text(text: str) -> str:
    text = text.lower()
    text = re.sub(r"http\S+|www\.\S+", " ", text)
    text = re.sub(r"[^a-z0-9\s\-]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def tokenize_and_lemmatize(text: str) -> List[str]:
    cleaned = clean_text(text)
    try:
        tokens = word_tokenize(cleaned)
    except LookupError:
        tokens = cleaned.split()
    tokens = [t for t in tokens if t not in _STOPWORDS and len(t) > 2]
    lemmas = [_LEMMATIZER.lemmatize(t) for t in tokens]
    return lemmas


def extract_keywords(text: str, top_n: int = 10) -> List[str]:
    lemmas = tokenize_and_lemmatize(text)
    freq = Counter(lemmas)
    return [w for w, _ in freq.most_common(top_n)]


def classify_industry(full_text: str) -> str:
    lowered = full_text.lower()
    scores: Dict[str, int] = {}
    for industry, keywords in INDUSTRY_KEYWORDS.items():
        score = sum(lowered.count(kw) for kw in keywords)
        if score:
            scores[industry] = score
    if not scores:
        return "Other"
    return max(scores.items(), key=lambda kv: kv[1])[0]


def _keyword_density_score(text: str, words: List[str], base: float = 50.0, weight: float = 6.0, cap: float = 45.0) -> float:
    lowered = text.lower()
    hits = sum(lowered.count(w) for w in words)
    return float(min(100.0, max(0.0, base + min(hits * weight, cap))))


def score_problem_severity(problem_text: str) -> float:
    """Heuristic 0-100 score for how acute/valuable the stated problem is."""
    urgent_words = ["waste", "loss", "expensive", "time-consuming", "manual", "error", "inefficient",
                    "delay", "fraud", "shortage", "risk", "failure", "difficult", "costly"]
    return _keyword_density_score(problem_text, urgent_words, base=45.0, weight=7.0, cap=50.0)


def score_uvp_strength(uvp_text: str, solution_text: str = "") -> float:
    combined = f"{uvp_text} {solution_text}"
    score = _keyword_density_score(combined, POSITIVE_SIGNAL_WORDS, base=48.0, weight=6.0, cap=45.0)
    penalty = sum(combined.lower().count(w) for w in NEGATIVE_SIGNAL_WORDS) * 5
    return float(max(0.0, min(100.0, score - penalty)))


def score_scalability_signal(solution_text: str, business_model_text: str = "") -> float:
    combined = f"{solution_text} {business_model_text}"
    return _keyword_density_score(combined, SCALABILITY_WORDS, base=42.0, weight=7.0, cap=48.0)


def score_business_model_clarity(revenue_model_text: str, business_model_text: str = "") -> float:
    combined = f"{revenue_model_text} {business_model_text}".strip()
    if not combined:
        return 40.0
    clarity_words = ["subscription", "commission", "freemium", "licensing", "advertising",
                      "transaction fee", "pay-per-use", "b2b", "b2c", "marketplace fee"]
    return _keyword_density_score(combined, clarity_words, base=50.0, weight=8.0, cap=40.0)


def analyze(
    startup_name: str,
    description: str,
    problem: str,
    solution: str,
    target_market: str,
    business_model: str,
    revenue_model: str = "",
    unique_value_proposition: str = "",
) -> dict:
    """Full NLP analysis used by both the /nlp/extract endpoint and internally
    before Random Forest prediction."""
    full_text = " ".join([description, problem, solution, target_market, business_model, unique_value_proposition])
    keywords = extract_keywords(full_text, top_n=12)
    industry = classify_industry(full_text)

    return {
        "startup_name": startup_name,
        "industry": industry,
        "keywords": keywords,
        "problem_summary": problem.strip()[:280],
        "target_customer_summary": target_market.strip()[:280],
        "solution_summary": solution.strip()[:280],
        "tokens_processed": len(tokenize_and_lemmatize(full_text)),
        "features": {
            "problem_severity": round(score_problem_severity(problem), 1),
            "unique_value_prop_score": round(score_uvp_strength(unique_value_proposition, solution), 1),
            "scalability_signal": round(score_scalability_signal(solution, business_model), 1),
            "business_model_clarity": round(score_business_model_clarity(revenue_model, business_model), 1),
        },
    }
