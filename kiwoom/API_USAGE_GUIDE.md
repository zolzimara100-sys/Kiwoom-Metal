# 키움증권 REST API 사용 가이드

## 🚀 빠른 시작

### 1단계: 프로젝트 클론 및 설정

```bash
cd /Users/juhyunhwang/kiwoom
```

### 2단계: application.properties 설정

`src/main/resources/application.properties` 파일을 열고 본인의 키움증권 API 정보 입력:

```properties
kiwoom.api.base-url=https://openapi.kiwoom.com
kiwoom.api.app-key=발급받은_앱키
kiwoom.api.app-secret=발급받은_앱시크릿
kiwoom.api.account-number=계좌번호
```

### 3단계: 빌드 및 실행

```bash
# Gradle 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

## 📡 REST API 엔드포인트

### 1. 헬스 체크
서비스가 정상 작동하는지 확인합니다.

```bash
curl http://localhost:8080/api/kiwoom/health
```

**응답 예시:**
```json
{
  "status": "OK",
  "service": "Kiwoom API Authentication Service"
}
```

### 2. 토큰 발급 (비동기)
OAuth 2.0 방식으로 액세스 토큰을 발급받습니다.

```bash
curl -X POST http://localhost:8080/api/kiwoom/auth/token
```

**응답 예시:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "accessTokenExpired": "2025-11-18T10:00:00"
}
```

### 3. 토큰 발급 (동기)
동기 방식으로 토큰을 발급받습니다.

```bash
curl -X POST http://localhost:8080/api/kiwoom/auth/token-sync
```

### 4. 토큰 상태 조회
현재 저장된 토큰의 상태를 확인합니다.

```bash
curl http://localhost:8080/api/kiwoom/auth/token-status
```

**응답 예시:**
```json
{
  "hasToken": true,
  "isExpired": false,
  "expireTime": 1700123456789,
  "token": "eyJhbGciOi..."
}
```

### 5. 유효한 토큰 조회
유효한 토큰을 반환하며, 만료된 경우 자동으로 갱신합니다.

```bash
curl http://localhost:8080/api/kiwoom/auth/valid-token
```

**응답 예시:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "유효한 토큰을 반환했습니다."
}
```

### 6. 토큰 초기화
저장된 토큰을 삭제합니다.

```bash
curl -X DELETE http://localhost:8080/api/kiwoom/auth/token
```

**응답 예시:**
```json
{
  "message": "토큰이 초기화되었습니다."
}
```

## 💻 Java 코드에서 사용하기

### 의존성 주입

```java
@Service
@RequiredArgsConstructor
public class YourService {
    
    private final KiwoomAuthService authService;
    
    public void yourMethod() {
        // 유효한 토큰 가져오기 (자동 갱신)
        String token = authService.getValidToken();
        
        // 토큰 사용
        // ...
    }
}
```

### 토큰 발급 예제

```java
@RestController
@RequiredArgsConstructor
public class YourController {
    
    private final KiwoomAuthService authService;
    
    @PostMapping("/get-token")
    public ResponseEntity<TokenResponse> getToken() {
        // 비동기 방식
        return authService.issueToken()
            .map(ResponseEntity::ok)
            .block();
        
        // 또는 동기 방식
        // TokenResponse token = authService.issueTokenSync();
        // return ResponseEntity.ok(token);
    }
}
```

## 🔐 보안 주의사항

### 1. application.properties 보호
- ✅ `.gitignore`에 추가됨
- ✅ Git에 절대 커밋하지 마세요
- ✅ 환경 변수 사용 권장

### 2. 환경 변수 사용 방법

```bash
# 환경 변수 설정
export KIWOOM_APP_KEY=your_app_key
export KIWOOM_APP_SECRET=your_app_secret

# 애플리케이션 실행
./gradlew bootRun
```

`application.properties`:
```properties
kiwoom.api.app-key=${KIWOOM_APP_KEY}
kiwoom.api.app-secret=${KIWOOM_APP_SECRET}
```

### 3. 프로덕션 환경
- Redis 등 외부 저장소에 토큰 저장
- HTTPS 사용 필수
- API Rate Limiting 구현

## 🛠 문제 해결

### 토큰 발급 실패
```
✗ 토큰 발급 실패: 401 Unauthorized
```
**해결방법:**
1. App Key와 App Secret 확인
2. 키움증권 API 서비스 신청 상태 확인
3. Base URL이 올바른지 확인

### 연결 타임아웃
```
✗ 토큰 발급 실패: Connection timeout
```
**해결방법:**
1. 네트워크 연결 확인
2. 방화벽 설정 확인
3. Base URL 확인

### 토큰 만료
- 토큰은 만료 5분 전에 자동으로 갱신됩니다
- `getValidToken()` 메서드 사용 권장

## 📚 추가 개발 가이드

### 키움 API 호출 예제

실제 키움 API를 호출하려면 `KiwoomApiService`를 참고하세요:

```java
@Service
@RequiredArgsConstructor
public class StockService {
    
    private final KiwoomApiService apiService;
    
    public void getStockInfo() {
        // 주식 현재가 조회
        apiService.getStockPrice("005930")  // 삼성전자
            .subscribe(
                response -> log.info("응답: {}", response),
                error -> log.error("에러: {}", error.getMessage())
            );
    }
}
```

### 커스터마이징

1. **토큰 저장소 변경**
   - `KiwoomAuthService`의 메모리 저장을 Redis로 변경
   
2. **추가 API 구현**
   - `KiwoomApiService`에 새로운 API 메서드 추가
   
3. **에러 핸들링**
   - `@ControllerAdvice`로 전역 예외 처리

## 📞 참고 자료

- 키움증권 공식 홈페이지
- 키움 Open API 포털
- Spring WebFlux 문서: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Project Reactor: https://projectreactor.io/

## 📄 라이선스

MIT License
