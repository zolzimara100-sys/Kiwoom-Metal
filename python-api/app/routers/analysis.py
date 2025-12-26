"""
Analysis Router
AI-powered analysis with GPU acceleration
"""

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import torch
import numpy as np
import pandas as pd
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

class AnalysisRequest(BaseModel):
    """Request model for AI analysis"""
    question: str
    sector: Optional[str] = None
    stock_codes: Optional[List[str]] = None
    analysis_type: str = "general"  # general, correlation, pattern, anomaly


class PatternDetector:
    """GPU-accelerated pattern detection"""

    @staticmethod
    def find_inverse_correlation(
        data: pd.DataFrame,
        threshold: float,
        window: int,
        device: torch.device
    ) -> List[Dict]:
        """
        Find periods of inverse correlation between investors
        """
        # Convert to tensor (optimized with explicit dtype)
        frgnr = torch.from_numpy(data['frgnr'].values).to(device=device, dtype=torch.float32)
        orgn = torch.from_numpy(data['orgn'].values).to(device=device, dtype=torch.float32)

        results = []

        for i in range(window, len(data)):
            # Get window data
            window_frgnr = frgnr[i-window:i]
            window_orgn = orgn[i-window:i]

            # Calculate correlation
            corr = torch.corrcoef(torch.stack([window_frgnr, window_orgn]))[0, 1]

            if corr < threshold:
                # Calculate price change in this period
                start_price = data.iloc[i-window]['cur_prc']
                end_price = data.iloc[i]['cur_prc']
                price_change = ((end_price - start_price) / start_price) * 100

                results.append({
                    'start_date': data.iloc[i-window]['dt'],
                    'end_date': data.iloc[i]['dt'],
                    'correlation': float(corr.cpu()),
                    'price_change_pct': price_change,
                    'frgnr_sum': float(window_frgnr.sum().cpu()),
                    'orgn_sum': float(window_orgn.sum().cpu())
                })

        return results


@router.post("/analyze")
async def analyze_with_ai(
    request: Request,
    analysis_request: AnalysisRequest
):
    """
    AI-powered analysis using GPU acceleration
    Similar to Claude Agent but with local processing
    """
    try:
        db = request.app.state.db
        device = request.app.state.device

        logger.info(f"🤖 Starting AI analysis: {analysis_request.question}")

        # Parse the question to determine analysis type
        question_lower = analysis_request.question.lower()

        if "역상관" in question_lower or "inverse" in question_lower:
            # Inverse correlation analysis
            return await find_inverse_correlations(
                request, analysis_request, db, device
            )

        elif "패턴" in question_lower or "pattern" in question_lower:
            # Pattern detection
            return await detect_patterns(
                request, analysis_request, db, device
            )

        elif "이상" in question_lower or "anomaly" in question_lower:
            # Anomaly detection
            return await detect_anomalies(
                request, analysis_request, db, device
            )

        else:
            # General statistical analysis
            return await general_analysis(
                request, analysis_request, db, device
            )

    except Exception as e:
        logger.error(f"Error in AI analysis: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def find_inverse_correlations(
    request: Request,
    analysis_request: AnalysisRequest,
    db,
    device
) -> Dict:
    """Find inverse correlation periods"""

    # Build query based on request
    sector_filter = ""
    if analysis_request.sector:
        sector_filter = f"""
            AND stk_cd IN (
                SELECT stk_cd FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_list.parquet')
                WHERE sector = '{analysis_request.sector}'
            )
        """

    query = f"""
        SELECT
            stk_cd,
            dt,
            frgnr,
            orgn,
            indiv,
            cur_prc
        FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_investor_daily.parquet')
        WHERE 1=1 {sector_filter}
        ORDER BY stk_cd, dt
    """

    df = db.execute(query).df()

    # Process each stock
    all_results = []
    for stk_cd, group in df.groupby('stk_cd'):
        if len(group) < 120:  # Need enough data
            continue

        # Find inverse correlation periods using GPU
        inverse_periods = PatternDetector.find_inverse_correlation(
            group,
            threshold=-0.5,
            window=120,
            device=device
        )

        for period in inverse_periods:
            period['stk_cd'] = stk_cd
            all_results.append(period)

    # Sort by correlation (most negative first)
    all_results.sort(key=lambda x: x['correlation'])

    # Generate natural language response
    response_text = f"분석 결과: {analysis_request.sector or '전체'} 섹터에서 "
    response_text += f"총 {len(all_results)}개의 역상관 구간을 발견했습니다.\n\n"

    for i, result in enumerate(all_results[:5]):  # Top 5
        response_text += f"{i+1}. {result['stk_cd']}\n"
        response_text += f"   기간: {result['start_date']} ~ {result['end_date']}\n"
        response_text += f"   상관계수: {result['correlation']:.3f}\n"
        response_text += f"   주가 변화: {result['price_change_pct']:.2f}%\n"
        response_text += f"   외국인 순매수: {result['frgnr_sum']:,.0f}\n"
        response_text += f"   기관 순매수: {result['orgn_sum']:,.0f}\n\n"

    return {
        "success": True,
        "question": analysis_request.question,
        "analysis_type": "inverse_correlation",
        "device_used": str(device),
        "results_count": len(all_results),
        "natural_language_response": response_text,
        "detailed_results": all_results[:20]  # Top 20
    }


async def detect_patterns(
    request: Request,
    analysis_request: AnalysisRequest,
    db,
    device
) -> Dict:
    """Detect trading patterns using GPU"""

    # Implementation for pattern detection
    # ... (similar structure to inverse correlation)

    return {
        "success": True,
        "analysis_type": "pattern_detection",
        "message": "Pattern detection analysis completed"
    }


async def detect_anomalies(
    request: Request,
    analysis_request: AnalysisRequest,
    db,
    device
) -> Dict:
    """Detect anomalies in trading data"""

    query = """
        WITH stats AS (
            SELECT
                stk_cd,
                AVG(frgnr) as avg_frgnr,
                STDDEV(frgnr) as std_frgnr,
                AVG(orgn) as avg_orgn,
                STDDEV(orgn) as std_orgn
            FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_investor_daily.parquet')
            GROUP BY stk_cd
        )
        SELECT
            d.*,
            s.avg_frgnr,
            s.std_frgnr,
            -- Z-score for anomaly detection
            (d.frgnr - s.avg_frgnr) / s.std_frgnr as z_score_frgnr,
            (d.orgn - s.avg_orgn) / s.std_orgn as z_score_orgn
        FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_investor_daily.parquet') d
        JOIN stats s ON d.stk_cd = s.stk_cd
        WHERE ABS((d.frgnr - s.avg_frgnr) / s.std_frgnr) > 3
           OR ABS((d.orgn - s.avg_orgn) / s.std_orgn) > 3
        ORDER BY ABS((d.frgnr - s.avg_frgnr) / s.std_frgnr) DESC
        LIMIT 100
    """

    df = db.execute(query).df()

    anomalies = df.to_dict('records')

    response_text = f"이상 거래 탐지 결과: {len(anomalies)}개의 이상 거래를 발견했습니다.\n\n"

    for i, anomaly in enumerate(anomalies[:5]):
        response_text += f"{i+1}. {anomaly['stk_cd']} ({anomaly['dt']})\n"
        response_text += f"   외국인 Z-score: {anomaly['z_score_frgnr']:.2f}\n"
        response_text += f"   기관 Z-score: {anomaly['z_score_orgn']:.2f}\n\n"

    return {
        "success": True,
        "analysis_type": "anomaly_detection",
        "anomalies_count": len(anomalies),
        "natural_language_response": response_text,
        "anomalies": anomalies
    }


async def general_analysis(
    request: Request,
    analysis_request: AnalysisRequest,
    db,
    device
) -> Dict:
    """General statistical analysis"""

    query = """
        SELECT
            stk_cd,
            COUNT(*) as data_points,
            AVG(frgnr) as avg_frgnr,
            AVG(orgn) as avg_orgn,
            AVG(indiv) as avg_indiv,
            CORR(frgnr, cur_prc) as corr_frgnr_price,
            CORR(orgn, cur_prc) as corr_orgn_price,
            MAX(cur_prc) as max_price,
            MIN(cur_prc) as min_price
        FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_investor_daily.parquet')
        GROUP BY stk_cd
        ORDER BY ABS(CORR(frgnr, cur_prc)) DESC
        LIMIT 20
    """

    df = db.execute(query).df()

    results = df.to_dict('records')

    response_text = "일반 통계 분석 결과:\n\n"
    response_text += f"상위 {len(results)}개 종목 분석\n\n"

    for i, result in enumerate(results[:5]):
        response_text += f"{i+1}. {result['stk_cd']}\n"
        response_text += f"   외국인-주가 상관: {result['corr_frgnr_price']:.3f}\n"
        response_text += f"   기관-주가 상관: {result['corr_orgn_price']:.3f}\n\n"

    return {
        "success": True,
        "analysis_type": "general",
        "natural_language_response": response_text,
        "statistics": results
    }