"""
PayRecover AI — Evaluation & Baseline Comparison
Runs all strategies on the held-out test set and reports business metrics.
"""

import pandas as pd
import numpy as np
import os
from simulation_engine import RecoveryAgent, BlindRetryAgent, RuleBasedAgent, AuditEvent

DATA_PATH = "data/payments.csv"
RESULTS_DIR = "data"
os.makedirs(RESULTS_DIR, exist_ok=True)


def evaluate_strategy(name, agent, df_test):
    print(f"\n{'='*60}")
    print(f"STRATEGY: {name}")
    print(f"{'='*60}")
    
    results = agent.run_batch(df_test)
    
    payment_summary = []
    for pid, group in results.groupby("payment_id"):
        success = (group["actual_result"] == "SUCCESS").any()
        recovered = group["recovered_amount"].sum()
        attempts = len(group)
        stopped = (group["actual_result"] == "STOPPED").any() or (group["stopping_reason"] != "").any()
        
        payment_summary.append({
            "payment_id": pid,
            "recovered": recovered,
            "success": success,
            "attempts": attempts,
            "stopped": stopped,
            "amount": group["amount"].iloc[0]
        })
    
    summary_df = pd.DataFrame(payment_summary)
    
    total_at_risk = summary_df["amount"].sum()
    total_recovered = summary_df["recovered"].sum()
    recovery_rate = summary_df["success"].mean() * 100
    revenue_recovery_rate = (total_recovered / total_at_risk) * 100 if total_at_risk > 0 else 0
    total_attempts = summary_df["attempts"].sum()
    efficiency = total_recovered / total_attempts if total_attempts > 0 else 0
    
    print(f"Total payments evaluated:     {len(summary_df)}")
    print(f"Total revenue at risk:        ₹{total_at_risk:,.0f}")
    print(f"Total recovered revenue:      ₹{total_recovered:,.0f}")
    print(f"Recovery rate (payments):     {recovery_rate:.1f}%")
    print(f"Revenue recovery rate:        {revenue_recovery_rate:.1f}%")
    print(f"Total recovery attempts:      {total_attempts}")
    print(f"Revenue per attempt:          ₹{efficiency:,.2f}")
    
    action_counts = results["selected_action"].value_counts()
    print(f"\nAction distribution:")
    for action, count in action_counts.items():
        print(f"  {action:20s}: {count:5d}")
    
    stop_reasons = results[results["stopping_reason"] != ""]["stopping_reason"].value_counts()
    if len(stop_reasons) > 0:
        print(f"\nStopping reasons:")
        for reason, count in stop_reasons.items():
            print(f"  {reason:30s}: {count:5d}")
    
    return {
        "strategy": name,
        "payments": len(summary_df),
        "revenue_at_risk": total_at_risk,
        "recovered_revenue": total_recovered,
        "recovery_rate": recovery_rate,
        "revenue_recovery_rate": revenue_recovery_rate,
        "attempts": total_attempts,
        "efficiency": efficiency,
        "results_df": results
    }


class NoRecoveryAgent:
    def run_batch(self, df):
        events = []
        for _, row in df.iterrows():
            events.append({
                "payment_id": row["payment_id"],
                "amount": row["amount"],
                "failure_code": row["failure_code"],
                "failure_category": row["failure_category"],
                "retry_count": 0,
                "ai_diagnosis": "NO_ACTION",
                "selected_action": "STOP",
                "expected_recovery": 0.0,
                "actual_result": "STOPPED",
                "recovered_amount": 0.0,
                "stopping_reason": "NO_RECOVERY_ATTEMPTED",
                "timestamp_step": 0
            })
        return pd.DataFrame(events)


def main():
    df = pd.read_csv(DATA_PATH)
    test_df = df[df["split"] == "test"].copy()
    
    print(f"Test set size: {len(test_df)} payments")
    print(f"Total revenue at risk in test set: ₹{test_df['amount'].sum():,.0f}")
    
    strategies = []
    
    strategies.append(evaluate_strategy("No Recovery", NoRecoveryAgent(), test_df))
    strategies.append(evaluate_strategy("Blind Retry (1x)", BlindRetryAgent(), test_df))
    strategies.append(evaluate_strategy("Rule-Based", RuleBasedAgent(), test_df))
    
    ai_agent = RecoveryAgent(
        diagnosis_model_path="models/diagnosis_model.pkl",
        recovery_model_path="models/recovery_model.pkl"
    )
    strategies.append(evaluate_strategy("PayRecover AI", ai_agent, test_df))
    
    print(f"\n{'='*60}")
    print("COMPARISON SUMMARY")
    print(f"{'='*60}")
    print(f"{'Strategy':<20} {'Recovered':>12} {'Rev Rec %':>10} {'Rate %':>8} {'Attempts':>10}")
    print("-" * 60)
    for s in strategies:
        print(f"{s['strategy']:<20} ₹{s['recovered_revenue']:>10,.0f} {s['revenue_recovery_rate']:>9.1f}% {s['recovery_rate']:>7.1f}% {s['attempts']:>10d}")
    
    for s in strategies:
        s["results_df"].to_csv(f"{RESULTS_DIR}/results_{s['strategy'].replace(' ', '_').lower()}.csv", index=False)
    
    print(f"\nDetailed results saved to {RESULTS_DIR}/")
    
    # Save summary
    summary = pd.DataFrame([{k: v for k, v in s.items() if k != "results_df"} for s in strategies])
    summary.to_csv(f"{RESULTS_DIR}/summary_comparison.csv", index=False)
    print("Summary saved to data/summary_comparison.csv")


if __name__ == "__main__":
    main()
