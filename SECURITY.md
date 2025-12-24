# 🔒 보안 설정 가이드

## API Key 관리

키움증권 API Key는 **민감한 정보**이므로 절대 Git에 커밋하지 마세요!

---

## 🚀 빠른 설정 방법

### 1단계: 보안 설정 파일 생성

```bash
# 템플릿 복사
cp src/main/resources/application-secret.properties.template \
   src/main/resources/application-secret.properties
```

### 2단계: 실제 키 입력

`src/main/resources/application-secret.properties` 파일을 열고 수정:

```properties
# Kiwoom API Credentials
kiwoom.api.app-key=실제_키움_앱키를_여기에_입력
kiwoom.api.app-secret=실제_시크릿키를_여기에_입력
kiwoom.api.account-number=계좌번호를_여기에_입력
```

### 3단계: 서버 실행

```bash
# dev + secret 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev,secret'
```

---

## 📁 파일 구조

```
src/main/resources/
├── application.properties              # 기본 설정 (공개 가능)
├── application-dev.properties          # 개발 설정 (공개 가능)
├── application-secret.properties       # 🔒 실제 키 (Git 무시)
└── application-secret.properties.template  # 템플릿 (Git 커밋 가능)
```

---

## 🛡️ 보안 조치

### ✅ 1. .gitignore 설정 (이미 완료)

```gitignore
# 민감한 정보 - API 키 보호
.env
**/application.properties
application-local.properties
application-secret.properties
```

### ✅ 2. Git 히스토리에서 제거 (이미 노출된 경우)

만약 실수로 키를 Git에 커밋했다면:

```bash
# 1. 파일을 Git 히스토리에서 완전히 제거
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/application.properties" \
  --prune-empty --tag-name-filter cat -- --all

# 2. 강제 푸시 (주의!)
git push origin --force --all

# 3. 키움증권 포털에서 API Key 재발급
```

⚠️ **중요**: Git에 노출된 키는 즉시 재발급 받으세요!

### ✅ 3. 환경별 설정

| 환경 | 프로파일 | 설정 파일 | Git 커밋 |
|------|---------|----------|---------|
| 개발 (로컬) | `dev,secret` | `application-secret.properties` | ❌ 금지 |
| 개발 (공유) | `dev` | `application-dev.properties` | ✅ 가능 |
| 프로덕션 | `prod,secret` | 환경 변수 또는 secret 파일 | ❌ 금지 |

---

## 🔐 추가 보안 권장사항

### 1. API Key 암호화 (선택사항)

더 높은 보안이 필요하면 Jasypt 사용:

```gradle
implementation 'com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5'
```

```properties
# 암호화된 값 저장
kiwoom.api.app-key=ENC(암호화된_값)
```

### 2. 권한 최소화

키움증권 API Key 발급 시:
- ✅ 필요한 권한만 선택
- ✅ IP 화이트리스트 설정
- ✅ 테스트 계좌 사용 (가능한 경우)

### 3. 키 순환 정책

정기적으로 API Key 재발급:
- 권장: 3개월마다
- 의심스러운 활동 감지 시 즉시

---

## ❓ 문제 해결

### Q: "App Key와 Secret Key 검증 실패" 에러

**원인**:
- API Key가 설정되지 않음
- 잘못된 Key 입력
- Key가 만료됨

**해결**:
```bash
# 1. application-secret.properties 파일 확인
cat src/main/resources/application-secret.properties

# 2. 키 형식 확인 (공백 없이)
kiwoom.api.app-key=실제키값

# 3. 프로파일 확인
./gradlew bootRun --args='--spring.profiles.active=dev,secret'
```

### Q: "Connection to Redis refused" 에러

**원인**: Redis 서버 미실행

**해결**:
```bash
# Docker Compose로 Redis 실행
docker-compose -f docker-compose.dev.yml up -d
```

---

## 📝 체크리스트

실제 운영 전 확인사항:

- [ ] `application-secret.properties` 파일 생성
- [ ] 실제 API Key 입력
- [ ] `.gitignore`에 `application-secret.properties` 포함 확인
- [ ] `git status`로 민감한 파일이 추적되지 않는지 확인
- [ ] Redis 서버 실행
- [ ] 백엔드 서버 실행 테스트
- [ ] OAuth 토큰 발급 테스트

---

## 🚨 절대 하지 말 것

❌ **절대 하지 마세요**:
1. API Key를 Git에 커밋
2. API Key를 Slack, 이메일 등으로 공유
3. 프로덕션 Key를 개발 환경에서 사용
4. 스크린샷에 API Key 노출
5. 로그에 API Key 출력

✅ **대신 이렇게 하세요**:
1. 설정 파일로 관리 (.gitignore 적용)
2. 비밀번호 관리 도구 사용 (1Password, LastPass 등)
3. 환경별 Key 분리
4. 민감한 정보는 마스킹 처리
5. 로깅 시 Key 제거

---

## 📞 지원

키움증권 API 관련 문의:
- 키움증권 OpenAPI 고객센터
- [https://www.kiwoom.com/](https://www.kiwoom.com/)
