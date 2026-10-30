"""
VentureAI ML Service

Architecture:
React Frontend
    ↓
Spring Boot Backend
    ↓
Python ML Service
    ↓
BART-base + Qwen + Flan-T5
"""

import os
import re
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from api.schemas import (
    PredictRequest,
    PredictResponse,
)

from model.bart_model import (
    load_bart_model,
    is_bart_loaded,
    is_bart_trained,
    bart_load_error,
    bart_device,
    BART_MODEL_DIR,
    predict_viability as run_bart_prediction,
)

from model.bart_text import build_bart_text

from model.generative.generative_models import (
    generate_qwen,
    generate_flant5,
)


app = FastAPI(
    title="VentureAI ML Service",
    description="AI Startup Validation and Business Intelligence ML Service",
    version="3.0.0",
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


BART_METRICS_PATH = os.path.join(
    BART_MODEL_DIR,
    "bart_metrics.json"
)


load_bart_model()


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "ventureai-ml-service",
        "models": {
            "bart": {
                "loaded": is_bart_loaded(),
                "trained": is_bart_trained(),
                "device": bart_device,
                "error": bart_load_error,
            },
            "qwen": "available",
            "flant5": "available",
        },
    }


@app.get("/metrics")
def metrics():
    if not os.path.exists(BART_METRICS_PATH):
        return {
            "model": "BART-base",
            "message": "Training metrics file not found"
        }

    import json

    with open(
        BART_METRICS_PATH,
        "r",
        encoding="utf-8"
    ) as f:
        return json.load(f)


@app.post("/nlp/extract")
def nlp_extract(payload: dict):

    text = " ".join(
        str(payload.get(key, ""))
        for key in [
            "startup_name",
            "description",
            "problem",
            "solution",
            "target_market",
            "business_model",
            "revenue_model",
            "unique_value_proposition",
        ]
    ).strip()

    if not text:
        raise HTTPException(
            status_code=400,
            detail="Text cannot be empty"
        )

    words = re.findall(
        r"\b[a-zA-Z][a-zA-Z0-9-]{2,}\b",
        text.lower()
    )

    stop_words = {
        "the",
        "and",
        "for",
        "with",
        "that",
        "this",
        "from",
        "are",
        "was",
        "will",
        "have",
        "has",
        "into",
        "your",
        "you",
        "our",
        "their",
        "startup",
        "business",
        "idea",
    }

    keywords = []

    for word in words:
        if word not in stop_words and word not in keywords:
            keywords.append(word)

    return {
        "text": text,
        "keywords": keywords[:20],
    }
@app.post(
    "/predict",
    response_model=PredictResponse
)
def predict(payload: PredictRequest):

    if not payload.industry:
        raise HTTPException(
            status_code=400,
            detail="Industry is required"
        )

    if not is_bart_loaded():
        raise HTTPException(
            status_code=503,
            detail="BART model is not loaded"
        )

    text = build_bart_text(
        industry=payload.industry,
        initial_investment=payload.initial_investment,
        monthly_operating_cost=payload.monthly_operating_cost,
    )

    result = run_bart_prediction(text)

    return {
        "score": result["score"],
        "classification": result["classification"],
        "confidence": result["confidence"],
    }


@app.post("/generate/qwen")
def generate_with_qwen(prompt: str):

    if not prompt or not prompt.strip():
        raise HTTPException(
            status_code=400,
            detail="Prompt cannot be empty"
        )

    try:
        result = generate_qwen(prompt)

        return {
            "model": "Qwen3-1.7B",
            "response": result,
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


@app.post("/generate/flant5")
def generate_with_flant5(prompt: str):

    if not prompt or not prompt.strip():
        raise HTTPException(
            status_code=400,
            detail="Prompt cannot be empty"
        )

    try:
        result = generate_flant5(prompt)

        return {
            "model": "Flan-T5-base",
            "response": result,
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


@app.get("/")
def root():
    return {
        "service": "VentureAI ML Service",
        "status": "running",
        "models": [
            "BART-base",
            "Qwen3-1.7B",
            "Flan-T5-base",
        ],
        "bart_loaded": is_bart_loaded(),
        "device": bart_device,
    }
