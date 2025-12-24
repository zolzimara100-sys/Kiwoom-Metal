# 키움증권 REST API 인증 프로그램

Spring Boot 기반의 키움증권 REST API 인증 서비스입니다.

## 프로젝트 구조

```
src/main/java/com/stocktrading/kiwoom/
├── config/
│   ├── KiwoomApiConfig.java       # API 설정 관리
│   └── WebClientConfig.java       # WebClient 설정
├── controller/
│   └── KiwoomController.java      # REST API 엔드포인트
├── dto/
│   └── TokenResponse.java         # 토큰 응답 DTO
├── service/
│   └── KiwoomAuthService.java     # 인증 서비스 로직
└── KiwoomApplication.java         # 메인 애플리케이션
```

## 주요 기능

1. **토큰 발급**: OAuth 2.0 방식으로 액세스 토큰 발급
2. **토큰 관리**: 자동 만료 확인 및 갱신
3. **REST API**: 토큰 관리를 위한 RESTful 엔드포인트 제공

## 설정 방법

### 1. application.properties 설정

`src/main/resources/application.properties` 파일을 열고 본인의 키움증권 API 정보를 입력하세요:

```properties
kiwoom.api.base-url=https://openapi.kiwoom.com
kiwoom.api.app-key=YOUR_APP_KEY
kiwoom.api.app-secret=YOUR_APP_SECRET
kiwoom.api.account-number=YOUR_ACCOUNT_NUMBER
```

### 2. 키움증권 API 신청
- 키움증권 홈페이지에서 REST API 서비스 신청
- App Key, App Secret 발급
- 발급받은 정보를 application.properties에 입력

## API 엔드포인트

### 1. 헬스 체크
```
GET /api/kiwoom/health
```

### 2. 토큰 발급 (비동기)
```
POST /api/kiwoom/auth/token
```

### 3. 토큰 발급 (동기)
```
POST /api/kiwoom/auth/token-sync
```

### 4. 토큰 상태 조회
```
GET /api/kiwoom/auth/token-status
```
응답 예시:
```json
{
  "hasToken": true,
  "isExpired": false,
  "expireTime": 1700000000000,
  "token": "eyJhbGciOi..."
}
```

### 5. 유효한 토큰 조회 (자동 갱신)
```
GET /api/kiwoom/auth/valid-token
```

### 6. 토큰 초기화
```
DELETE /api/kiwoom/auth/token
```

## 실행 방법

### Gradle로 빌드 및 실행
```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### 실행 후 테스트
```bash
# 헬스 체크
curl http://localhost:8080/api/kiwoom/health

# 토큰 발급
curl -X POST http://localhost:8080/api/kiwoom/auth/token-sync

# 토큰 상태 확인
curl http://localhost:8080/api/kiwoom/auth/token-status
```

## 주요 클래스 설명

### KiwoomAuthService
- 토큰 발급 및 관리 로직
- 자동 만료 확인 및 갱신 기능
- 메모리 기반 토큰 저장

### KiwoomController
- REST API 엔드포인트 제공
- 토큰 관리 API
- 상태 확인 API

### KiwoomApiConfig
- application.properties의 설정값 관리
- @ConfigurationProperties를 통한 자동 바인딩

## 보안 주의사항

⚠️ **중요**: `application.properties`에 민감한 정보(App Key, App Secret)가 포함되어 있습니다.
- Git에 커밋하기 전에 `.gitignore`에 추가하거나
- 환경 변수를 사용하여 관리하세요

## 참고사항

- 토큰은 만료 5분 전에 자동으로 갱신됩니다
- 현재는 메모리 기반으로 토큰을 저장합니다 (재시작 시 초기화됨)
- 실제 운영 환경에서는 Redis 등 영구 저장소 사용을 권장합니다

## 라이선스

MIT License
