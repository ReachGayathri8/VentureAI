"""
Trains the BART startup-viability classifier used by POST /predict
(see model/bart_model.py + model/bart_text.py + api/main.py).

TRAINING / INFERENCE CONSISTENCY (read this first):

This script builds every training example's text with the exact same
function api/main.py calls at inference time:
    model.bart_text.build_bart_text(industry, initial_investment, monthly_operating_cost)
There is no separate, similar-looking text-building code here - only
that one shared function, imported below. See model/bart_text.py's
module docstring for the full reasoning behind restricting the shared
representation to these 3 fields (industry, initial_investment,
monthly_operating_cost) instead of all 12 of PredictRequest's fields.
Short version: those are the only 3 fields with a genuine,
unit-compatible counterpart in the real datasets below; the other 9
PredictRequest fields are subjective 0-100 founder self-ratings that
no available dataset contains in any form.

DATASETS USED, AND WHY (inspected before writing this):

  datasets/raw/startup_data.csv
      66,369 real Crunchbase-style company records with a real
      outcome (`status`: operating/acquired/ipo/closed).
      Maps to the shared fields as:
          industry               <- category_list
          initial_investment     <- funding_total_usd
          monthly_operating_cost <- Not specified (no such field here)
      (datasets/raw/big_startup_secsees_dataset.csv is byte-identical
      to this file - verified by hash - so it is intentionally not
      read a second time; reading both would silently double-count
      the same rows.)

  datasets/raw/startup_success_prediction_dataset.csv
      923 real companies (the well-known Kaggle "Startup Success
      Prediction" dataset), with a real binary outcome (`labels`:
      1=acquired, 0=closed).
      Maps to the shared fields as:
          industry               <- category_code
          initial_investment     <- funding_total_usd
          monthly_operating_cost <- Not specified (no such field here)

  datasets/raw/startup_success_dataset.csv
      100,000 rows with a real 3-class outcome (`outcome`:
      Failure/Acquisition/IPO).
      Maps to the shared fields as:
          industry               <- sector
          initial_investment     <- Not specified (no total-funding
                                     figure in this dataset - only a
                                     round COUNT and a burn/revenue
                                     rate, which are not the same
                                     thing as a total invested amount)
          monthly_operating_cost <- burn_rate_million * 1,000,000
                                     (a genuine monthly cash-outflow
                                     figure, unit-converted from
                                     millions to plain USD to match
                                     PredictRequest's units)

  datasets/raw/Listofstartups.csv (241 rows, real company profiles)
  and datasets/raw/startup_funding.csv (3,044 rows, funding events)
      Neither has an outcome/success column, and neither has a
      reliable, row-level relationship to any of the three labeled
      datasets above (no shared company identifier). Per this
      project's own rule not to invent a label or a relationship that
      doesn't exist, NEITHER is used for training. They remain in
      datasets/raw/, untouched and unused.

None of the original CSV files under datasets/raw/ are modified by
this script. The only files this script writes are the combined,
derived training set (see DERIVED_DATA_PATH below) and the model
checkpoint/metrics under OUTPUT_DIR.

TARGET / NO LEAKAGE:

Each labeled source's own real outcome column (status / labels /
outcome) is used ONLY to compute the binary `label` column
(1=Viable, 0=Not Viable). None of those columns - and none of any
other field not explicitly listed above - are ever written into the
text BART reads.

  startup_data.csv:      status in {operating, acquired, ipo} -> 1, closed -> 0
  ...prediction_dataset: labels == 1 -> 1 (acquired), labels == 0 -> 0 (closed)
  ...success_dataset:    outcome in {Acquisition, IPO} -> 1, Failure -> 0

SMOKE TEST vs FULL TRAINING:

  Smoke test (fast, small sample, 1 epoch - verifies the pipeline
  runs end-to-end, NOT a real trained model):
      (Windows CMD)   set BART_SMOKE_TEST=1 && python train_bart.py

  Full training (the real thing; slow on CPU as you've already seen -
  run this once CUDA is working):
      python train_bart.py

Either mode saves the fine-tuned model to OUTPUT_DIR
("startup_bart_model", resolved relative to this script's own
location so it works regardless of the current working directory on
Windows) - exactly where model/bart_model.py looks for a trained
checkpoint. api/main.py's /predict starts using it automatically the
next time the FastAPI service is restarted.
"""

import json
import os
import sys

import numpy as np
import pandas as pd
import torch
import torch.nn as nn

from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_recall_fscore_support,
    classification_report,
    confusion_matrix,
)
from sklearn.utils.class_weight import compute_class_weight

from datasets import Dataset

from transformers import (
    BartTokenizer,
    BartForSequenceClassification,
    Trainer,
    TrainingArguments,
)

# Make sure this script can be run from any working directory on
# Windows (e.g. double-clicked, or invoked from a parent folder) and
# still find the `model` package next to it.
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

# The SAME function api/main.py uses for POST /predict. This is the
# one piece of code responsible for training/inference consistency -
# see its module docstring for the full reasoning.
from model.bart_text import build_bart_text


# ============================================================
# CONFIGURATION
# ============================================================

SMOKE_TEST = os.environ.get("BART_SMOKE_TEST", "0") == "1"

# Rows sampled from EACH labeled source (not a total). A source with
# fewer real rows than this just contributes all of them.
N_PER_SOURCE = 40 if SMOKE_TEST else 2000
NUM_EPOCHS = 1 if SMOKE_TEST else 3

MODEL_NAME = "facebook/bart-base"

# All paths anchored to this script's own directory, not the current
# working directory - robust on Windows regardless of how/where the
# script is invoked from.
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "startup_bart_model")
DATASETS_DIR = os.path.join(SCRIPT_DIR, "datasets", "raw")
TRAINING_DIR = os.path.join(SCRIPT_DIR, "training")

CRUNCHBASE_PATH = os.path.join(DATASETS_DIR, "startup_data.csv")
KAGGLE_923_PATH = os.path.join(DATASETS_DIR, "startup_success_prediction_dataset.csv")
SUCCESS_100K_PATH = os.path.join(DATASETS_DIR, "startup_success_dataset.csv")

DERIVED_DATA_PATH = os.path.join(TRAINING_DIR, "real_startups_bart_shared_text.csv")

MAX_LENGTH = 256

RANDOM_STATE = 42


# ============================================================
# START
# ============================================================

print("=" * 70)
print("VENTUREAI - BART STARTUP VIABILITY TRAINING (real datasets,")
print("shared text construction with POST /predict)")
if SMOKE_TEST:
    print("(SMOKE TEST MODE - small/fast run to verify the pipeline. ")
    print(" This does NOT produce a real trained startup-viability model.)")
print("=" * 70)

print("\nPyTorch version:", torch.__version__)
print("CUDA available:", torch.cuda.is_available())
print("Rows sampled per source:", N_PER_SOURCE, "| Epochs:", NUM_EPOCHS)


# ============================================================
# 1. SOURCE A - datasets/raw/startup_data.csv (Crunchbase, real)
# ============================================================

print(f"\nLoading {CRUNCHBASE_PATH} ...")

crunchbase = pd.read_csv(CRUNCHBASE_PATH)
crunchbase["status"] = crunchbase["status"].astype(str).str.strip().str.lower()

status_mapping = {"operating": 1, "acquired": 1, "ipo": 1, "closed": 0}
crunchbase["label"] = crunchbase["status"].map(status_mapping)
crunchbase = crunchbase.dropna(subset=["label"])
crunchbase["label"] = crunchbase["label"].astype(int)

n_a = min(N_PER_SOURCE, len(crunchbase))
crunchbase_sample = crunchbase.sample(n=n_a, random_state=RANDOM_STATE).copy()

crunchbase_sample["text"] = crunchbase_sample.apply(
    lambda row: build_bart_text(
        industry=row["category_list"],
        initial_investment=row["funding_total_usd"],
        monthly_operating_cost=None,  # not present in this dataset
    ),
    axis=1,
)
crunchbase_sample["source"] = "startup_data.csv"
crunchbase_sample = crunchbase_sample[["text", "label", "source"]]

print(f"Sampled {len(crunchbase_sample)} rows from startup_data.csv.")


# ============================================================
# 2. SOURCE B - startup_success_prediction_dataset.csv (Kaggle 923, real)
# ============================================================

print(f"\nLoading {KAGGLE_923_PATH} ...")

kaggle923 = pd.read_csv(KAGGLE_923_PATH)
kaggle923 = kaggle923.dropna(subset=["labels"])
kaggle923["label"] = kaggle923["labels"].astype(int)

n_b = min(N_PER_SOURCE, len(kaggle923))
kaggle923_sample = kaggle923.sample(n=n_b, random_state=RANDOM_STATE).copy()

kaggle923_sample["text"] = kaggle923_sample.apply(
    lambda row: build_bart_text(
        industry=row["category_code"],
        initial_investment=row["funding_total_usd"],
        monthly_operating_cost=None,  # not present in this dataset
    ),
    axis=1,
)
kaggle923_sample["source"] = "startup_success_prediction_dataset.csv"
kaggle923_sample = kaggle923_sample[["text", "label", "source"]]

print(f"Sampled {len(kaggle923_sample)} rows from startup_success_prediction_dataset.csv "
      f"(only {len(kaggle923)} real labeled rows exist in total).")


# ============================================================
# 3. SOURCE C - startup_success_dataset.csv (100k, real)
# ============================================================

print(f"\nLoading {SUCCESS_100K_PATH} ...")

success100k = pd.read_csv(SUCCESS_100K_PATH)
success100k["label"] = success100k["outcome"].isin(["Acquisition", "IPO"]).astype(int)

n_c = min(N_PER_SOURCE, len(success100k))
success100k_sample = success100k.sample(n=n_c, random_state=RANDOM_STATE).copy()

success100k_sample["text"] = success100k_sample.apply(
    lambda row: build_bart_text(
        industry=row["sector"],
        initial_investment=None,  # no total-funding figure in this dataset
        monthly_operating_cost=row["burn_rate_million"] * 1_000_000,
    ),
    axis=1,
)
success100k_sample["source"] = "startup_success_dataset.csv"
success100k_sample = success100k_sample[["text", "label", "source"]]

print(f"Sampled {len(success100k_sample)} rows from startup_success_dataset.csv.")


# ============================================================
# 4. COMBINE (pooled by shared text schema, not column-merged)
# ============================================================

data = pd.concat(
    [crunchbase_sample, kaggle923_sample, success100k_sample],
    ignore_index=True,
)
data = data.sample(frac=1.0, random_state=RANDOM_STATE).reset_index(drop=True)

os.makedirs(TRAINING_DIR, exist_ok=True)
data.to_csv(DERIVED_DATA_PATH, index=False)
print(f"\nSaved combined derived training set to: {DERIVED_DATA_PATH}")

print("\nRows per source:")
print(data["source"].value_counts())

print("\nLabel distribution (0=Not Viable, 1=Viable):")
print(data["label"].value_counts())

print("\nExample inputs (note: identical shape to what POST /predict sends BART):")
print("-" * 70)
for i in range(min(3, len(data))):
    print(data["text"].iloc[i])
print("-" * 70)

data = data[["text", "label"]]


# ============================================================
# 5. TRAIN / TEST SPLIT
# ============================================================

print("\nSplitting dataset...")

train_df, test_df = train_test_split(
    data,
    test_size=0.20,
    random_state=RANDOM_STATE,
    stratify=data["label"],
)

print("Training samples:", len(train_df))
print("Testing samples:", len(test_df))


# ============================================================
# 5b. CLASS WEIGHTS (handles label imbalance across pooled sources)
# ============================================================

class_weights = compute_class_weight(
    class_weight="balanced",
    classes=np.array([0, 1]),
    y=train_df["label"].values,
)

print("\nComputed class weights (0=Not Viable, 1=Viable):")
print(class_weights)

class_weights_tensor = torch.tensor(class_weights, dtype=torch.float)


# ============================================================
# 6. CONVERT TO HUGGINGFACE DATASETS
# ============================================================

train_dataset = Dataset.from_pandas(train_df, preserve_index=False)
test_dataset = Dataset.from_pandas(test_df, preserve_index=False)


# ============================================================
# 7. LOAD BART TOKENIZER
# ============================================================

print("\nLoading BART tokenizer...")

tokenizer = BartTokenizer.from_pretrained(MODEL_NAME)


# ============================================================
# 8. TOKENIZE
# ============================================================

def tokenize_function(examples):
    return tokenizer(
        examples["text"],
        padding="max_length",
        truncation=True,
        max_length=MAX_LENGTH,
    )


print("Tokenizing training dataset...")
train_dataset = train_dataset.map(tokenize_function, batched=True, desc="Tokenizing train data")

print("Tokenizing test dataset...")
test_dataset = test_dataset.map(tokenize_function, batched=True, desc="Tokenizing test data")


# ============================================================
# 9. RENAME LABEL / DROP RAW TEXT
# ============================================================

train_dataset = train_dataset.rename_column("label", "labels")
test_dataset = test_dataset.rename_column("label", "labels")

train_dataset = train_dataset.remove_columns(["text"])
test_dataset = test_dataset.remove_columns(["text"])


# ============================================================
# 10. LOAD BART MODEL
# ============================================================

print("\nLoading BART model...")
print("NOTE: 'classification_head' weights being newly initialized")
print("is EXPECTED here - facebook/bart-base has no startup")
print("classification head yet. Training below is what teaches it.")

model = BartForSequenceClassification.from_pretrained(
    MODEL_NAME,
    num_labels=2,
)


# ============================================================
# 11. METRICS
# ============================================================

def compute_metrics(eval_prediction):

    predictions = eval_prediction.predictions
    labels = eval_prediction.label_ids

    predicted_labels = np.argmax(predictions, axis=1)

    accuracy = accuracy_score(labels, predicted_labels)

    precision_macro, recall_macro, f1_macro, _ = precision_recall_fscore_support(
        labels, predicted_labels, average="macro", zero_division=0
    )

    precision_per_class, recall_per_class, f1_per_class, _ = precision_recall_fscore_support(
        labels, predicted_labels, average=None, labels=[0, 1], zero_division=0
    )

    return {
        "accuracy": accuracy,
        "precision_macro": precision_macro,
        "recall_macro": recall_macro,
        "f1_macro": f1_macro,
        "precision_not_viable": precision_per_class[0],
        "recall_not_viable": recall_per_class[0],
        "f1_not_viable": f1_per_class[0],
        "precision_viable": precision_per_class[1],
        "recall_viable": recall_per_class[1],
        "f1_viable": f1_per_class[1],
    }


# ============================================================
# 12. TRAINING ARGUMENTS
# ============================================================

training_args = TrainingArguments(
    output_dir=OUTPUT_DIR,
    eval_strategy="epoch",
    save_strategy="epoch",
    learning_rate=2e-5,
    per_device_train_batch_size=4,
    per_device_eval_batch_size=4,
    gradient_accumulation_steps=4,
    num_train_epochs=NUM_EPOCHS,
    weight_decay=0.01,
    logging_steps=10 if SMOKE_TEST else 100,
    save_total_limit=2,
    load_best_model_at_end=True,
    metric_for_best_model="f1_macro",
    greater_is_better=True,
    report_to="none",
    fp16=torch.cuda.is_available(),
    dataloader_num_workers=0,
)


# ============================================================
# 12b. WEIGHTED-LOSS TRAINER (class imbalance)
# ============================================================

class WeightedLossTrainer(Trainer):

    def compute_loss(self, model, inputs, return_outputs=False, num_items_in_batch=None):

        labels = inputs.pop("labels")
        outputs = model(**inputs)
        logits = outputs.logits

        loss_fct = nn.CrossEntropyLoss(weight=class_weights_tensor.to(logits.device))
        loss = loss_fct(logits.view(-1, model.config.num_labels), labels.view(-1))

        return (loss, outputs) if return_outputs else loss


# ============================================================
# 13. CREATE TRAINER
# ============================================================

trainer = WeightedLossTrainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=test_dataset,
    compute_metrics=compute_metrics,
)


# ============================================================
# 14. TRAIN
# ============================================================

print("\n")
print("=" * 70)
print("STARTING BART TRAINING")
print("=" * 70)

trainer.train()


# ============================================================
# 15. EVALUATION
# ============================================================

print("\n")
print("=" * 70)
print("EVALUATING MODEL")
print("=" * 70)

evaluation_results = trainer.evaluate()

print("\nEvaluation results:")
for key, value in evaluation_results.items():
    print(f"{key}: {value}")


# ============================================================
# 16. PREDICTIONS
# ============================================================

print("\nGenerating predictions...")

prediction_output = trainer.predict(test_dataset)
predictions = np.argmax(prediction_output.predictions, axis=1)
actual = prediction_output.label_ids


# ============================================================
# 17. CLASSIFICATION REPORT
# ============================================================

print("\n")
print("=" * 70)
print("CLASSIFICATION REPORT")
print("=" * 70)

report_text = classification_report(
    actual, predictions, target_names=["Not Viable", "Viable"]
)
print(report_text)

report_dict = classification_report(
    actual, predictions, target_names=["Not Viable", "Viable"], output_dict=True
)


# ============================================================
# 18. CONFUSION MATRIX
# ============================================================

conf_matrix = confusion_matrix(actual, predictions)
print("\nConfusion Matrix:")
print(conf_matrix)


# ============================================================
# 19. FINAL ACCURACY
# ============================================================

accuracy = accuracy_score(actual, predictions)
print("\nFinal Accuracy:")
print(f"{accuracy:.4f}")
print(f"{accuracy * 100:.2f}%")


# ============================================================
# 20. SAVE MODEL
# ============================================================

print("\n")
print("=" * 70)
print("SAVING BART MODEL")
print("=" * 70)

os.makedirs(OUTPUT_DIR, exist_ok=True)

trainer.save_model(OUTPUT_DIR)
tokenizer.save_pretrained(OUTPUT_DIR)

print("\nModel saved successfully!")
print(f"Location: {OUTPUT_DIR}")


# ============================================================
# 21. SAVE METRICS (read by GET /metrics in api/main.py)
# ============================================================

bart_metrics = {
    "model": "BART",
    "trained": True,
    "loaded": True,
    "smoke_test": SMOKE_TEST,
    "base_checkpoint": MODEL_NAME,
    "num_training_samples": len(train_df),
    "num_eval_samples": len(test_df),
    "num_epochs": NUM_EPOCHS,
    "shared_text_fields": ["industry", "initial_investment", "monthly_operating_cost"],
    "data_sources": {
        "startup_data.csv": int(len(crunchbase_sample)),
        "startup_success_prediction_dataset.csv": int(len(kaggle923_sample)),
        "startup_success_dataset.csv": int(len(success100k_sample)),
    },
    "data_source_notes": (
        "Trained on real company records pooled from 3 labeled datasets, "
        "each with its own real outcome column mapped to Viable/Not "
        "Viable and never included as an input feature. Text built with "
        "the SAME model.bart_text.build_bart_text() function POST "
        "/predict uses, restricted to the 3 fields with a genuine "
        "counterpart in these datasets (industry, initial_investment, "
        "monthly_operating_cost) - see model/bart_text.py for why the "
        "other 9 PredictRequest fields are not used. "
        "big_startup_secsees_dataset.csv was skipped as an exact "
        "duplicate of startup_data.csv. Listofstartups.csv and "
        "startup_funding.csv were not used - neither has an outcome "
        "column or a reliable row-level relationship to a labeled "
        f"record. See derived file: {DERIVED_DATA_PATH}"
    ),
    "evaluation": evaluation_results,
    "classification_report": report_dict,
    "confusion_matrix": conf_matrix.tolist(),
    "final_accuracy": float(accuracy),
}

metrics_path = os.path.join(OUTPUT_DIR, "bart_metrics.json")

with open(metrics_path, "w", encoding="utf-8") as f:
    json.dump(bart_metrics, f, indent=2, default=str)

print(f"\nSaved metrics to: {metrics_path}")


# ============================================================
# 22. TEST SINGLE PREDICTION (uses the SAME shared text function)
# ============================================================

print("\n")
print("=" * 70)
print("TESTING SAVED MODEL")
print("=" * 70)

test_text = build_bart_text(
    industry="Technology",
    initial_investment=500000,
    monthly_operating_cost=100000,
)
print("\nTest input text:", test_text)

inputs = tokenizer(
    test_text,
    return_tensors="pt",
    truncation=True,
    max_length=MAX_LENGTH,
)

model.eval()

with torch.no_grad():
    outputs = model(**inputs)
    probabilities = torch.softmax(outputs.logits, dim=1)
    prediction = torch.argmax(probabilities, dim=1).item()

result = "VIABLE" if prediction == 1 else "NOT VIABLE"

print("\nTest prediction:", result)
print("Viability probability:", f"{probabilities[0][1].item() * 100:.2f}%")

print("\n")
print("=" * 70)
print("BART TRAINING COMPLETE")
print("=" * 70)
