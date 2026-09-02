"""
PayRecover AI — Synthetic Payment Dataset Generator
Generates 10,000 realistic failed payment events with ground-truth recovery probabilities.
"""

import pandas as pd
import numpy as np
import os
from datetime import datetime, timedelta

np.random.seed(42)

N = 10000
OUTPUT_DIR = "data"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# --- 1. Payment Methods & Distributions ---
PAYMENT_METHODS = ["UPI", "CARD", "NETBANKING", "WALLET"]
PAYMENT_METHOD_WEIGHTS = [0.45, 0.35, 0.15, 0.05]

BANKS = ["HDFC", "ICICI", "SBI", "Axis", "Kotak", "Yes", "PNB", "BOB"]
BANK_WEIGHTS = [0.20, 0.18, 0.16, 0.14, 0.12, 0.08, 0.07, 0.05]

MERCHANT_CATEGORIES = ["SaaS", "Ecommerce", "EdTech", "FoodDelivery", "Travel", "Gaming"]
MERCHANT_WEIGHTS = [0.25, 0.30, 0.15, 0.15, 0.10, 0.05]

DEVICE_TYPES = ["Android", "iOS", "Web", "Desktop"]
NETWORK_TYPES = ["4G", "5G", "WiFi", "3G"]
LOCATION_ZONES = ["Bangalore", "Mumbai", "Delhi", "Hyderabad", "Chennai", "Pune", "Kolkata"]

# --- 2. Failure Codes & Categories ---
FAILURE_CODES = {
    "BANK_TIMEOUT": "TEMPORARY",
    "GATEWAY_TIMEOUT": "TEMPORARY",
    "NETWORK_ERROR": "TEMPORARY",
    "UPI_TECHNICAL_ERROR": "TEMPORARY",
    "INSUFFICIENT_FUNDS": "CUSTOMER",
    "CARD_EXPIRED": "CUSTOMER",
    "LIMIT_EXCEEDED": "CUSTOMER",
    "AUTHENTICATION_FAILED": "CUSTOMER",
    "INVALID_ACCOUNT": "HARD",
    "BLOCKED_METHOD": "HARD",
    "PAYMENT_EXPIRED": "HARD",
    "CUSTOMER_DECLINED": "HARD",
}

FAILURE_CODE_LIST = list(FAILURE_CODES.keys())
FAILURE_WEIGHTS = [0.18, 0.15, 0.12, 0.10, 0.14, 0.08, 0.07, 0.06, 0.03, 0.03, 0.02, 0.02]

# --- 3. Ground Truth Recovery Probabilities per (Failure Code × Action) ---
ACTIONS = ["RETRY_NOW", "RETRY_LATER", "SWITCH_METHOD", "SEND_NOTIFICATION", "STOP"]

RECOVERY_PROBS = {
    # Temporary failures — high recovery potential
    "BANK_TIMEOUT":        {"RETRY_NOW": 0.55, "RETRY_LATER": 0.82, "SWITCH_METHOD": 0.61, "SEND_NOTIFICATION": 0.20, "STOP": 0.00},
    "GATEWAY_TIMEOUT":     {"RETRY_NOW": 0.52, "RETRY_LATER": 0.80, "SWITCH_METHOD": 0.60, "SEND_NOTIFICATION": 0.18, "STOP": 0.00},
    "NETWORK_ERROR":       {"RETRY_NOW": 0.48, "RETRY_LATER": 0.75, "SWITCH_METHOD": 0.58, "SEND_NOTIFICATION": 0.15, "STOP": 0.00},
    "UPI_TECHNICAL_ERROR": {"RETRY_NOW": 0.50, "RETRY_LATER": 0.78, "SWITCH_METHOD": 0.65, "SEND_NOTIFICATION": 0.17, "STOP": 0.00},
    
    # Customer-related failures — mixed recovery potential
    "INSUFFICIENT_FUNDS":  {"RETRY_NOW": 0.18, "RETRY_LATER": 0.44, "SWITCH_METHOD": 0.58, "SEND_NOTIFICATION": 0.72, "STOP": 0.05},
    "CARD_EXPIRED":        {"RETRY_NOW": 0.08, "RETRY_LATER": 0.12, "SWITCH_METHOD": 0.55, "SEND_NOTIFICATION": 0.65, "STOP": 0.15},
    "LIMIT_EXCEEDED":      {"RETRY_NOW": 0.22, "RETRY_LATER": 0.38, "SWITCH_METHOD": 0.48, "SEND_NOTIFICATION": 0.55, "STOP": 0.08},
    "AUTHENTICATION_FAILED":{"RETRY_NOW": 0.35, "RETRY_LATER": 0.50, "SWITCH_METHOD": 0.42, "SEND_NOTIFICATION": 0.60, "STOP": 0.10},
    
    # Hard failures — low recovery potential
    "INVALID_ACCOUNT":     {"RETRY_NOW": 0.02, "RETRY_LATER": 0.03, "SWITCH_METHOD": 0.10, "SEND_NOTIFICATION": 0.12, "STOP": 0.99},
    "BLOCKED_METHOD":      {"RETRY_NOW": 0.03, "RETRY_LATER": 0.04, "SWITCH_METHOD": 0.15, "SEND_NOTIFICATION": 0.18, "STOP": 0.95},
    "PAYMENT_EXPIRED":     {"RETRY_NOW": 0.01, "RETRY_LATER": 0.02, "SWITCH_METHOD": 0.05, "SEND_NOTIFICATION": 0.08, "STOP": 0.98},
    "CUSTOMER_DECLINED":   {"RETRY_NOW": 0.01, "RETRY_LATER": 0.01, "SWITCH_METHOD": 0.03, "SEND_NOTIFICATION": 0.05, "STOP": 0.99},
}

ACTION_COSTS = {
    "RETRY_NOW": 2.0,
    "RETRY_LATER": 2.0,
    "SWITCH_METHOD": 5.0,
    "SEND_NOTIFICATION": 8.0,
    "STOP": 0.0,
}

HOUR_WEIGHTS = np.array([
    0.02, 0.01, 0.01, 0.01, 0.01, 0.02, 0.04, 0.06, 0.07, 0.07,
    0.07, 0.07, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06, 0.05,
    0.05, 0.04, 0.03, 0.03
])
HOUR_WEIGHTS = HOUR_WEIGHTS / HOUR_WEIGHTS.sum()


def generate_dataset(n=N):
    records = []
    
    for i in range(n):
        payment_id = f"PAY_{i+1:06d}"
        customer_id = f"CUST_{np.random.randint(1, 5000):05d}"
        
        amount = int(np.random.lognormal(mean=6.5, sigma=1.2))
        amount = max(99, min(amount, 50000))
        
        payment_method = np.random.choice(PAYMENT_METHODS, p=PAYMENT_METHOD_WEIGHTS)
        bank = np.random.choice(BANKS, p=BANK_WEIGHTS)
        merchant_category = np.random.choice(MERCHANT_CATEGORIES, p=MERCHANT_WEIGHTS)
        
        days_ago = np.random.randint(0, 30)
        hour = np.random.choice(range(24), p=HOUR_WEIGHTS)
        minute = np.random.randint(0, 60)
        timestamp = datetime.now() - timedelta(days=int(days_ago), hours=int(24-hour), minutes=int(minute))
        
        failure_code = np.random.choice(FAILURE_CODE_LIST, p=FAILURE_WEIGHTS)
        failure_category = FAILURE_CODES[failure_code]
        retry_count = 0
        
        if failure_category == "TEMPORARY":
            gateway_latency_ms = int(np.random.normal(4200, 1500))
        else:
            gateway_latency_ms = int(np.random.normal(1800, 800))
        gateway_latency_ms = max(200, gateway_latency_ms)
        
        customer_hash = hash(customer_id) % 1000
        base_success_rate = 0.70 + (customer_hash / 1000) * 0.28
        customer_success_rate = round(np.clip(base_success_rate + np.random.normal(0, 0.05), 0.3, 0.99), 2)
        
        previous_failures_30d = max(0, int(np.random.poisson(2 * (1 - customer_success_rate))))
        previous_successful_retries = max(0, int(np.random.poisson(3 * customer_success_rate)))
        customer_tenure_days = np.random.randint(7, 1000)
        
        subscription_status = np.random.choice(["ACTIVE", "TRIAL", "CHURNED", "NONE"], p=[0.55, 0.20, 0.10, 0.15])
        
        day_of_week = timestamp.strftime("%A")
        device_type = np.random.choice(DEVICE_TYPES, p=[0.45, 0.30, 0.15, 0.10])
        network_type = np.random.choice(NETWORK_TYPES, p=[0.40, 0.20, 0.30, 0.10])
        location_zone = np.random.choice(LOCATION_ZONES)
        
        customer_quality_boost = (customer_success_rate - 0.70) * 0.15
        
        ground_truth = {}
        for action in ACTIONS:
            base_prob = RECOVERY_PROBS[failure_code][action]
            adjusted_prob = min(0.99, max(0.0, base_prob + customer_quality_boost))
            ground_truth[action] = round(adjusted_prob, 4)
        
        best_action = max(
            [a for a in ACTIONS if a != "STOP"],
            key=lambda a: amount * ground_truth[a] - ACTION_COSTS[a]
        )
        
        records.append({
            "payment_id": payment_id,
            "customer_id": customer_id,
            "amount": amount,
            "payment_method": payment_method,
            "bank": bank,
            "timestamp": timestamp.strftime("%Y-%m-%d %H:%M:%S"),
            "merchant_category": merchant_category,
            "payment_status": "FAILED",
            "failure_code": failure_code,
            "failure_category": failure_category,
            "retry_count": retry_count,
            "gateway_latency_ms": gateway_latency_ms,
            "customer_success_rate": customer_success_rate,
            "previous_failures_30d": previous_failures_30d,
            "previous_successful_retries": previous_successful_retries,
            "customer_tenure_days": customer_tenure_days,
            "subscription_status": subscription_status,
            "hour": hour,
            "day_of_week": day_of_week,
            "device_type": device_type,
            "network_type": network_type,
            "location_zone": location_zone,
            "gt_retry_now": ground_truth["RETRY_NOW"],
            "gt_retry_later": ground_truth["RETRY_LATER"],
            "gt_switch_method": ground_truth["SWITCH_METHOD"],
            "gt_send_notification": ground_truth["SEND_NOTIFICATION"],
            "gt_stop": ground_truth["STOP"],
            "gt_best_action": best_action,
        })
    
    df = pd.DataFrame(records)
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)
    n_train = int(0.70 * n)
    n_val = int(0.15 * n)
    
    df.loc[:n_train-1, "split"] = "train"
    df.loc[n_train:n_train+n_val-1, "split"] = "val"
    df.loc[n_train+n_val:, "split"] = "test"
    
    return df


if __name__ == "__main__":
    df = generate_dataset()
    df.to_csv(f"{OUTPUT_DIR}/payments.csv", index=False)
    print(f"Generated {len(df)} records")
    print(f"Train: {(df['split']=='train').sum()}")
    print(f"Val:   {(df['split']=='val').sum()}")
    print(f"Test:  {(df['split']=='test').sum()}")
    print("\nFailure category distribution:")
    print(df["failure_category"].value_counts())
    print("\nFailure code distribution:")
    print(df["failure_code"].value_counts())
    print("\nSample record:")
    print(df.iloc[0].to_string())
