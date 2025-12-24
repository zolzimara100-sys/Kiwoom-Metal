# Kiwoom Frontend

투자자 차트(ka10060) 데이터 수집 화면 포함한 React + Vite 프론트엔드.

## 주요 변경 (요구사항 반영)
- 종목코드 직접 입력 필드 제거, 한글 종목명 검색 + "확인" 버튼/Enter 지원
- 다건 검색 시 시장(KOSPI → KOSDAQ → KONEX → 기타) 그룹 헤더로 정렬 표시
- 선택 후 종목명만 입력창 유지, 코드 별도 표기
- 선택된 종목에 대해 날짜 범위(`GET /api/v1/investor-chart/{code}/date-range`) 표시: "데이터 범위: YYYY-MM-DD ~ YYYY-MM-DD 가 저장되어 있습니다."
- 수집 모드 라벨 변경: 단일 일자 → 과거 데이터 가져오기 / 최근 30일 → 현재까지 데이터 가져오기 / 기간 배치 → 구간 데이터 가져오기
- 콤보 박스 기본값: 수량 / 순매수 / 원/주 나머지 비활성화

## 개발 환경 실행

```zsh
cd frontend
npm install
npm run dev
```

기본 포트: Vite `5173` (또는 설정에 따라 `3000`). 백엔드 CORS 설정은 `CorsConfig`에서 `http://localhost:5173` 허용.

## API 엔드포인트 요약
- 종목 검색: `GET /api/v1/stock-list/search?keyword=삼성`
- 날짜 범위: `GET /api/v1/investor-chart/{stockCode}/date-range`
- 단일 수집: `POST /api/v1/investor-chart/fetch`
- 최근(현재까지) 수집: `POST /api/v1/investor-chart/fetch/recent/{stockCode}`
- 구간 배치 수집: `POST /api/v1/investor-chart/fetch/batch`

## 토큰
`localStorage.oauth_token` 존재 시 Authorization 헤더 자동 첨부.

## 향후 개선 포인트
- 검색 결과에 페이지네이션 또는 가상 스크롤
- 중복/저장 데이터 실시간 상태 표시 (요구사항 9 확장)
- 모바일 대응 UI 세분화

## Trouble Shooting
- 검색 결과 없음: 키워드 공백 제거 후 재시도
- 날짜 범위 없음: 아직 해당 종목 데이터가 수집되지 않은 경우
- CORS 오류: 백엔드 `CorsConfig` origins에 현재 프론트 URL 추가 필요

<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/drive/19_MafI-k0Lwn35Ugg5Q-U8kRXTEYrZwS

## Run Locally

**Prerequisites:**  Node.js


1. Install dependencies:
   `npm install`
2. Set the `GEMINI_API_KEY` in [.env.local](.env.local) to your Gemini API key
3. Run the app:
   `npm run dev`
