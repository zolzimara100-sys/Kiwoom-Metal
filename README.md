# 키움증권 자동거래 시스템 🚀

Spring Boot 기반 키움증권 REST API 통합 프로젝트

## 📋 주요 기능

- ✅ 토큰 자동 발급 및 갱신
- ✅ 일별 잔고 수익률 조회 (2025.11.16 기준 테스트 완료)
- ✅ 주식 현재가 조회
- ✅ 계좌 잔고 조회
- ✅ 비동기 처리 (Spring WebFlux)
- ✅ 자동 에러 핸들링 및 재시도

## 🛠️ 기술 스택

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring WebFlux** (비동기 처리)
- **Lombok**
- **Gradle 8.14.3**

## ⚙️ 설정 방법

### 1. application.properties 설정

`application.properties.example`을 복사하여 `application.properties` 생성

```bash
cp kiwoom/src/main/resources/application.properties.example \
   kiwoom/src/main/resources/application.properties
```

### 2. 키움증권 API 인증 정보 입력

```properties
kiwoom.api.app-key=실제_발급받은_앱키
kiwoom.api.app-secret=실제_발급받은_앱시크릿
kiwoom.api.account-number=실제_계좌번호
```

### 3. 접속 IP 등록

키움증권 OpenAPI 포털에서 현재 IP 주소를 허용 목록에 추가
- 로컬 개발: 본인 공인 IP
- Codespaces: Codespaces VM의 공인 IP

```bash
# 현재 IP 확인
curl ifconfig.me
```

## 🚀 실행 방법

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar kiwoom/build/libs/kiwoom-0.0.1-SNAPSHOT.jar
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 📡 API 엔드포인트

### 인증
- `POST /api/kiwoom/auth/token` - 토큰 발급
- `GET /api/kiwoom/auth/token-status` - 토큰 상태 조회
- `GET /api/kiwoom/auth/valid-token` - 유효한 토큰 조회 (자동 갱신)
- `DELETE /api/kiwoom/auth/token` - 토큰 초기화

### 잔고 조회
- `GET /api/kiwoom/balance/daily?queryDate=YYYYMMDD` - 일별 잔고 수익률 조회
- `GET /api/kiwoom/balance/daily/continue` - 일별 잔고 수익률 연속 조회

### 헬스 체크
- `GET /api/kiwoom/health` - 서버 상태 확인

## 📊 사용 예시

### 일별 잔고 수익률 조회

```bash
# 2025년 11월 16일 잔고 조회
curl "http://localhost:8080/api/kiwoom/balance/daily?queryDate=20251116"

# 응답 예시
{
  "dt": "20251114",
  "tot_buy_amt": "31812475",
  "tot_evlt_amt": "40147800",
  "tot_evltv_prft": "8264314",
  "tot_prft_rt": "25.98",
  "day_bal_rt": [
    {
      "stk_cd": "015760",
      "stk_nm": "한국전력",
      "rmnd_qty": "847",
      "buy_uv": "37559",
      "cur_prc": "47400",
      "prft_rt": "25.98"
    }
  ]
}
```

### 토큰 상태 확인

```bash
curl http://localhost:8080/api/kiwoom/auth/token-status
```

## 🏗️ 프로젝트 구조

```
kiwoom/
├── src/main/java/com/stocktrading/kiwoom/
│   ├── config/
│   │   ├── KiwoomApiConfig.java       # API 설정
│   │   └── WebClientConfig.java       # WebClient 설정
│   ├── controller/
│   │   ├── DailyBalanceController.java # 일별 잔고 컨트롤러
│   │   └── KiwoomController.java      # 기본 컨트롤러
│   ├── service/
│   │   ├── DailyBalanceService.java   # 잔고 조회 서비스
│   │   ├── KiwoomApiService.java      # API 서비스
│   │   └── KiwoomAuthService.java     # 인증 서비스
│   ├── dto/
│   │   ├── DailyBalanceRequest.java   # 요청 DTO
│   │   ├── DailyBalanceResponse.java  # 응답 DTO
│   │   └── TokenResponse.java         # 토큰 DTO
│   └── KiwoomApplication.java
└── src/main/resources/
    └── application.properties.example
```

## 📝 테스트 결과

### 2025년 11월 16일 잔고 조회 성공

- **총매수금액**: 31,812,475원
- **총평가금액**: 40,147,800원
- **총평가손익**: +8,264,314원
- **총수익률**: +25.98%
- **보유종목**: 한국전력 (015760) 847주

## ⚠️ 보안 주의사항

- **절대** `application.properties`에 실제 API 키를 커밋하지 마세요!
- 이 파일은 `.gitignore`에 포함되어 Git에 업로드되지 않습니다
- API 키가 노출되지 않도록 주의하세요
- Codespaces 사용 시 IP 주소가 변경될 수 있으니 키움 포털에서 IP 재등록 필요

## 🔧 트러블슈팅

### "지정단말기 인증 실패" 오류
- 키움증권 OpenAPI 포털에서 현재 IP를 허용 목록에 추가하세요

### "Token이 유효하지 않습니다" 오류
- 토큰이 만료되었거나 잘못되었습니다
- `/api/kiwoom/auth/token` 엔드포인트로 새 토큰을 발급하세요

## 📚 참고 문서

- [API_USAGE_GUIDE.md](kiwoom/API_USAGE_GUIDE.md)
- [CONFIGURATION_GUIDE.md](kiwoom/CONFIGURATION_GUIDE.md)
- [README_KIWOOM_AUTH.md](kiwoom/README_KIWOOM_AUTH.md)

## 📝 라이선스

MIT License
