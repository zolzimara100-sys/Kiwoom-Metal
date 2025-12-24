# Frontend Setup Guide

## 프로젝트 구조

```
kiwoom/
├── frontend/                    # React 프론트엔드
│   ├── components/             # React 컴포넌트
│   ├── App.tsx                 # 메인 앱
│   ├── index.tsx               # 진입점
│   ├── package.json            # 의존성
│   ├── vite.config.ts          # Vite 설정
│   └── .env.local              # 환경 변수
├── src/                        # Spring Boot 백엔드
│   └── main/java/...
├── build.gradle                # 백엔드 빌드 설정
└── FRONTEND_SETUP.md           # 이 문서
```

---

## 기술 스택

### 백엔드
- **프레임워크**: Spring Boot 3.5.7
- **언어**: Java 21
- **API**: REST API
- **포트**: 8080

### 프론트엔드
- **프레임워크**: React 19.2.0
- **언어**: TypeScript 5.8.2
- **빌드 도구**: Vite 6.2.0
- **포트**: 3000

---

## 시작하기

### 1. 백엔드 실행

```bash
# 프로젝트 루트에서
./gradlew bootRun

# 또는 Docker Compose 사용
docker-compose -f docker-compose.dev.yml up
```

**백엔드 서버**: http://localhost:8080

**주요 API 엔드포인트**:
- `GET /api/v1/oauth/token` - OAuth 토큰 조회
- `POST /api/v1/oauth/token` - OAuth 토큰 발급
- `GET /api/v1/investor-chart/{stockCode}` - 투자자 차트 데이터
- `GET /api/v1/stock-list` - 종목 리스트

---

### 2. 프론트엔드 실행

```bash
# frontend 폴더로 이동
cd frontend

# 의존성 설치 (최초 1회만)
npm install

# 개발 서버 실행
npm run dev
```

**프론트엔드 서버**: http://localhost:3000

**브라우저에서** http://localhost:3000 접속

---

## 환경 변수 설정

### 프론트엔드 (.env.local)

```bash
# 백엔드 API URL
VITE_API_BASE_URL=http://localhost:8080

# Google Gemini API (선택사항)
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

**주의**: `.env.local` 파일은 Git에 커밋되지 않습니다.

---

## 개발 워크플로우

### 동시 개발 시

**터미널 1 (백엔드)**:
```bash
./gradlew bootRun
```

**터미널 2 (프론트엔드)**:
```bash
cd frontend
npm run dev
```

### Hot Reload
- **백엔드**: Spring Boot DevTools 사용 시 자동 재시작
- **프론트엔드**: Vite가 자동으로 변경사항 감지 및 HMR(Hot Module Replacement)

---

## 빌드

### 프론트엔드 프로덕션 빌드

```bash
cd frontend
npm run build
```

빌드된 파일은 `frontend/dist/` 폴더에 생성됩니다.

### 백엔드 빌드

```bash
./gradlew clean build -x test
```

빌드된 JAR 파일: `build/libs/kiwoom-0.0.1-SNAPSHOT.jar`

---

## CORS 설정

백엔드에서 프론트엔드 요청을 허용하도록 CORS가 설정되어 있습니다.

**허용된 오리진**:
- `http://localhost:3000` (Vite 개발 서버)
- `http://localhost:5173` (대체 포트)

**설정 파일**: `src/main/java/com/stocktrading/kiwoom/config/CorsConfig.java`

---

## 주요 컴포넌트

### AuthCard.tsx
- 키움증권 서버 연결 상태 표시
- 연결 버튼 제공

### DataLinkCard.tsx
- 데이터 수집 페이지 링크
- 백엔드 `/investor-chart/collect` 페이지로 이동

### Header.tsx
- 페이지 헤더 및 타이틀

---

## 트러블슈팅

### CORS 에러 발생 시
1. 백엔드가 실행 중인지 확인
2. 프론트엔드 `.env.local`의 `VITE_API_BASE_URL` 확인
3. 브라우저 콘솔에서 에러 메시지 확인

### 백엔드 연결 실패 시
```bash
# 백엔드 서버 상태 확인
curl http://localhost:8080/api/v1/oauth/token
```

### 프론트엔드 빌드 에러
```bash
# node_modules 재설치
cd frontend
rm -rf node_modules package-lock.json
npm install
```

---

## API 연동 가이드

### 예시: AuthCard.tsx 실제 API 호출 구현

```typescript
const handleConnect = async () => {
  setStatus(ConnectionStatus.CONNECTING);

  try {
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE_URL}/api/auth/connect`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    const data = await response.json();

    if (data.success) {
      setStatus(ConnectionStatus.CONNECTED);
    } else {
      setStatus(ConnectionStatus.ERROR);
    }
  } catch (error) {
    console.error('Connection failed:', error);
    setStatus(ConnectionStatus.ERROR);
  }
};
```

---

## 다음 단계

1. **AuthCard 실제 API 연동** - 현재는 시뮬레이션만 구현됨
2. **추가 페이지 개발** - 투자자 차트, 종목 상세 등
3. **상태 관리 라이브러리 추가** - Zustand, Redux 등
4. **API 문서화** - Swagger/OpenAPI 추가
5. **프로덕션 배포** - Nginx, Docker 등

---

## 참고 자료

- [React 공식 문서](https://react.dev/)
- [Vite 공식 문서](https://vite.dev/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [키움증권 OpenAPI 문서](https://www.kiwoom.com/)
