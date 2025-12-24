from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import date
from enum import Enum


class IndicatorType(str, Enum):
    """기술적 지표 타입"""
    MA = "ma"
    RSI = "rsi"
    MACD = "macd"
    BOLLINGER = "bollinger"
    STOCHASTIC = "stochastic"


class TrendDirection(str, Enum):
    """추세 방향"""
    UP = "up"
    DOWN = "down"
    SIDEWAYS = "sideways"


class SignalType(str, Enum):
    """시그널 타입"""
    BUY = "buy"
    SELL = "sell"
    HOLD = "hold"


class TechnicalIndicatorRequest(BaseModel):
    """기술적 지표 계산 요청"""
    stock_code: str = Field(..., description="종목코드")
    start_date: date = Field(..., description="시작일")
    end_date: date = Field(..., description="종료일")
    indicators: List[IndicatorType] = Field(..., description="계산할 지표 목록")


class TechnicalIndicatorResponse(BaseModel):
    """기술적 지표 계산 응답"""
    stock_code: str
    indicators: Dict[str, Any]
    calculated_at: str


class TrendAnalysisRequest(BaseModel):
    """추세 분석 요청"""
    stock_code: str = Field(..., description="종목코드")
    period_days: int = Field(default=30, description="분석 기간 (일)")


class TrendAnalysisResponse(BaseModel):
    """추세 분석 응답"""
    stock_code: str
    trend_direction: TrendDirection
    trend_strength: float = Field(..., ge=0.0, le=1.0, description="추세 강도 (0~1)")
    signal: SignalType
    confidence: float = Field(..., ge=0.0, le=1.0, description="신뢰도 (0~1)")
    description: str


class InvestorPatternResponse(BaseModel):
    """투자자 패턴 분석 응답"""
    stock_code: str
    dominant_investor: str = Field(..., description="주도 투자자")
    foreign_strength: float
    institution_strength: float
    individual_strength: float
    supply_demand_score: float = Field(..., description="수급 점수")
    analysis: str


class CorrelationResponse(BaseModel):
    """상관관계 분석 응답"""
    stock_code: str
    correlations: Dict[str, float]
    most_correlated: str
    correlation_strength: float
