"""
PayRecover AI — FastAPI Service
Exposes trained ML models through REST APIs for the Spring Boot backend.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Dict, List, Optional
import pandas as pd
import numpy as np
import joblib
import os
from dataclasses import asdict
from simulation_engine import RecoveryAgent, AuditEvent, ACTIONS, ACTION_COSTS, MAX_RETRIES, MIN_RECOVERY_PROB

app = FastAPI(
    title="PayRecover AI Service",
    description="AI-Powered Payment Recovery & Revenue Protection Agent",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load models on startup
MODEL_DIR = "models"
agent = RecoveryAgent(
    diagnosis_model_path=os.path.join(MODEL_DIR, "diagnosis_model.pkl"),
    recovery_model_path=os.path.join(MODEL_DIR, "recovery_model.pkl"),
)


# ─── Request / Response Models ────────────────────────────────────────────────

class DiagnoseRequest(BaseModel):
    paymentMethod: str = Field(..., alias="paymentMethod")
    amount: float
    failureCode: str = Field(..., alias="failureCode")
    bank: str = "HDFC"
    merchantCategory: str = Field("SaaS", alias="merchantCategory")
    deviceType: str = Field("Android", alias="deviceType")
    networkType: str = Field("4G", alias="networkType")
    locationZone: str = Field("Bangalore", alias="locationZone")
    dayOfWeek: str = Field("Monday", alias="dayOfWeek")
    subscriptionStatus: str = Field("ACTIVE", alias="subscriptionStatus")
    retryCount: int = Field(0, alias="retryCount")
    gatewayLatencyMs: int = Field(2000, alias="gatewayLatencyMs")
    customerSuccessRate: float = Field(0.80, alias="customerSuccessRate")
    previousFailures30d: int = Field(0, alias="previousFailures30d")
    previousSuccessfulRetries: int = Field(2, alias="previousSuccessfulRetries")
    customerTenureDays: int = Field(365, alias="customerTenureDays")
    hour: int = 12

    model_config = {"populate_by_name": True}


class DiagnoseResponse(BaseModel):
    diagnosis: str
    confidence: float


class RecoveryProbRequest(BaseModel):
    paymentMethod: str = Field(..., alias="paymentMethod")
    amount: float
    failureCode: str = Field(..., alias="failureCode")
    failureCategory: str = Field("TEMPORARY", alias="failureCategory")
    bank: str = "HDFC"
    merchantCategory: str = Field("SaaS", alias="merchantCategory")
    deviceType: str = Field("Android", alias="deviceType")
    networkType: str = Field("4G", alias="networkType")
    locationZone: str = Field("Bangalore", alias="locationZone")
    dayOfWeek: str = Field("Monday", alias="dayOfWeek")
    subscriptionStatus: str = Field("ACTIVE", alias="subscriptionStatus")
    retryCount: int = Field(0, alias="retryCount")
    gatewayLatencyMs: int = Field(2000, alias="gatewayLatencyMs")
    customerSuccessRate: float = Field(0.80, alias="customerSuccessRate")
    previousFailures30d: int = Field(0, alias="previousFailures30d")
    previousSuccessfulRetries: int = Field(2, alias="previousSuccessfulRetries")
    customerTenureDays: int = Field(365, alias="customerTenureDays")
    hour: int = 12

    model_config = {"populate_by_name": True}


class RecoveryProbResponse(BaseModel):
    retryNow: float
    retryLater: float
    switchMethod: float
    notification: float
    stop: float


class DecideRequest(BaseModel):
    paymentMethod: str = Field(..., alias="paymentMethod")
    amount: float
    failureCode: str = Field(..., alias="failureCode")
    failureCategory: str = Field("TEMPORARY", alias="failureCategory")
    bank: str = "HDFC"
    merchantCategory: str = Field("SaaS", alias="merchantCategory")
    deviceType: str = Field("Android", alias="deviceType")
    networkType: str = Field("4G", alias="networkType")
    locationZone: str = Field("Bangalore", alias="locationZone")
    dayOfWeek: str = Field("Monday", alias="dayOfWeek")
    subscriptionStatus: str = Field("ACTIVE", alias="subscriptionStatus")
    retryCount: int = Field(0, alias="retryCount")
    gatewayLatencyMs: int = Field(2000, alias="gatewayLatencyMs")
    customerSuccessRate: float = Field(0.80, alias="customerSuccessRate")
    previousFailures30d: int = Field(0, alias="previousFailures30d")
    previousSuccessfulRetries: int = Field(2, alias="previousSuccessfulRetries")
    customerTenureDays: int = Field(365, alias="customerTenureDays")
    hour: int = 12
    customerOptedOut: bool = Field(False, alias="customerOptedOut")
    paymentExpired: bool = Field(False, alias="paymentExpired")
    riskScore: float = Field(0.0, alias="riskScore")

    model_config = {"populate_by_name": True}


class DecideResponse(BaseModel):
    action: str
    expectedRecovery: float
    stoppingReason: str
    probabilities: Dict[str, float]


class SimulateRequest(BaseModel):
    paymentMethod: str = Field(..., alias="paymentMethod")
    amount: float
    failureCode: str = Field(..., alias="failureCode")
    failureCategory: str = Field("TEMPORARY", alias="failureCategory")
    bank: str = "HDFC"
    merchantCategory: str = Field("SaaS", alias="merchantCategory")
    deviceType: str = Field("Android", alias="deviceType")
    networkType: str = Field("4G", alias="networkType")
    locationZone: str = Field("Bangalore", alias="locationZone")
    dayOfWeek: str = Field("Monday", alias="dayOfWeek")
    subscriptionStatus: str = Field("ACTIVE", alias="subscriptionStatus")
    retryCount: int = Field(0, alias="retryCount")
    gatewayLatencyMs: int = Field(2000, alias="gatewayLatencyMs")
    customerSuccessRate: float = Field(0.80, alias="customerSuccessRate")
    previousFailures30d: int = Field(0, alias="previousFailures30d")
    previousSuccessfulRetries: int = Field(2, alias="previousSuccessfulRetries")
    customerTenureDays: int = Field(365, alias="customerTenureDays")
    hour: int = 12

    model_config = {"populate_by_name": True}


class AuditEventResponse(BaseModel):
    paymentId: str
    amount: float
    failureCode: str
    failureCategory: str
    retryCount: int
    aiDiagnosis: str
    selectedAction: str
    expectedRecovery: float
    actualResult: str
    recoveredAmount: float
    stoppingReason: str
    timestampStep: int


class SimulateResponse(BaseModel):
    paymentId: str
    events: List[AuditEventResponse]
    totalRecovered: float
    finalStatus: str


# ─── Helper ───────────────────────────────────────────────────────────────────

def _build_payment_series(data) -> pd.Series:
    """Convert a Pydantic request into a pandas Series matching the training schema."""
    return pd.Series({
        "payment_id": f"PAY_API_{np.random.randint(100000, 999999)}",
        "amount": data.amount,
        "payment_method": data.paymentMethod,
        "bank": data.bank,
        "merchant_category": data.merchantCategory,
        "failure_code": data.failureCode,
        "failure_category": getattr(data, "failureCategory", "TEMPORARY"),
        "device_type": data.deviceType,
        "network_type": data.networkType,
        "location_zone": data.locationZone,
        "day_of_week": data.dayOfWeek,
        "subscription_status": data.subscriptionStatus,
        "retry_count": getattr(data, "retryCount", 0),
        "gateway_latency_ms": getattr(data, "gatewayLatencyMs", 2000),
        "customer_success_rate": getattr(data, "customerSuccessRate", 0.80),
        "previous_failures_30d": getattr(data, "previousFailures30d", 0),
        "previous_successful_retries": getattr(data, "previousSuccessfulRetries", 2),
        "customer_tenure_days": getattr(data, "customerTenureDays", 365),
        "hour": getattr(data, "hour", 12),
    })


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    models_loaded = {
        "diagnosis": agent.diagnosis_model is not None,
        "recovery": agent.recovery_model is not None,
    }
    all_loaded = all(models_loaded.values())
    return {
        "status": "healthy" if all_loaded else "degraded",
        "service": "payrecover-ai",
        "version": "1.0.0",
        "models": models_loaded,
    }


@app.post("/api/ai/diagnose", response_model=DiagnoseResponse)
def diagnose(req: DiagnoseRequest):
    """Diagnose failure category from payment features."""
    try:
        series = _build_payment_series(req)
        diagnosis = agent.diagnose(series)

        # Confidence from probability estimates if available
        if agent.diagnosis_model is not None:
            X = pd.DataFrame([series[agent.diag_cat + agent.diag_num]])
            probs = agent.diagnosis_model.predict_proba(X)[0]
            confidence = float(max(probs))
        else:
            confidence = 0.5

        return DiagnoseResponse(diagnosis=diagnosis, confidence=round(confidence, 4))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/ai/recovery-probability", response_model=RecoveryProbResponse)
def recovery_probability(req: RecoveryProbRequest):
    """Predict recovery probability for each candidate action."""
    try:
        series = _build_payment_series(req)
        series["failure_category"] = req.failureCategory
        probs = agent.predict_recovery_probs(series)

        return RecoveryProbResponse(
            retryNow=round(probs.get("RETRY_NOW", 0.0), 4),
            retryLater=round(probs.get("RETRY_LATER", 0.0), 4),
            switchMethod=round(probs.get("SWITCH_METHOD", 0.0), 4),
            notification=round(probs.get("SEND_NOTIFICATION", 0.0), 4),
            stop=round(probs.get("STOP", 0.0), 4),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/ai/decide", response_model=DecideResponse)
def decide(req: DecideRequest):
    """AI decides the best recovery action with stopping rules."""
    try:
        series = _build_payment_series(req)
        series["failure_category"] = req.failureCategory
        action, expected_recovery, stop_reason, candidates = agent.decide_action(
            series,
            retry_count=getattr(req, "retryCount", 0),
            customer_opted_out=getattr(req, "customerOptedOut", False),
            payment_expired=getattr(req, "paymentExpired", False),
            risk_score=getattr(req, "riskScore", 0.0),
        )

        return DecideResponse(
            action=action,
            expectedRecovery=round(expected_recovery, 2),
            stoppingReason=stop_reason,
            probabilities={k.lower(): round(v, 4) for k, v in candidates.items()} if candidates else {},
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/ai/simulate", response_model=SimulateResponse)
def simulate(req: SimulateRequest):
    """Run full recovery simulation for a single payment."""
    try:
        series = _build_payment_series(req)
        series["failure_category"] = req.failureCategory

        events = agent.run_single_payment(series)

        total_recovered = sum(e.recovered_amount for e in events)
        last_event = events[-1] if events else None
        final_status = last_event.actual_result if last_event else "UNKNOWN"

        def snake_to_camel(d: dict) -> dict:
            mapping = {
                "payment_id": "paymentId",
                "failure_code": "failureCode",
                "failure_category": "failureCategory",
                "retry_count": "retryCount",
                "ai_diagnosis": "aiDiagnosis",
                "selected_action": "selectedAction",
                "expected_recovery": "expectedRecovery",
                "actual_result": "actualResult",
                "recovered_amount": "recoveredAmount",
                "stopping_reason": "stoppingReason",
                "timestamp_step": "timestampStep",
            }
            return {mapping.get(k, k): v for k, v in d.items()}

        return SimulateResponse(
            paymentId=series["payment_id"],
            events=[AuditEventResponse(**snake_to_camel(asdict(e))) for e in events],
            totalRecovered=total_recovered,
            finalStatus=final_status,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/ai/actions")
def list_actions():
    """List available recovery actions and their costs."""
    return {
        "actions": ACTIONS,
        "costs": ACTION_COSTS,
        "maxRetries": MAX_RETRIES,
        "minRecoveryProb": MIN_RECOVERY_PROB,
    }
