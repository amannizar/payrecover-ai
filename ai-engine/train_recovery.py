"""
Train Recovery Probability Model
Predicts P(success | payment, action) for each candidate action.
"""

import pandas as pd
import numpy as np
import joblib
import os
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from scipy.special import expit, logit

DATA_PATH = "data/payments.csv"
MODEL_DIR = "models"
os.makedirs(MODEL_DIR, exist_ok=True)

BASE_CATEGORICAL = ["payment_method", "bank", "merchant_category", "failure_code", 
                    "failure_category", "device_type", "network_type", "location_zone", 
                    "day_of_week", "subscription_status"]
BASE_NUMERICAL = ["amount", "retry_count", "gateway_latency_ms", "customer_success_rate",
                  "previous_failures_30d", "previous_successful_retries", "customer_tenure_days", "hour"]

ALL_CATEGORICAL = BASE_CATEGORICAL + ["action"]
ALL_NUMERICAL = BASE_NUMERICAL

ACTIONS = ["RETRY_NOW", "RETRY_LATER", "SWITCH_METHOD", "SEND_NOTIFICATION", "STOP"]

GT_COLS = {
    "RETRY_NOW": "gt_retry_now",
    "RETRY_LATER": "gt_retry_later",
    "SWITCH_METHOD": "gt_switch_method",
    "SEND_NOTIFICATION": "gt_send_notification",
    "STOP": "gt_stop"
}


def prepare_training_data(df):
    rows = []
    for _, row in df.iterrows():
        for action in ACTIONS:
            r = row[BASE_CATEGORICAL + BASE_NUMERICAL].to_dict()
            r["action"] = action
            r["target_prob"] = row[GT_COLS[action]]
            rows.append(r)
    return pd.DataFrame(rows)


def main():
    df = pd.read_csv(DATA_PATH)
    train = df[df["split"] == "train"]
    val = df[df["split"] == "val"]
    
    print("Preparing expanded training data (one row per action)...")
    train_expanded = prepare_training_data(train)
    val_expanded = prepare_training_data(val)
    
    preprocessor = ColumnTransformer([
        ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), ALL_CATEGORICAL),
        ("num", StandardScaler(), ALL_NUMERICAL)
    ])
    
    reg = GradientBoostingRegressor(
        n_estimators=150,
        max_depth=5,
        learning_rate=0.1,
        random_state=42
    )
    
    pipeline = Pipeline([("preprocessor", preprocessor), ("regressor", reg)])
    
    y_train_raw = train_expanded["target_prob"].values
    y_train = logit(np.clip(y_train_raw, 0.001, 0.999))
    
    pipeline.fit(train_expanded[ALL_CATEGORICAL + ALL_NUMERICAL], y_train)
    
    y_pred_logit = pipeline.predict(val_expanded[ALL_CATEGORICAL + ALL_NUMERICAL])
    y_pred = expit(y_pred_logit)
    y_true = val_expanded["target_prob"].values
    
    print("=" * 50)
    print("RECOVERY PROBABILITY MODEL — VALIDATION RESULTS")
    print("=" * 50)
    print(f"MSE:  {mean_squared_error(y_true, y_pred):.6f}")
    print(f"MAE:  {mean_absolute_error(y_true, y_pred):.6f}")
    print(f"R²:   {r2_score(y_true, y_pred):.4f}")
    
    print("\nCalibration (binned):")
    bins = np.linspace(0, 1, 11)
    for i in range(len(bins)-1):
        mask = (y_pred >= bins[i]) & (y_pred < bins[i+1])
        if mask.sum() > 0:
            print(f"  Pred [{bins[i]:.1f}, {bins[i+1]:.1f}): Actual = {y_true[mask].mean():.3f}, Count = {mask.sum()}")
    
    joblib.dump(pipeline, f"{MODEL_DIR}/recovery_model.pkl")
    print(f"\nModel saved to {MODEL_DIR}/recovery_model.pkl")


if __name__ == "__main__":
    main()
