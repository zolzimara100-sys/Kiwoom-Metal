from fastapi import APIRouter, HTTPException
from typing import List, Optional
from datetime import date

from app.models.schemas import (
    TechnicalIndicatorRequest,
    TechnicalIndicatorResponse,
    TrendAnalysisRequest,
    TrendAnalysisResponse
)
from app.services.technical_analysis_service import TechnicalAnalysisService

router = APIRouter()
analysis_service = TechnicalAnalysisService()


@router.post("/technical-indicators", response_model=TechnicalIndicatorResponse)
async def calculate_technical_indicators(request: TechnicalIndicatorRequest):
    """
    기술적 지표 계산

    - MA (Moving Average): 이동평균
    - RSI (Relative Strength Index): 상대강도지수
    - MACD: 이동평균 수렴확산
    - Bollinger Bands: 볼린저 밴드
    """
    try:
        result = await analysis_service.calculate_indicators(
            stock_code=request.stock_code,
            start_date=request.start_date,
            end_date=request.end_date,
            indicators=request.indicators
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/trend-analysis", response_model=TrendAnalysisResponse)
async def analyze_trend(request: TrendAnalysisRequest):
    """
    추세 분석

    - 상승/하락/횡보 추세 판단
    - 추세 강도 계산
    - 매수/매도 시그널 생성
    """
    try:
        result = await analysis_service.analyze_trend(
            stock_code=request.stock_code,
            period_days=request.period_days
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/investor-pattern/{stock_code}")
async def analyze_investor_pattern(
    stock_code: str,
    start_date: Optional[date] = None,
    end_date: Optional[date] = None
):
    """
    투자자별 매매 패턴 분석

    - 외국인/기관/개인 매매 패턴
    - 주도 세력 분석
    - 수급 강도 분석
    """
    try:
        result = await analysis_service.analyze_investor_pattern(
            stock_code=stock_code,
            start_date=start_date,
            end_date=end_date
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/correlation/{stock_code}")
async def analyze_correlation(
    stock_code: str,
    target_stocks: List[str],
    period_days: int = 30
):
    """
    종목 간 상관관계 분석

    - 가격 상관계수
    - 거래량 상관계수
    - 동조화 지수
    """
    try:
        result = await analysis_service.analyze_correlation(
            stock_code=stock_code,
            target_stocks=target_stocks,
            period_days=period_days
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
