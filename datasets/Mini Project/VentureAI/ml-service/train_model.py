import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

# 1. Load dataset
df = pd.read_csv("datasets/raw/startup_data.csv")

print("Dataset shape:", df.shape)
print("\nColumns:")
print(df.columns.tolist())

# 2. Remove duplicate rows
df = df.drop_duplicates()

# 3. Remove rows with missing values
df = df.dropna()

# 4. Select target column
# CHANGE THIS after checking your dataset
TARGET = "status"

X = df.drop(columns=[TARGET])
y = df[TARGET]

# 5. Encode categorical columns
label_encoders = {}

for column in X.select_dtypes(include=["object"]).columns:
    encoder = LabelEncoder()
    X[column] = encoder.fit_transform(X[column].astype(str))
    label_encoders[column] = encoder

# Encode target if it is text
if y.dtype == "object":
    target_encoder = LabelEncoder()
    y = target_encoder.fit_transform(y.astype(str))
else:
    target_encoder = None

# 6. Split dataset
X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42,
    stratify=y
)

# 7. Train Random Forest
model = RandomForestClassifier(
    n_estimators=200,
    random_state=42,
    class_weight="balanced"
)

model.fit(X_train, y_train)

# 8. Evaluate
y_pred = model.predict(X_test)

accuracy = accuracy_score(y_test, y_pred)

print("\nAccuracy:", accuracy)
print("\nClassification Report:")
print(classification_report(y_test, y_pred))

# 9. Save model
joblib.dump(model, "startup_model.pkl")
joblib.dump(label_encoders, "label_encoders.pkl")

if target_encoder:
    joblib.dump(target_encoder, "target_encoder.pkl")

print("\nModel saved successfully!")