# PRD: 통계분석 - 데이터 수집 후 통계 작업 일괄 처리

## 문서 정보
| 항목 | 내용 |
|------|------|
| 문서명 | 통계분석 - 데이터 수집 후 통계 작업 일괄 처리 PRD |
| 작성일 | 2025-12-17 |
| 버전 | 1.0 |

---

## 요구사항 목록

### REQ-001: 단일 종목 데이터 수집 후 통계 API 자동 호출

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-001 |
| **요구사항 제목** | 단일 종목 데이터 수집 완료 후 통계 분석 API 자동 호출 |
| **요구사항 변경 대상** | Frontend |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - 단일 종목 수집 완료 핸들러 |
| **요구사항 상세 내용** | - "종목선택" 모드에서 특정 종목의 데이터 수집(과거/최신)이 완료된 직후, 다음 3가지 API를 **순차적으로** 자동 호출한다.<br>  1. 이동평균(Moving Average) 계산 API<br>  2. 상관분석(Correlation) 계산 API<br>  3. 수급분석(Supply/Demand) 계산 API<br>- 사용자의 추가 버튼 클릭 없이 자동으로 연속 실행되어야 한다. |

---

### REQ-002: 단일 종목 통계 작업 진행 상태 표시

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-002 |
| **요구사항 제목** | 단일 종목 통계 작업 진행 상태 실시간 표시 |
| **요구사항 변경 대상** | Frontend (화면) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - Status Panel (화면 하단 상태 표시 영역) |
| **요구사항 상세 내용** | - 각 통계 API 작업이 수행될 때마다 화면 하단의 **상태 표시 영역(Status Panel)**에 현재 진행 중인 작업 내용을 실시간으로 표시한다.<br>- 표시 예시:<br>  - "이동평균 계산 중..."<br>  - "상관분석 진행 중..."<br>  - "수급분석 진행 중..."<br>  - "모든 통계 작업 완료" |

---

### REQ-003: KOSPI200 일괄 수집 후 통계 배치 작업 자동 실행

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-003 |
| **요구사항 제목** | KOSPI200 전체 종목 통계 배치 작업 자동 실행 |
| **요구사항 변경 대상** | Frontend |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - KOSPI200 배치 처리 핸들러 (`handleKospi200Submit`) |
| **요구사항 상세 내용** | - "KOSPI200" 모드에서 전체 종목(약 200개)의 최신 데이터 수신이 완료된 직후, 다음 3가지 배치 작업을 **순차적으로** 자동 실행한다.<br>  1. 전체 종목 이동평균 일괄 계산<br>  2. 전체 종목 상관분석 일괄 계산<br>  3. 전체 종목 수급분석 일괄 계산<br>- 데이터 수집 실패 여부와 관계없이 통계 배치 작업은 **무조건 실행**되어야 한다. |

---

### REQ-004: KOSPI200 2단계 Phase 진행 UI 구현

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-004 |
| **요구사항 제목** | KOSPI200 처리 2단계(Phase) 구조 UI 구현 |
| **요구사항 변경 대상** | Frontend (화면) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - KOSPI200 진행 상황 표시 영역 |
| **요구사항 상세 내용** | KOSPI200 처리를 2개의 Phase로 구분하여 UI에 표시한다.<br><br>**Phase 1: 데이터 수집**<br>- KOSPI200 전체 종목(약 200개)의 최신 데이터를 순차적으로 수집<br>- 수집 완료 시 Phase 1 완료 표시<br><br>**Phase 2: 통계DB작업**<br>- 이동평균 → 상관분석 → 수급분석 순차 실행<br>- 모든 통계 작업 완료 시 Phase 2 완료 표시 |

---

### REQ-005: Phase 상태별 시각적 스타일 적용

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-005 |
| **요구사항 제목** | Phase Box 상태별 시각적 구분 스타일 적용 |
| **요구사항 변경 대상** | Frontend (화면/CSS) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - Phase Box 컴포넌트 스타일 |
| **요구사항 상세 내용** | 2개의 Phase Box를 가로로 배치(grid-cols-2)하고, 각 상태별로 다음 스타일을 적용한다.<br><br>| 상태 | 배경색 | 테두리색 |<br>|------|--------|----------|<br>| 대기 중 | `bg-gray-100` | `border-gray-300` |<br>| 진행 중 | `bg-blue-100` | `border-blue-500` |<br>| 완료 | `bg-green-100` | `border-green-500` |<br><br>- 완료 시 초록색 체크마크 아이콘 표시 |

---

### REQ-006: Phase 1 (데이터 수집) 실시간 진행률 표시

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-006 |
| **요구사항 제목** | Phase 1 데이터 수집 진행률 실시간 표시 |
| **요구사항 변경 대상** | Frontend (화면) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - Phase 1 상태 표시 영역 |
| **요구사항 상세 내용** | Phase 1 진행 중 다음 정보를 실시간으로 표시한다.<br><br>- 현재 수집 중인 종목명<br>- 전체 진행률 (현재/전체)<br>- 표시 형식: `{종목명} ({현재순번}/{전체종목수})`<br>- 예시: "삼성전자 (50/200)" |

---

### REQ-007: Phase 2 (통계DB작업) 실시간 작업 정보 표시

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-007 |
| **요구사항 제목** | Phase 2 통계 작업 실시간 정보 표시 |
| **요구사항 변경 대상** | Frontend (화면) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - Phase 2 상태 표시 영역 |
| **요구사항 상세 내용** | Phase 2 진행 중 현재 실행 중인 통계 작업명과 처리 날짜를 실시간으로 표시한다.<br><br>- 표시 형식: `{작업명}-{YYYY-MM-DD}`<br>- 표시 예시:<br>  - "이동평균-2025-12-16"<br>  - "투자자상관분석-2025-12-16"<br>  - "수급분석-2025-12-16" |

---

### REQ-008: 현재 단계 표시 Box 구현

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-008 |
| **요구사항 제목** | 현재 단계 표시 Box UI 구현 |
| **요구사항 변경 대상** | Frontend (화면) |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - 현재 단계 표시 Box 컴포넌트 |
| **요구사항 상세 내용** | "종목선택" 모드와 동일한 스타일의 현재 단계 표시 Box를 구현한다.<br><br>- 현재 진행 중인 Phase를 표시<br>  - "Phase 1: 데이터 수집"<br>  - "Phase 2: 통계DB작업"<br>- Phase 2 진행 시 현재 작업명과 날짜를 함께 표시<br>  - 예: "Phase 2: 통계DB작업 - 이동평균-2025-12-16" |

---

### REQ-009: 통계 API 엔드포인트 호출

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-009 |
| **요구사항 제목** | 통계 분석 API 엔드포인트 연동 |
| **요구사항 변경 대상** | Frontend (API Client) |
| **요구사항 변경대상 상세** | `api.ts` - 통계 API 호출 함수 |
| **요구사항 상세 내용** | 다음 3개의 통계 API 엔드포인트를 호출하는 함수를 구현/연동한다.<br><br>| 작업 | API 엔드포인트 |<br>|------|----------------|<br>| 이동평균 | `POST /api/statistics/moving-average/calculate` |<br>| 상관분석 | `POST /api/statistics/correlation/calculate` |<br>| 수급분석 | `POST /api/statistics/supply-demand/calculate` |<br><br>- 각 API는 순차적으로 호출되어야 하며, 이전 API 완료 후 다음 API를 호출한다. |

---

### REQ-010: 에러 처리 및 예외 상황 대응

| 구분 | 내용 |
|------|------|
| **요구사항번호** | REQ-010 |
| **요구사항 제목** | 에러 처리 및 예외 상황 대응 |
| **요구사항 변경 대상** | Frontend |
| **요구사항 변경대상 상세** | `DataCollection.tsx` - 에러 핸들링 로직 |
| **요구사항 상세 내용** | 다음 에러 처리 및 예외 상황에 대응한다.<br><br>1. **수집할 데이터가 없는 경우**<br>   - Phase 2는 무조건 실행<br><br>2. **데이터 수집 실패 시**<br>   - Phase 2는 무조건 실행<br>   - Phase 1 실패 표시 후 Phase 2 진행<br><br>3. **각 Phase별 결과 표시**<br>   - 성공: 초록색 체크마크 + 완료 메시지<br>   - 실패: 빨간색 X 마크 + 에러 메시지 |

---

## 요구사항 요약 테이블

| 요구사항번호 | 요구사항 제목 | 변경 대상 | 변경대상 상세 |
|-------------|--------------|----------|--------------|
| REQ-001 | 단일 종목 통계 API 자동 호출 | Frontend | `DataCollection.tsx` - 수집 완료 핸들러 |
| REQ-002 | 단일 종목 통계 작업 진행 상태 표시 | Frontend | `DataCollection.tsx` - Status Panel |
| REQ-003 | KOSPI200 통계 배치 작업 자동 실행 | Frontend | `DataCollection.tsx` - `handleKospi200Submit` |
| REQ-004 | KOSPI200 2단계 Phase 진행 UI | Frontend | `DataCollection.tsx` - 진행 상황 표시 영역 |
| REQ-005 | Phase 상태별 시각적 스타일 적용 | Frontend/CSS | `DataCollection.tsx` - Phase Box 스타일 |
| REQ-006 | Phase 1 실시간 진행률 표시 | Frontend | `DataCollection.tsx` - Phase 1 상태 영역 |
| REQ-007 | Phase 2 실시간 작업 정보 표시 | Frontend | `DataCollection.tsx` - Phase 2 상태 영역 |
| REQ-008 | 현재 단계 표시 Box 구현 | Frontend | `DataCollection.tsx` - 현재 단계 Box |
| REQ-009 | 통계 API 엔드포인트 연동 | Frontend | `api.ts` - API 호출 함수 |
| REQ-010 | 에러 처리 및 예외 상황 대응 | Frontend | `DataCollection.tsx` - 에러 핸들링 |

---

## 참고 사항

### 관련 API 엔드포인트
| 기능 | HTTP Method | 엔드포인트 |
|------|-------------|-----------|
| 이동평균 계산 | POST | `/api/statistics/moving-average/calculate` |
| 상관분석 계산 | POST | `/api/statistics/correlation/calculate` |
| 수급분석 계산 | POST | `/api/statistics/supply-demand/calculate` |

### 관련 파일
- Frontend: `/frontend/src/pages/DataCollection.tsx`
- API Client: `/frontend/src/services/api.ts`
