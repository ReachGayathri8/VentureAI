"""
VentureAI XGBoost Model Training

Trains:
    1. XGBoost Regressor  -> viability score (0-100)
    2. XGBoost Classifier -> viability classification

Run from ml-service:
    python -m training.train_model

Produces:
    model/viability_regressor.joblib
    model/viability_classifier.joblib
    model/industry_encoder.joblib
    model/classification_encoder.joblib
    model/feature_schema.joblib
    model/metrics.json

IMPORTANT:
    The current project uses generate_dataset.generate(), which produces
    synthetic training data. Model quality therefore depends on the quality
    of that generated data. For real-world performance, replace the generated
    dataset with a representative labelled startup dataset.
"""

from __future__ import annotations

import json
import os

import joblib
import numpy as np
import pandas as pd

from xgboost import XGBClassifier, XGBRegressor

from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    mean_absolute_error,
    mean_squared_error,
    precision_score,
    recall_score,
    r2_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

from training.generate_dataset import FEATURE_COLUMNS, INDUSTRIES, generate


# ---------------------------------------------------------
# Paths
# ---------------------------------------------------------

MODEL_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "model")
)


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

RANDOM_STATE = 42

TEST_SIZE = 0.20

N_SAMPLES = 6000


# ---------------------------------------------------------
# Training
# ---------------------------------------------------------

def train() -> None:

    print("=" * 70)
    print("VENTUREAI - XGBOOST TRAINING")
    print("=" * 70)

    os.makedirs(MODEL_DIR, exist_ok=True)

    # -----------------------------------------------------
    # 1. Generate / load dataset
    # -----------------------------------------------------

    print("\n[1/7] Preparing dataset...")

    df = generate(n_samples=N_SAMPLES)

    if df.empty:
        raise ValueError("Training dataset is empty.")

    required_columns = (
        ["industry"]
        + FEATURE_COLUMNS
        + ["viability_score", "classification"]
    )

    missing_columns = [
        column for column in required_columns
        if column not in df.columns
    ]

    if missing_columns:
        raise ValueError(
            f"Dataset is missing required columns: {missing_columns}"
        )

    print(f"Dataset size: {len(df)} rows")
    print(f"Features: {len(FEATURE_COLUMNS)}")


    # -----------------------------------------------------
    # 2. Clean dataset
    # -----------------------------------------------------

    print("\n[2/7] Cleaning dataset...")

    # Replace infinite values
    df = df.replace([np.inf, -np.inf], np.nan)

    # Remove rows with missing required values
    before = len(df)

    df = df.dropna(
        subset=FEATURE_COLUMNS
        + ["industry", "viability_score", "classification"]
    )

    removed = before - len(df)

    if removed > 0:
        print(f"Removed {removed} invalid rows.")

    if len(df) < 100:
        raise ValueError(
            "Not enough valid training samples after cleaning."
        )


    # -----------------------------------------------------
    # 3. Encode categorical variables
    # -----------------------------------------------------

    print("\n[3/7] Encoding categorical variables...")

    industry_encoder = LabelEncoder()

    industry_encoder.fit(INDUSTRIES)

    # Protect against an unexpected industry value
    unknown_industries = set(df["industry"]) - set(
        industry_encoder.classes_
    )

    if unknown_industries:
        raise ValueError(
            f"Unknown industries found in dataset: "
            f"{unknown_industries}"
        )

    df["industry_encoded"] = industry_encoder.transform(
        df["industry"]
    )

    # Classification labels
    classification_encoder = LabelEncoder()

    df["classification_encoded"] = (
        classification_encoder.fit_transform(
            df["classification"]
        )
    )

    feature_cols = [
        "industry_encoded"
    ] + FEATURE_COLUMNS

    X = df[feature_cols].astype(float)

    y_reg = df["viability_score"].astype(float)

    y_clf = df["classification_encoded"].astype(int)


    # -----------------------------------------------------
    # 4. Train/test split
    # -----------------------------------------------------

    print("\n[4/7] Creating train/test split...")

    (
        X_train,
        X_test,
        yreg_train,
        yreg_test,
        yclf_train,
        yclf_test,
    ) = train_test_split(
        X,
        y_reg,
        y_clf,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=y_clf,
    )

    print(f"Training samples: {len(X_train)}")
    print(f"Testing samples : {len(X_test)}")


    # -----------------------------------------------------
    # 5. XGBoost Regression Model
    # -----------------------------------------------------

    print("\n[5/7] Training XGBoost regressor...")

    regressor = XGBRegressor(
        n_estimators=500,
        max_depth=6,
        learning_rate=0.04,
        subsample=0.85,
        colsample_bytree=0.85,
        min_child_weight=4,
        reg_alpha=0.05,
        reg_lambda=1.5,
        objective="reg:squarederror",
        eval_metric="rmse",
        random_state=RANDOM_STATE,
        n_jobs=-1,
        tree_method="hist",
    )

    regressor.fit(
        X_train,
        yreg_train,
        eval_set=[(X_test, yreg_test)],
        verbose=False,
    )

    pred_reg = regressor.predict(X_test)

    # Keep score in valid range
    pred_reg = np.clip(pred_reg, 0, 100)


    # -----------------------------------------------------
    # 6. XGBoost Classification Model
    # -----------------------------------------------------

    print("\n[6/7] Training XGBoost classifier...")

    number_of_classes = len(
        classification_encoder.classes_
    )

    classifier = XGBClassifier(
        n_estimators=500,
        max_depth=5,
        learning_rate=0.04,
        subsample=0.85,
        colsample_bytree=0.85,
        min_child_weight=4,
        reg_alpha=0.05,
        reg_lambda=1.5,
        objective="multi:softprob",
        num_class=number_of_classes,
        eval_metric="mlogloss",
        random_state=RANDOM_STATE,
        n_jobs=-1,
        tree_method="hist",
    )

    classifier.fit(
        X_train,
        yclf_train,
        eval_set=[(X_test, yclf_test)],
        verbose=False,
    )

    pred_clf = classifier.predict(X_test).astype(int)

    pred_probability = classifier.predict_proba(X_test)


    # -----------------------------------------------------
    # 7. Evaluation
    # -----------------------------------------------------

    print("\n[7/7] Evaluating models...")

    regression_mae = mean_absolute_error(
        yreg_test,
        pred_reg,
    )

    regression_rmse = np.sqrt(
        mean_squared_error(
            yreg_test,
            pred_reg,
        )
    )

    regression_r2 = r2_score(
        yreg_test,
        pred_reg,
    )

    classifier_accuracy = accuracy_score(
        yclf_test,
        pred_clf,
    )

    classifier_precision = precision_score(
        yclf_test,
        pred_clf,
        average="weighted",
        zero_division=0,
    )

    classifier_recall = recall_score(
        yclf_test,
        pred_clf,
        average="weighted",
        zero_division=0,
    )

    classifier_f1 = f1_score(
        yclf_test,
        pred_clf,
        average="weighted",
        zero_division=0,
    )

    # Multiclass ROC-AUC
    try:
        classifier_roc_auc = roc_auc_score(
            yclf_test,
            pred_probability,
            multi_class="ovr",
            average="weighted",
        )
    except ValueError:
        classifier_roc_auc = None


    # -----------------------------------------------------
    # Feature importance
    # -----------------------------------------------------

    feature_importance = dict(
        zip(
            feature_cols,
            regressor.feature_importances_.round(6).tolist(),
        )
    )

    # Sort by importance
    feature_importance = dict(
        sorted(
            feature_importance.items(),
            key=lambda item: item[1],
            reverse=True,
        )
    )


    # -----------------------------------------------------
    # Confusion matrix
    # -----------------------------------------------------

    cm = confusion_matrix(
        yclf_test,
        pred_clf,
    )

    confusion_matrix_data = cm.tolist()


    # -----------------------------------------------------
    # Classification report
    # -----------------------------------------------------

    report = classification_report(
        yclf_test,
        pred_clf,
        target_names=classification_encoder.classes_,
        output_dict=True,
        zero_division=0,
    )


    # -----------------------------------------------------
    # Metrics
    # -----------------------------------------------------

    metrics = {

        "model": "XGBoost",

        "model_type": {
            "regressor": "XGBRegressor",
            "classifier": "XGBClassifier",
        },

        "dataset": {
            "source": "generated_dataset",
            "samples": int(len(df)),
            "train_samples": int(len(X_train)),
            "test_samples": int(len(X_test)),
        },

        "regression": {
            "mae": float(regression_mae),
            "rmse": float(regression_rmse),
            "r2": float(regression_r2),
        },

        "classification": {
            "accuracy": float(classifier_accuracy),
            "precision_weighted": float(classifier_precision),
            "recall_weighted": float(classifier_recall),
            "f1_weighted": float(classifier_f1),
            "roc_auc_weighted_ovr": (
                None
                if classifier_roc_auc is None
                else float(classifier_roc_auc)
            ),
        },

        "classification_labels": (
            classification_encoder.classes_.tolist()
        ),

        "confusion_matrix": confusion_matrix_data,

        "classification_report": report,

        "feature_importances": feature_importance,

        "configuration": {
            "random_state": RANDOM_STATE,
            "test_size": TEST_SIZE,
            "n_estimators": 500,
            "max_depth_regressor": 6,
            "max_depth_classifier": 5,
            "learning_rate": 0.04,
            "subsample": 0.85,
            "colsample_bytree": 0.85,
        },

        "warning": (
            "Current training data is generated/synthetic. "
            "Evaluation metrics on generated data should not be "
            "interpreted as real-world startup success accuracy."
        ),
    }


    # -----------------------------------------------------
    # Save models
    # -----------------------------------------------------

    print("\nSaving trained models...")

    joblib.dump(
        regressor,
        os.path.join(
            MODEL_DIR,
            "viability_regressor.joblib",
        ),
    )

    joblib.dump(
        classifier,
        os.path.join(
            MODEL_DIR,
            "viability_classifier.joblib",
        ),
    )

    joblib.dump(
        industry_encoder,
        os.path.join(
            MODEL_DIR,
            "industry_encoder.joblib",
        ),
    )

    joblib.dump(
        classification_encoder,
        os.path.join(
            MODEL_DIR,
            "classification_encoder.joblib",
        ),
    )

    joblib.dump(
        feature_cols,
        os.path.join(
            MODEL_DIR,
            "feature_schema.joblib",
        ),
    )


    # -----------------------------------------------------
    # Save metrics
    # -----------------------------------------------------

    metrics_path = os.path.join(
        MODEL_DIR,
        "metrics.json",
    )

    with open(
        metrics_path,
        "w",
        encoding="utf-8",
    ) as f:

        json.dump(
            metrics,
            f,
            indent=2,
        )


    # -----------------------------------------------------
    # Console output
    # -----------------------------------------------------

    print("\n" + "=" * 70)
    print("XGBOOST TRAINING COMPLETE")
    print("=" * 70)

    print("\nRegression:")
    print(f"MAE  : {regression_mae:.4f}")
    print(f"RMSE : {regression_rmse:.4f}")
    print(f"R²   : {regression_r2:.4f}")

    print("\nClassification:")
    print(f"Accuracy : {classifier_accuracy:.4f}")
    print(f"Precision: {classifier_precision:.4f}")
    print(f"Recall   : {classifier_recall:.4f}")
    print(f"F1       : {classifier_f1:.4f}")

    if classifier_roc_auc is not None:
        print(f"ROC-AUC  : {classifier_roc_auc:.4f}")

    print("\nFeature importance:")

    for feature, importance in feature_importance.items():
        print(
            f"  {feature:<35} "
            f"{importance:.4f}"
        )

    print("\nModels saved to:")
    print(MODEL_DIR)

    print("\nIMPORTANT:")
    print(
        "These metrics are based on generated/synthetic data."
    )


# ---------------------------------------------------------
# Entry point
# ---------------------------------------------------------

if __name__ == "__main__":
    train()