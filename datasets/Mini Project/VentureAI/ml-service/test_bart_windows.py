import torch
from transformers import BartTokenizer, BartForSequenceClassification

MODEL_PATH = "startup_bart_model"

print("Loading tokenizer...")
tokenizer = BartTokenizer.from_pretrained(MODEL_PATH)

print("Loading trained BART...")
model = BartForSequenceClassification.from_pretrained(MODEL_PATH)

device = torch.device("cpu")
model.to(device)
model.eval()

text = (
    "Industry: Technology. "
    "Initial investment: 500000 USD. "
    "Monthly operating cost: 100000 USD."
)

inputs = tokenizer(
    text,
    return_tensors="pt",
    truncation=True,
    max_length=256
)

inputs = {
    key: value.to(device)
    for key, value in inputs.items()
}

print("Running prediction...")

with torch.no_grad():
    logits = model(**inputs).logits
    probabilities = torch.softmax(logits, dim=-1)[0]

prediction = int(torch.argmax(probabilities))
viability_probability = float(probabilities[1]) * 100

print("================================")
print("BART WINDOWS TEST SUCCESS")
print("Prediction:", "VIABLE" if prediction == 1 else "NOT VIABLE")
print("Viability probability:", round(viability_probability, 2), "%")
print("Device:", device)
print("================================")
