"""
Train Failure Diagnosis Classifier
Predicts failure_category from payment + customer + context features.
"""

import pandas as pd
import numpy as np
import joblib
import os
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, f1_score

DATA_PATH = "data/payments.csv"
MODEL_DIR = "models"
os.makedirs(MODEL_DIR, exist_ok=True)

CATEGORICAL = ["payment_method", "bank", "merchant_category", "failure_code", 
               "device_type", "network_type", "location_zone", "day_of_week", "subscription_status"]
NUMERICAL = ["amount", "retry_count", "gateway_latency_ms", "customer_success_rate",
             "previous_failures_30d", "previous_successful_retries", "customer_tenure_days", "hour"]

TARGET = "failure_category"


def load_data():
    df = pd.read_csv(DATA_PATH)
    train = df[df["split"] == "train"]
    val = df[df["split"] == "val"]
    return train, val


def build_pipeline():
    preprocessor = ColumnTransformer([
        ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), CATEGORICAL),
        ("num", StandardScaler(), NUMERICAL)
    ])
    
    clf = RandomForestClassifier(
        n_estimators=200,
        max_depth=12,
        min_samples_leaf=5,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1
    )
    
    return Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", clf)
    ])


def main():
    train, val = load_data()
    
    X_train = train[CATEGORICAL + NUMERICAL]
    y_train = train[TARGET]
    X_val = val[CATEGORICAL + NUMERICAL]
    y_val = val[TARGET]
    
    pipeline = build_pipeline()
    pipeline.fit(X_train, y_train)
    
    y_pred = pipeline.predict(X_val)
    
    print("=" * 50)
    print("FAILURE DIAGNOSIS MODEL — VALIDATION RESULTS")
    print("=" * 50)
    print(f"F1-Score (macro): {f1_score(y_val, y_pred, average='macro'):.4f}")
    print(f"F1-Score (weighted): {f1_score(y_val, y_pred, average='weighted'):.4f}")
    print("\nClassification Report:")
    print(classification_report(y_val, y_pred))
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_val, y_pred))
    
    joblib.dump(pipeline, f"{MODEL_DIR}/diagnosis_model.pkl")
    print(f"\nModel saved to {MODEL_DIR}/diagnosis_model.pkl")


if __name__ == "__main__":
    main()
