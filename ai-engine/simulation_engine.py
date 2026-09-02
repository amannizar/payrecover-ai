"""
PayRecover AI — Recovery Decision Agent & Simulator
Runs the agentic recovery loop with stopping rules and audit trail generation.
"""

import pandas as pd
import numpy as np
import joblib
import os
from dataclasses import dataclass, field, asdict
from typing import List, Dict
from scipy.special import expit

MODEL_DIR = "models"

ACTIONS = ["RETRY_NOW", "RETRY_LATER", "SWITCH_METHOD", "SEND_NOTIFICATION", "STOP"]
ACTION_COSTS = {
    "RETRY_NOW": 2.0,
    "RETRY_LATER": 2.0,
    "SWITCH_METHOD": 5.0,
    "SEND_NOTIFICATION": 8.0,
    "STOP": 0.0,
}

MAX_RETRIES = 2
MIN_RECOVERY_PROB = 0.20

GT_MAP = {
    "RETRY_NOW": "gt_retry_now",
    "RETRY_LATER": "gt_retry_later",
    "SWITCH_METHOD": "gt_switch_method",
    "SEND_NOTIFICATION": "gt_send_notification",
    "STOP": "gt_stop"
}


@dataclass
class AuditEvent:
    payment_id: str
    amount: float
    failure_code: str
    failure_category: str
    retry_count: int
    ai_diagnosis: str
    candidate_actions: Dict[str, float] = field(default_factory=dict)
    selected_action: str = ""
    expected_recovery: float = 0.0
    actual_result: str = ""
    recovered_amount: float = 0.0
    stopping_reason: str = ""
    timestamp_step: int = 0


class RecoveryAgent:
    def __init__(self, diagnosis_model_path=None, recovery_model_path=None):
        self.diagnosis_model = None
        self.recovery_model = None
        
        if diagnosis_model_path and os.path.exists(diagnosis_model_path):
            self.diagnosis_model = joblib.load(diagnosis_model_path)
        if recovery_model_path and os.path.exists(recovery_model_path):
            self.recovery_model = joblib.load(recovery_model_path)
        
        self.diag_cat = ["payment_method", "bank", "merchant_category", "failure_code", 
                         "device_type", "network_type", "location_zone", "day_of_week", "subscription_status"]
        self.diag_num = ["amount", "retry_count", "gateway_latency_ms", "customer_success_rate",
                         "previous_failures_30d", "previous_successful_retries", "customer_tenure_days", "hour"]
        
        self.rec_cat = ["payment_method", "bank", "merchant_category", "failure_code", "failure_category",
                        "device_type", "network_type", "location_zone", "day_of_week", "subscription_status", "action"]
        self.rec_num = self.diag_num
    
    def diagnose(self, payment: pd.Series) -> str:
        if self.diagnosis_model is None:
            return payment["failure_category"]
        X = pd.DataFrame([payment[self.diag_cat + self.diag_num]])
        return self.diagnosis_model.predict(X)[0]
    
    def predict_recovery_probs(self, payment: pd.Series, diagnosed_category: str = None) -> Dict[str, float]:
        if self.recovery_model is None:
            return {action: payment.get(GT_MAP[action], 0.3) for action in ACTIONS}
        
        probs = {}
        for action in ACTIONS:
            row = payment[self.rec_cat[:-1] + self.rec_num].copy()
            # Use the DIAGNOSED category, not the true one — this is what the agent would know
            if diagnosed_category is not None:
                row["failure_category"] = diagnosed_category
            row["action"] = action
            X = pd.DataFrame([row])
            logit_pred = self.recovery_model.predict(X)[0]
            probs[action] = float(np.clip(expit(logit_pred), 0.0, 0.99))
        return probs
    
    def decide_action(self, payment: pd.Series, retry_count: int,
                      customer_opted_out: bool = False,
                      payment_expired: bool = False,
                      risk_score: float = 0.0,
                      diagnosed_category: str = None) -> tuple:
        if customer_opted_out:
            return "STOP", 0.0, "CUSTOMER_OPTED_OUT", {}
        if payment_expired:
            return "STOP", 0.0, "PAYMENT_EXPIRED", {}
        if retry_count >= MAX_RETRIES:
            return "STOP", 0.0, "MAX_RETRIES_REACHED", {}
        if risk_score > 0.8:
            return "STOP", 0.0, "HIGH_RISK_SCORE", {}
        
        probs = self.predict_recovery_probs(payment, diagnosed_category)
        amount = payment["amount"]
        best_action = "STOP"
        best_ev = 0.0
        ev_map = {}
        
        for action in ACTIONS:
            if action == "STOP":
                ev = 0.0
            else:
                p_success = probs.get(action, 0.0)
                cost = ACTION_COSTS[action]
                ev = amount * p_success - cost
            ev_map[action] = ev
            
            if ev > best_ev and action != "STOP":
                if action in ["RETRY_NOW", "RETRY_LATER"] and probs.get(action, 0) < MIN_RECOVERY_PROB:
                    continue
                best_ev = ev
                best_action = action
        
        if best_action == "STOP" and retry_count == 0:
            non_stop = {a: ev for a, ev in ev_map.items() if a != "STOP"}
            if non_stop:
                best_action = max(non_stop, key=non_stop.get)
                best_ev = non_stop[best_action]
        
        if best_action == "STOP":
            return "STOP", 0.0, "NO_POSITIVE_EV", probs
        
        expected_recovery = amount * probs.get(best_action, 0.0)
        return best_action, expected_recovery, "", probs
    
    def simulate_outcome(self, payment: pd.Series, action: str) -> bool:
        p_success = payment.get(GT_MAP.get(action, "gt_stop"), 0.0)
        return np.random.random() < p_success
    
    def run_single_payment(self, payment: pd.Series) -> List[AuditEvent]:
        audit_trail = []
        retry_count = int(payment["retry_count"])
        
        payment_state = payment.copy()
        
        for step in range(5):
            diagnosis = self.diagnose(payment_state)
            action, expected_recovery, stop_reason, candidates = self.decide_action(
                payment_state, retry_count, diagnosed_category=diagnosis
            )
            
            event = AuditEvent(
                payment_id=payment_state["payment_id"],
                amount=payment_state["amount"],
                failure_code=payment_state["failure_code"],
                failure_category=diagnosis,
                retry_count=retry_count,
                ai_diagnosis=diagnosis,
                candidate_actions=candidates,
                selected_action=action,
                expected_recovery=expected_recovery,
                timestamp_step=step
            )
            
            if action == "STOP" or stop_reason:
                event.stopping_reason = stop_reason or "AGENT_STOPPED"
                event.actual_result = "STOPPED"
                audit_trail.append(event)
                break
            
            success = self.simulate_outcome(payment_state, action)
            
            if success:
                event.actual_result = "SUCCESS"
                event.recovered_amount = payment_state["amount"]
                audit_trail.append(event)
                break
            else:
                event.actual_result = "FAILED"
                event.recovered_amount = 0.0
                audit_trail.append(event)
                retry_count += 1
                payment_state = payment_state.copy()
                payment_state["retry_count"] = retry_count
        
        return audit_trail
    
    def run_batch(self, df: pd.DataFrame) -> pd.DataFrame:
        all_events = []
        for _, payment in df.iterrows():
            events = self.run_single_payment(payment)
            all_events.extend([asdict(e) for e in events])
        return pd.DataFrame(all_events)


class BlindRetryAgent:
    def run_single_payment(self, payment: pd.Series) -> List[AuditEvent]:
        success = np.random.random() < payment.get("gt_retry_now", 0.3)
        event = AuditEvent(
            payment_id=payment["payment_id"],
            amount=payment["amount"],
            failure_code=payment["failure_code"],
            failure_category=payment["failure_category"],
            retry_count=0,
            ai_diagnosis="BLIND_RETRY",
            selected_action="RETRY_NOW",
            expected_recovery=payment["amount"] * payment.get("gt_retry_now", 0.3),
            actual_result="SUCCESS" if success else "FAILED",
            recovered_amount=payment["amount"] if success else 0.0,
            stopping_reason="SINGLE_RETRY_EXHAUSTED" if not success else "",
            timestamp_step=0
        )
        return [event]
    
    def run_batch(self, df: pd.DataFrame) -> pd.DataFrame:
        all_events = []
        for _, payment in df.iterrows():
            events = self.run_single_payment(payment)
            all_events.extend([asdict(e) for e in events])
        return pd.DataFrame(all_events)


class RuleBasedAgent:
    def run_single_payment(self, payment: pd.Series) -> List[AuditEvent]:
        category = payment["failure_category"]
        
        if category == "TEMPORARY":
            action = "RETRY_LATER"
        elif category == "CUSTOMER":
            action = "SEND_NOTIFICATION"
        else:
            action = "STOP"
        
        if action == "STOP":
            event = AuditEvent(
                payment_id=payment["payment_id"],
                amount=payment["amount"],
                failure_code=payment["failure_code"],
                failure_category=category,
                retry_count=0,
                ai_diagnosis="RULE_BASED",
                selected_action="STOP",
                actual_result="STOPPED",
                stopping_reason="RULE_HARD_FAILURE",
                timestamp_step=0
            )
            return [event]
        
        gt_map = {"RETRY_LATER": "gt_retry_later", "SEND_NOTIFICATION": "gt_send_notification"}
        p = payment.get(gt_map.get(action, "gt_retry_now"), 0.3)
        success = np.random.random() < p
        
        event = AuditEvent(
            payment_id=payment["payment_id"],
            amount=payment["amount"],
            failure_code=payment["failure_code"],
            failure_category=category,
            retry_count=0,
            ai_diagnosis="RULE_BASED",
            selected_action=action,
            expected_recovery=payment["amount"] * p,
            actual_result="SUCCESS" if success else "FAILED",
            recovered_amount=payment["amount"] if success else 0.0,
            stopping_reason="SINGLE_ATTEMPT_EXHAUSTED" if not success else "",
            timestamp_step=0
        )
        return [event]
    
    def run_batch(self, df: pd.DataFrame) -> pd.DataFrame:
        all_events = []
        for _, payment in df.iterrows():
            events = self.run_single_payment(payment)
            all_events.extend([asdict(e) for e in events])
        return pd.DataFrame(all_events)


if __name__ == "__main__":
    df = pd.read_csv("data/payments.csv")
    test = df[df["split"] == "test"].head(100)
    
    agent = RecoveryAgent()
    results = agent.run_batch(test)
    print(f"Processed {len(test)} payments, {len(results)} audit events")
    print(f"Unique payments with SUCCESS: {results[results['actual_result']=='SUCCESS']['payment_id'].nunique()}")
