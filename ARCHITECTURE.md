# 키움증권 자동거래 시스템 - 아키텍처 문서

## 🏗️ 전체 아키텍처

### 헥사고날 아키텍처 (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                    DRIVING ADAPTERS (IN)                     │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐ │
│  │ Controllers  │  │ WebSocket     │  │ Schedulers       │ │
│  │ (REST API)   │  │ (Real-time)   │  │ (Batch Jobs)     │ │
│  └──────┬───────┘  └───────┬───────┘  └────────┬─────────┘ │
└─────────┼──────────────────┼───────────────────┼───────────┘
          │                  │                   │
┌─────────┼──────────────────┼───────────────────┼───────────┐
│         ↓                  ↓                   ↓           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        APPLICATION SERVICES (USE CASES)              │  │
│  │  - FetchInvestorTradingUseCase                       │  │
│  │  - QueryStockPriceUseCase                            │  │
│  │  - AuthenticateUseCase                               │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              DOMAIN MODELS (CORE)                    │  │
│  │  - InvestorTrading, StockPrice, Token, DailyBalance │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │             OUTPUT PORTS (INTERFACES)                │  │
│  │  - InvestorTradingPort                               │  │
│  │  - KiwoomApiPort, CachePort, AuthPort                │  │
│  │  - PythonAnalysisPort                                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────┬──────────────────┬───────────────────┬───────────┘
          │                  │                   │
┌─────────┼──────────────────┼───────────────────┼───────────┐
│         ↓                  ↓                   ↓           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │ Persistence  │  │ External API │  │ Cache (Redis)    │ │
│  │ (PostgreSQL) │  │ (Kiwoom API) │  │ (Real-time)      │ │
│  └──────────────┘  └──────────────┘  └──────────────────┘ │
│                    DRIVEN ADAPTERS (OUT)                    │
└─────────────────────────────────────────────────────────────┘
```

## 📁 디렉토리 구조

```
src/main/java/com/stocktrading/kiwoom/
├── domain/                         # 핵심 비즈니스 로직 (의존성 없음)
│   ├── model/                      # 도메인 모델 (JPA 독립적)
│   │   ├── InvestorTrading.java
│   │   ├── StockPrice.java
│   │   ├── Token.java
│   │   └── DailyBalance.java
│   ├── port/
│   │   ├── in/                     # Use Case 인터페이스
│   │   │   ├── FetchInvestorTradingUseCase.java
│   │   │   ├── QueryInvestorTradingUseCase.java
│   │   │   ├── QueryStockPriceUseCase.java
│   │   │   └── AuthenticateUseCase.java
│   │   └── out/                    # Infrastructure 인터페이스
│   │       ├── InvestorTradingPort.java
│   │       ├── KiwoomApiPort.java
│   │       ├── CachePort.java
│   │       ├── AuthPort.java
│   │       ├── StockPricePort.java
│   │       └── PythonAnalysisPort.java
│   └── tr/                         # TR(Transaction Request) 추상화
│       ├── KiwoomTR.java           # 추상 클래스
│       ├── InvestorTradingTR.java
│       ├── StockPriceTR.java
│       └── TRFactory.java
│
├── application/                    # Use Case 구현
│   └── service/
│       ├── InvestorTradingApplicationService.java
│       ├── AuthenticationApplicationService.java
│       ├── StockPriceApplicationService.java
│       └── BatchAggregationScheduler.java
│
└── adapter/                        # 외부 시스템 연결
    ├── in/                         # Driving Adapters
    │   ├── web/                    # REST Controllers (기존)
    │   └── websocket/              # WebSocket 실시간 처리
    │       ├── RealTimeStockPriceHandler.java
    │       └── ReactiveStockPriceProcessor.java
    └── out/                        # Driven Adapters
        ├── persistence/            # DB 어댑터
        │   ├── entity/
        │   │   ├── StockInvestorDaily.java
        │   │   └── StockInvestorDailyId.java
        │   ├── repository/
        │   │   └── StockInvestorDailyRepository.java
        │   ├── InvestorTradingMapper.java
        │   └── InvestorTradingPersistenceAdapter.java
        ├── external/               # 외부 API 어댑터
        │   ├── KiwoomApiAdapter.java
        │   └── PythonApiAdapter.java
        └── cache/                  # Redis 어댑터
            ├── RedisCacheAdapter.java
            └── RedisAuthAdapter.java

python-analysis/                    # Python 분석 서버
├── app/
│   ├── main.py
│   ├── api/
│   │   └── analysis_router.py
│   ├── models/
│   │   └── schemas.py
│   └── services/
│       └── technical_analysis_service.py
└── requirements.txt
```

## 🔄 데이터 흐름

### 1. 실시간 주가 처리 파이프라인

```
WebSocket (키움 실시간)
    ↓
RealTimeStockPriceHandler
    ↓
ReactiveStockPriceProcessor
    ├→ Redis 캐싱 (TTL: 5초) - 실시간 조회용
    ├→ Redis Pub/Sub 발행 - 이벤트 전파
    └→ 배치 큐 추가 - PostgreSQL 저장 대기
        ↓
BatchAggregationScheduler (1분/5분/1시간/일별)
    ↓
PostgreSQL 집계 저장 (과거 데이터 분석용)
```

### 2. API 호출 흐름 (헥사고날)

```
Controller (REST API)
    ↓
Use Case Interface (Port In)
    ↓
Application Service (Use Case 구현)
    ↓
Output Port Interface
    ↓
Adapter 구현체 (Infrastructure)
    ↓
외부 시스템 (DB/API/Cache)
```

### 3. TR(Transaction) 패턴

```java
// TR 정의
InvestorTradingTR tr = trFactory.createInvestorTradingTR();

// TR 실행
TrendAnalysisResult result = tr.execute(
    request,
    token,
    apiAdapter  // KiwoomApiPort 구현체
);
```

## 💾 데이터 저장 전략

| 데이터 종류 | 저장소 | TTL/보관기간 | 용도 |
|------------|--------|-------------|------|
| **실시간 주가** | Redis | 5초 | 실시간 조회, 빠른 응답 |
| **인증 토큰** | Redis | 토큰 만료시까지 | 자동 갱신, 세션 관리 |
| **투자자 거래** | PostgreSQL | 영구 | 과거 분석, 통계 |
| **집계 데이터** | PostgreSQL | 영구 | 1분/5분/1시간/일별 OHLCV |
| **Rate Limit** | Redis | 1분 | API 호출 제한 |

## 🐍 Python 분석 서버 통합

### Java → Python 통신

```java
// Java에서 Python API 호출
PythonAnalysisPort pythonPort;

TrendAnalysisResult result = pythonPort.analyzeTrend(
    "005930",  // 삼성전자
    30         // 30일
).block();

// 결과: 추세 방향, 강도, 신호, 신뢰도
```

### Python 분석 기능

1. **기술적 지표**
   - MA (이동평균), RSI, MACD, Bollinger Bands

2. **추세 분석**
   - 상승/하락/횡보 판단
   - 매수/매도 시그널

3. **투자자 패턴**
   - 외국인/기관/개인 매매 분석
   - 수급 강도 계산

4. **상관관계 분석**
   - 종목 간 상관계수
   - 동조화 지수

## 🚀 실행 방법

### 1. PostgreSQL & Redis 시작

```bash
docker-compose up -d
```

### 2. Java 애플리케이션 시작

```bash
./gradlew bootRun
```

### 3. Python 분석 서버 시작

```bash
cd python-analysis
pip install -r requirements.txt
uvicorn app.main:app --reload
```

## 📊 모니터링

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus
- **Python API Docs**: http://localhost:8000/docs

## 🔧 주요 설정

### application.properties

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/kiwoom

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Python Analysis API
python.analysis.base-url=http://localhost:8000

# Kiwoom API
kiwoom.api.app-key=YOUR_APP_KEY
kiwoom.api.app-secret=YOUR_APP_SECRET
```

## 📝 다음 단계

### TODO (구현 필요)

1. **Controller 리팩토링**: 기존 Controller를 Port 사용하도록 수정
2. **WebSocket 클라이언트 완성**: 실제 키움 WebSocket 프로토콜 구현
3. **JSON 파싱**: InvestorTradingApplicationService의 JSON 파싱 로직
4. **배치 집계 로직**: BatchAggregationScheduler의 실제 집계 구현
5. **Python DB 연결**: Python에서 PostgreSQL 직접 조회
6. **에러 처리**: 전역 Exception Handler 구현
7. **테스트 코드**: 단위/통합 테스트 작성

## 🎯 아키텍처 장점

### 1. **독립성** (Hexagonal 핵심)
- Domain은 외부 의존성 없음 (JPA, Spring 독립)
- Infrastructure 교체 용이 (PostgreSQL → MySQL)
- 테스트 용이성 (Mock 생성 간편)

### 2. **확장성**
- 새로운 TR 추가 간편 (TRFactory에 등록)
- 새로운 Adapter 추가 가능 (Kafka, MongoDB 등)
- Python 외 다른 언어 통합 가능

### 3. **실시간 처리**
- Redis 캐싱으로 빠른 응답
- WebSocket으로 실시간 수신
- Reactive Stream으로 백프레셔 제어

### 4. **분석 파워**
- Python의 풍부한 라이브러리 활용
- Java의 안정성과 Python의 분석력 결합
- 독립적인 분석 서버 스케일링 가능

## 📚 참고 문서

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [FastAPI](https://fastapi.tiangolo.com/)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)
