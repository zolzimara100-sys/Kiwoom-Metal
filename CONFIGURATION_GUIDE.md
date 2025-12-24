# application.properties 설정 가이드

## 키움증권 API 설정 방법

1. 키움증권 홈페이지에서 REST API 서비스 신청
2. 신청 승인 후 App Key와 App Secret 발급
3. 아래 설정값을 본인의 정보로 변경

### 설정 항목

```properties
# 키움증권 API Base URL
# - 실제 운영: https://openapi.kiwoom.com
# - 모의투자: https://openapivts.kiwoom.com (가능한 경우)
kiwoom.api.base-url=YOUR_API_BASE_URL

# 발급받은 App Key
kiwoom.api.app-key=YOUR_APP_KEY

# 발급받은 App Secret
kiwoom.api.app-secret=YOUR_APP_SECRET

# 계좌번호 (선택사항)
kiwoom.api.account-number=YOUR_ACCOUNT_NUMBER
```

### 보안 권장사항

⚠️ **중요**: 절대 Git에 실제 키 정보를 커밋하지 마세요!

1. `.gitignore`에 `application.properties` 추가
2. 또는 환경 변수 사용:
   ```bash
   export KIWOOM_APP_KEY=your_app_key
   export KIWOOM_APP_SECRET=your_app_secret
   ```
   
   그리고 application.properties에서:
   ```properties
   kiwoom.api.app-key=${KIWOOM_APP_KEY}
   kiwoom.api.app-secret=${KIWOOM_APP_SECRET}
   ```

### API 신청 참고 사이트

- 키움증권 공식 홈페이지
- 키움 Open API 포털 (있는 경우)
- 고객센터 문의: 1544-xxxx
