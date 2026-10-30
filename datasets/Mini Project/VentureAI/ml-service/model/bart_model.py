
import os
import torch
from transformers import BartForSequenceClassification, BartTokenizerFast

# Path to your existing trained model folder
BART_MODEL_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "startup_bart_model"
)

tokenizer = None
model = None
bart_load_error = None

bart_device = "cuda" if torch.cuda.is_available() else "cpu"


def load_bart_model():
    global tokenizer, model, bart_load_error

    try:
        if not os.path.isdir(BART_MODEL_DIR):
            raise FileNotFoundError(
                f"BART model directory not found: {BART_MODEL_DIR}"
            )

        tokenizer = BartTokenizerFast.from_pretrained(BART_MODEL_DIR)

        model = BartForSequenceClassification.from_pretrained(
            BART_MODEL_DIR
        )

        model.to(bart_device)
        model.eval()

        bart_load_error = None

        print("BART model loaded successfully")
        print("Model path:", BART_MODEL_DIR)
        print("Device:", bart_device)

        return True

    except Exception as e:
        tokenizer = None
        model = None
        bart_load_error = str(e)

        print("BART model loading failed:", e)
        return False


def is_bart_loaded():
    return model is not None and tokenizer is not None


def is_bart_trained():
    return os.path.isfile(
        os.path.join(BART_MODEL_DIR, "model.safetensors")
    )


def predict_viability(text):
    if not isinstance(text, str) or not text.strip():
        raise ValueError("Startup description cannot be empty.")

    if not is_bart_loaded():
        if not load_bart_model():
            raise RuntimeError(
                f"Could not load BART model: {bart_load_error}"
            )

    inputs = tokenizer(
        text.strip(),
        return_tensors="pt",
        truncation=True,
        max_length=128
    )

    inputs = {
        key: value.to(bart_device)
        for key, value in inputs.items()
    }

    with torch.inference_mode():
        outputs = model(**inputs)

    probabilities = torch.softmax(outputs.logits, dim=-1)[0]

    predicted_class = int(torch.argmax(probabilities).item())
    confidence = float(probabilities[predicted_class].item())

    classification = (
        "Viable" if predicted_class == 1
        else "Not Viable"
    )

    score = round(confidence * 100, 2)

    return {
        "score": score,
        "classification": classification,
        "confidence": score
    }


load_bart_model()
