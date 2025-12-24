from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging

from app.api import analysis_router

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

# FastAPI 앱 생성
app = FastAPI(
    title="Kiwoom Stock Analysis API",
    description="주식 데이터 분석 API",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(analysis_router.router, prefix="/api/analysis", tags=["analysis"])


@app.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    return {
        "status": "healthy",
        "service": "kiwoom-analysis-api"
    }


@app.on_event("startup")
async def startup_event():
    logger.info("Starting Kiwoom Analysis API...")


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("Shutting down Kiwoom Analysis API...")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
