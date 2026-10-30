import torch
from transformers import AutoTokenizer, AutoModelForCausalLM
from transformers import AutoModelForSeq2SeqLM

QWEN_MODEL = "Qwen/Qwen3-1.7B"
FLANT5_MODEL = "google/flan-t5-base"

device = "cuda" if torch.cuda.is_available() else "cpu"

qwen_tokenizer = None
qwen_model = None

flant5_tokenizer = None
flant5_model = None


def load_qwen():
    global qwen_tokenizer, qwen_model

    qwen_tokenizer = AutoTokenizer.from_pretrained(QWEN_MODEL)
    qwen_model = AutoModelForCausalLM.from_pretrained(
        QWEN_MODEL
    )

    qwen_model.to(device)
    qwen_model.eval()

    print("Qwen loaded successfully")
    print("Qwen device:", device)


def load_flant5():
    global flant5_tokenizer, flant5_model

    flant5_tokenizer = AutoTokenizer.from_pretrained(
        FLANT5_MODEL
    )

    flant5_model = AutoModelForSeq2SeqLM.from_pretrained(
        FLANT5_MODEL
    )

    flant5_model.to(device)
    flant5_model.eval()

    print("Flan-T5 loaded successfully")
    print("Flan-T5 device:", device)


def generate_qwen(prompt):
    if qwen_model is None:
        load_qwen()

    inputs = qwen_tokenizer(
        prompt,
        return_tensors="pt",
        truncation=True,
        max_length=1024
    )

    inputs = {
        key: value.to(device)
        for key, value in inputs.items()
    }

    with torch.no_grad():
        output = qwen_model.generate(
            **inputs,
            max_new_tokens=300,
            do_sample=True,
            temperature=0.7
        )

    return qwen_tokenizer.decode(
        output[0],
        skip_special_tokens=True
    )


def generate_flant5(prompt):
    if flant5_model is None:
        load_flant5()

    inputs = flant5_tokenizer(
        prompt,
        return_tensors="pt",
        truncation=True,
        max_length=512
    )

    inputs = {
        key: value.to(device)
        for key, value in inputs.items()
    }

    with torch.no_grad():
        output = flant5_model.generate(
            **inputs,
            max_new_tokens=200
        )

    return flant5_tokenizer.decode(
        output[0],
        skip_special_tokens=True
    )