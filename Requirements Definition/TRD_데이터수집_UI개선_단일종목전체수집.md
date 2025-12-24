# TRD: 데이터 수집 UI 개선 - 단일 종목 전체 데이터 수집

## 1. 개요

### 1.1 목적
- 데이터 수집 페이지의 UI를 단순화하여 사용성 개선
- 종목 선택 후 한 번의 버튼 클릭으로 전체 데이터(과거 + 최신) 수집 가능
- **KOSPI200 수집과 동일한 실시간 진행 상황 표시 제공**

### 1.2 범위
- `/data-collection` 페이지 UI 변경
- 단일 종목 선택 모드의 데이터 수집 로직 통합
- **실시간 진행 상황 UI 추가**

---

## 2. 현재 구조 분석

### 2.1 기존 UI 구조
```
┌─────────────────────────────────────┐
│  종목 선택 (Single Stock Search)   │
│  - 검색창                           │
│  - 확인 버튼                        │
│  - 선택된 종목 정보 표시            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  수집 모드 선택                      │
│  ┌──────┐ ┌──────┐ ┌──────┐        │
│  │ 과거 │ │ 현재 │ │ 구간 │        │
│  └──────┘ └──────┘ └──────┘        │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  선택된 모드의 수집 폼               │
│  - 날짜 입력                         │
│  - 파라미터 설정                     │
│  - "데이터 수집 시작" 버튼           │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  처리 결과 (완료 후에만 표시)       │
└─────────────────────────────────────┘
```

### 2.2 기존 수집 모드
1. **과거 데이터 가져오기** (`mode='single'`)
   - 사용자가 지정한 날짜까지 과거로 거슬러 올라가며 수집
   - DB 최초 날짜 - 1일부터 시작하여 목표 날짜까지 반복 수집

2. **현재까지 데이터 가져오기** (`mode='recent'`)
   - 오늘부터 시작하여 DB 최신 날짜까지 수집
   - DB 최신 날짜 이후의 누락된 데이터를 채움

3. **구간 데이터 가져오기** (`mode='batch'`)
   - 시작일 ~ 종료일 범위의 데이터를 수집

### 2.3 KOSPI200 진행 상황 표시 (참고)
```
✅ 실시간 진행 상황 (Line 961-1103):
├─ 현재 처리 중인 종목 표시
├─ 진행률 바 (예: 150/200 종목)
├─ 현재 종목 통계 (수신/저장/중복/오류)
├─ 누적 통계
└─ 오류 내역 실시간 표시
```

---

## 3. 변경 요구사항

### 3.1 UI 변경사항

#### 제거할 요소
```typescript
// Line 914-959: 수집 모드 선택 섹션 전체 제거
<div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
  <h2 className="text-lg font-semibold text-gray-900 mb-4">수집 모드 선택</h2>
  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
    {/* 과거 데이터 가져오기 버튼 */}
    {/* 현재까지 데이터 가져오기 버튼 */}
    {/* 구간 데이터 가져오기 버튼 */}
  </div>
</div>

// Line 1105-1334: 각 모드별 수집 폼 제거
{mode === 'single' && selectionMode === 'single-stock' && (
  <form onSubmit={handleSingleSubmit}>...</form>
)}
{mode === 'recent' && selectionMode === 'single-stock' && (
  <form onSubmit={handleRecentSubmit}>...</form>
)}
{mode === 'batch' && selectionMode === 'single-stock' && (
  <form onSubmit={handleBatchSubmit}>...</form>
)}
```

#### 추가할 요소

**1) 전체 데이터 수집 버튼 (종목 선택 섹션 아래)**
```typescript
{selectedStock && selectionMode === 'single-stock' && (
  <div className="mt-4">
    <button
      onClick={handleFullDataCollection}
      disabled={loading}
      className="w-full py-3 bg-blue-600 text-white font-semibold rounded-lg
                 hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed
                 transition-colors flex items-center justify-center gap-2"
    >
      <Download className="w-5 h-5" />
      {loading ? '수집 중...' : `${selectedStock.name} 전체데이터 가져오기`}
    </button>
  </div>
)}
```

**2) 실시간 진행 상황 표시 섹션 (새로 추가)**
```typescript
{/* Single Stock Progress Display - KOSPI200과 동일한 스타일 */}
{selectionMode === 'single-stock' && selectedStock && singleStockProgress && (
  <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
    <h2 className="text-lg font-semibold text-gray-900 mb-4">진행 상황</h2>
    {/* 진행 상황 내용 (상세는 Section 5 참조) */}
  </div>
)}
```

### 3.2 새로운 UI 구조
```
┌─────────────────────────────────────┐
│  종목 선택 (Single Stock Search)   │
│  - 검색창                           │
│  - 확인 버튼                        │
│  - 선택된 종목 정보 표시            │
│                                     │
│  ┌───────────────────────────┐    │
│  │ {종목명} 전체데이터가져오기│    │
│  └───────────────────────────┘    │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  KOSPI200 선택                      │
│  - KOSPI200 전체 수집 시작 버튼     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  진행 상황 (수집 중에만 표시) ✨NEW │
│  ├─ 현재 Phase (과거/최신)          │
│  ├─ Phase 진행 표시기               │
│  ├─ 현재 처리 중인 날짜              │
│  ├─ 현재 Phase 통계                 │
│  └─ 누적 통계                       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  처리 결과 (완료 후 표시)            │
└─────────────────────────────────────┘
```

---

## 4. 기술 구현 사항

### 4.1 진행 상황 State 정의

```typescript
// 단일 종목 진행 상황 state 추가
const [singleStockProgress, setSingleStockProgress] = useState<{
  phase: 'past' | 'recent' | null;  // 현재 Phase
  currentDate: string;                // 현재 처리 중인 날짜 (YYYYMMDD)
  totalRequests: number;              // 총 요청 수

  // 현재 Phase 통계
  receivedCount: number;              // 현재 라운드 수신 건수
  savedCount: number;                 // 현재 라운드 저장 건수
  duplicateCount: number;             // 현재 라운드 중복 건수
  errorCount: number;                 // 현재 라운드 오류 건수

  // 누적 통계
  cumulativeReceived: number;
  cumulativeSaved: number;
  cumulativeDuplicate: number;
  cumulativeError: number;

  // Phase별 상태
  pastPhaseComplete: boolean;         // Phase 1 완료 여부
  recentPhaseComplete: boolean;       // Phase 2 완료 여부

  // 오류 내역
  errors: string[];
} | null>(null);
```

### 4.2 새로운 수집 함수: `handleFullDataCollection`

#### 로직 흐름
```
초기화
 ├─ loading = true
 ├─ singleStockProgress 초기화
 └─ 통계 변수 초기화

Phase 1: 과거 데이터 수집
 ├─ 시작: DB 최초 날짜(startDate) - 1일
 ├─ 목표: 과거로 최대한 거슬러 올라감 (예: 2000-01-01)
 ├─ 진행 상황 업데이트 (매 요청마다)
 │  └─ setSingleStockProgress() 호출
 └─ 종료 조건:
    - 수신 데이터가 없을 때
    - 또는 최대 요청 횟수 도달

Phase 1 완료 표시
 └─ pastPhaseComplete = true, phase = 'recent'

Phase 2: 최신 데이터 수집
 ├─ 시작: 오늘 날짜
 ├─ 목표: DB 최신 날짜(endDate)까지
 ├─ 진행 상황 업데이트 (매 요청마다)
 │  └─ setSingleStockProgress() 호출
 └─ 종료 조건:
    - 수신한 가장 과거 날짜 <= DB 최신 날짜
    - 또는 수신 데이터가 없을 때

Phase 2 완료 표시
 └─ recentPhaseComplete = true

완료 처리
 ├─ 최종 결과 메시지
 ├─ 3초 후 진행 상황 숨김
 └─ loading = false
```

#### 구현 코드 (진행 상황 업데이트 포함)
```typescript
const handleFullDataCollection = async () => {
  if (!selectedStock || !dateRange) {
    setMessage({ type: 'error', text: '종목을 선택해주세요.' });
    return;
  }

  setLoading(true);
  setMessage(null);

  let totalRequestCount = 0;
  let totalReceivedCount = 0;
  let totalSavedCount = 0;
  let totalDuplicateCount = 0;
  let totalErrorCount = 0;
  const allSavedDates: string[] = [];
  const allDuplicateDates: string[] = [];
  const allErrorDates: string[] = [];

  try {
    // ========================================
    // 진행 상황 초기화
    // ========================================
    const dbStartDate = new Date(dateRange.startDate);
    dbStartDate.setDate(dbStartDate.getDate() - 1);
    let currentDt = dbStartDate.toISOString().split('T')[0].replace(/-/g, '');

    setSingleStockProgress({
      phase: 'past',
      currentDate: currentDt,
      totalRequests: 0,
      receivedCount: 0,
      savedCount: 0,
      duplicateCount: 0,
      errorCount: 0,
      cumulativeReceived: 0,
      cumulativeSaved: 0,
      cumulativeDuplicate: 0,
      cumulativeError: 0,
      pastPhaseComplete: false,
      recentPhaseComplete: false,
      errors: [],
    });

    // ========================================
    // Phase 1: 과거 데이터 수집
    // ========================================
    console.log('=== Phase 1: 과거 데이터 수집 시작 ===');

    const MIN_DATE = '20000101'; // 최소 날짜 (2000-01-01)
    let shouldContinuePast = true;

    while (shouldContinuePast) {
      totalRequestCount++;

      const requestForm = {
        stkCd: selectedStock.code,
        dt: currentDt,
        amtQtyTp: '2',
        trdeTp: '0',
        unitTp: '1',
      };

      const result = await investorChartApi.fetchWithStatus(requestForm);

      if (result.error) {
        console.error(`[과거] 오류: ${result.error}`);
        totalErrorCount++;
        allErrorDates.push(`${currentDt}: ${result.error}`);

        // 진행 상황 업데이트 (오류)
        setSingleStockProgress(prev => prev ? {
          ...prev,
          errorCount: prev.errorCount + 1,
          cumulativeError: totalErrorCount,
          errors: [...prev.errors, `${currentDt}: ${result.error}`],
        } : null);

        break;
      }

      if (result.data) {
        const savedDates = (result.data.savedDates || []).map((d: any) => d.toString());
        const duplicateDates = (result.data.duplicateDates || []).map((d: any) => d.toString());
        const receivedThisRound = savedDates.length + duplicateDates.length;

        totalReceivedCount += receivedThisRound;
        totalSavedCount += savedDates.length;
        totalDuplicateCount += duplicateDates.length;
        allSavedDates.push(...savedDates);
        allDuplicateDates.push(...duplicateDates);

        // ✨ 진행 상황 실시간 업데이트
        setSingleStockProgress(prev => prev ? {
          ...prev,
          currentDate: currentDt,
          totalRequests: totalRequestCount,
          receivedCount: receivedThisRound,
          savedCount: savedDates.length,
          duplicateCount: duplicateDates.length,
          cumulativeReceived: totalReceivedCount,
          cumulativeSaved: totalSavedCount,
          cumulativeDuplicate: totalDuplicateCount,
          cumulativeError: totalErrorCount,
        } : null);

        const allDates = [...savedDates, ...duplicateDates];

        if (allDates.length === 0 || currentDt <= MIN_DATE) {
          console.log('[과거] 종료: 데이터 없음 또는 최소 날짜 도달');
          shouldContinuePast = false;
        } else {
          allDates.sort();
          const oldestReceivedDate = allDates[0];
          const next = new Date(oldestReceivedDate.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'));
          next.setDate(next.getDate() - 1);
          currentDt = next.toISOString().split('T')[0].replace(/-/g, '');
        }
      } else {
        shouldContinuePast = false;
      }

      // 5요청마다 범위 갱신 및 2초 대기
      if (totalRequestCount % 5 === 0 || !shouldContinuePast) {
        if (selectedStock) {
          const updatedDateRange = await stockListApi.getDateRange(selectedStock.code);
          if (updatedDateRange.data) {
            setDateRange(updatedDateRange.data);
          }
        }

        setLastResult({
          fromDate: currentDt,
          toDate: dateRange.startDate,
          requestedCount: totalRequestCount,
          receivedCount: totalReceivedCount,
          savedCount: totalSavedCount,
          duplicateCount: totalDuplicateCount,
          errorCount: totalErrorCount,
          savedDates: allSavedDates.slice(),
          duplicateDates: allDuplicateDates.slice(),
          errorDates: allErrorDates.slice(),
          mode: 'single',
        });

        if (shouldContinuePast) {
          await sleep(2000);
        }
      }

      if (totalRequestCount >= 200) {
        console.warn('[과거] 최대 요청 횟수 도달');
        break;
      }
    }

    // ✨ Phase 1 완료 표시
    setSingleStockProgress(prev => prev ? {
      ...prev,
      pastPhaseComplete: true,
      phase: 'recent',
    } : null);

    console.log('=== Phase 1 완료 ===');
    console.log(`과거 데이터: 요청=${totalRequestCount}, 수신=${totalReceivedCount}, 저장=${totalSavedCount}`);

    // ========================================
    // Phase 2: 최신 데이터 수집
    // ========================================
    console.log('=== Phase 2: 최신 데이터 수집 시작 ===');

    // 데이터 범위 재조회 (Phase 1에서 업데이트되었으므로)
    const updatedRange = await stockListApi.getDateRange(selectedStock.code);
    const latestEndDate = updatedRange.data?.endDate || dateRange.endDate;
    const latestSavedYmd = latestEndDate.replace(/-/g, '');

    currentDt = new Date().toISOString().split('T')[0].replace(/-/g, '');
    let shouldContinueRecent = true;

    while (shouldContinueRecent) {
      totalRequestCount++;

      const requestForm = {
        stkCd: selectedStock.code,
        dt: currentDt,
        amtQtyTp: '2',
        trdeTp: '0',
        unitTp: '1',
      };

      const result = await investorChartApi.fetchWithStatus(requestForm);

      if (result.error) {
        console.error(`[최신] 오류: ${result.error}`);
        totalErrorCount++;
        allErrorDates.push(`${currentDt}: ${result.error}`);

        // 진행 상황 업데이트 (오류)
        setSingleStockProgress(prev => prev ? {
          ...prev,
          errorCount: prev.errorCount + 1,
          cumulativeError: totalErrorCount,
          errors: [...prev.errors, `${currentDt}: ${result.error}`],
        } : null);

        break;
      }

      if (result.data) {
        const savedDates = (result.data.savedDates || []).map((d: any) => d.toString());
        const duplicateDates = (result.data.duplicateDates || []).map((d: any) => d.toString());
        const receivedThisRound = savedDates.length + duplicateDates.length;

        totalReceivedCount += receivedThisRound;
        totalSavedCount += savedDates.length;
        totalDuplicateCount += duplicateDates.length;
        allSavedDates.push(...savedDates);
        allDuplicateDates.push(...duplicateDates);

        // ✨ 진행 상황 실시간 업데이트
        setSingleStockProgress(prev => prev ? {
          ...prev,
          currentDate: currentDt,
          totalRequests: totalRequestCount,
          receivedCount: receivedThisRound,
          savedCount: savedDates.length,
          duplicateCount: duplicateDates.length,
          cumulativeReceived: totalReceivedCount,
          cumulativeSaved: totalSavedCount,
          cumulativeDuplicate: totalDuplicateCount,
          cumulativeError: totalErrorCount,
        } : null);

        const allDates = [...savedDates, ...duplicateDates];

        if (allDates.length === 0) {
          console.log('[최신] 종료: 데이터 없음');
          shouldContinueRecent = false;
        } else {
          allDates.sort();
          const oldestReceivedDate = allDates[0];

          if (oldestReceivedDate <= latestSavedYmd) {
            console.log(`[최신] 종료: 기존 데이터와 연결됨 (${oldestReceivedDate} <= ${latestSavedYmd})`);
            shouldContinueRecent = false;
          } else {
            const next = new Date(oldestReceivedDate.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3'));
            next.setDate(next.getDate() - 1);
            currentDt = next.toISOString().split('T')[0].replace(/-/g, '');
          }
        }
      } else {
        shouldContinueRecent = false;
      }

      // 5요청마다 범위 갱신 및 2초 대기
      if (totalRequestCount % 5 === 0 || !shouldContinueRecent) {
        if (selectedStock) {
          const updatedDateRange = await stockListApi.getDateRange(selectedStock.code);
          if (updatedDateRange.data) {
            setDateRange(updatedDateRange.data);
          }
        }

        setLastResult({
          fromDate: dateRange.startDate,
          toDate: currentDt,
          requestedCount: totalRequestCount,
          receivedCount: totalReceivedCount,
          savedCount: totalSavedCount,
          duplicateCount: totalDuplicateCount,
          errorCount: totalErrorCount,
          savedDates: allSavedDates.slice(),
          duplicateDates: allDuplicateDates.slice(),
          errorDates: allErrorDates.slice(),
          mode: 'recent',
        });

        if (shouldContinueRecent) {
          await sleep(2000);
        }
      }

      if (totalRequestCount >= 200) {
        console.warn('[최신] 최대 요청 횟수 도달');
        break;
      }
    }

    // ✨ Phase 2 완료 표시
    setSingleStockProgress(prev => prev ? {
      ...prev,
      recentPhaseComplete: true,
    } : null);

    console.log('=== Phase 2 완료 ===');
    console.log(`전체 데이터: 요청=${totalRequestCount}, 수신=${totalReceivedCount}, 저장=${totalSavedCount}`);

  } catch (error) {
    console.error('전체 데이터 수집 중 예외 발생:', error);
    setMessage({ type: 'error', text: `데이터 수집 중 오류가 발생했습니다: ${error}` });
  } finally {
    setLoading(false);

    // 최종 데이터 범위 갱신
    if (selectedStock) {
      const finalDateRange = await stockListApi.getDateRange(selectedStock.code);
      if (finalDateRange.data) {
        setDateRange(finalDateRange.data);
      }
    }

    // 최종 결과 메시지
    if (totalErrorCount > 0) {
      setMessage({
        type: 'error',
        text: `전체 데이터 수집이 완료되었습니다. (오류: ${totalErrorCount}건)`
      });
    } else {
      setMessage({
        type: 'success',
        text: `${selectedStock.name} 전체 데이터 수집이 완료되었습니다!`
      });
    }

    setStatusPanel({
      savedDates: allSavedDates,
      duplicateDates: allDuplicateDates,
      errorDates: allErrorDates
    });

    setLastResult({
      requestedCount: totalRequestCount,
      receivedCount: totalReceivedCount,
      savedCount: totalSavedCount,
      duplicateCount: totalDuplicateCount,
      errorCount: totalErrorCount,
      savedDates: allSavedDates,
      duplicateDates: allDuplicateDates,
      errorDates: allErrorDates,
      mode: 'recent',
    });

    // ✨ 3초 후 진행 상황 숨김
    setTimeout(() => {
      setSingleStockProgress(null);
    }, 3000);
  }
};
```

---

## 5. 파일 수정 상세

### 5.1 `/frontend/pages/DataCollection.tsx`

#### 5.1.1 State 관리 변경
```typescript
// ===== 제거 =====
// const [mode, setMode] = useState<CollectionMode>('single');
// const [singleForm, setSingleForm] = useState<InvestorChartRequest>({...});
// const [batchForm, setBatchForm] = useState<InvestorChartBatchRequest>({...});

// ===== 유지 =====
const [selectionMode, setSelectionMode] = useState<SelectionMode>('single-stock');

// ===== 추가 ✨ =====
// 단일 종목 진행 상황 state
const [singleStockProgress, setSingleStockProgress] = useState<{
  phase: 'past' | 'recent' | null;
  currentDate: string;
  totalRequests: number;
  receivedCount: number;
  savedCount: number;
  duplicateCount: number;
  errorCount: number;
  cumulativeReceived: number;
  cumulativeSaved: number;
  cumulativeDuplicate: number;
  cumulativeError: number;
  pastPhaseComplete: boolean;
  recentPhaseComplete: boolean;
  errors: string[];
} | null>(null);
```

#### 5.1.2 UI 수정 - 진행 상황 섹션 추가

**위치: 종목 선택 섹션과 KOSPI200 섹션 사이**

```typescript
{/* ========================================
    Single Stock Progress Display
    ======================================== */}
{selectionMode === 'single-stock' && selectedStock && singleStockProgress && (
  <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
    <h2 className="text-lg font-semibold text-gray-900 mb-4">진행 상황</h2>

    {/* Current Phase Indicator */}
    <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
      <p className="text-sm text-blue-800 mb-1">현재 단계</p>
      <p className="text-lg font-semibold text-blue-900">
        {singleStockProgress.phase === 'past'
          ? '📅 Phase 1: 과거 데이터 수집 중...'
          : '📈 Phase 2: 최신 데이터 수집 중...'}
      </p>
      <p className="text-sm text-blue-700 mt-1">
        처리 중인 날짜: {formatDateForDisplay(singleStockProgress.currentDate)}
      </p>
    </div>

    {/* Phase Progress Indicators */}
    <div className="grid grid-cols-2 gap-4 mb-4">
      {/* Phase 1 */}
      <div className={`p-3 rounded-lg border-2 transition-all ${
        singleStockProgress.pastPhaseComplete
          ? 'bg-green-50 border-green-500'
          : singleStockProgress.phase === 'past'
          ? 'bg-blue-50 border-blue-500 ring-2 ring-blue-200'
          : 'bg-gray-50 border-gray-300'
      }`}>
        <div className={`text-sm font-medium mb-1 flex items-center gap-2 ${
          singleStockProgress.pastPhaseComplete ? 'text-green-700' : 'text-gray-700'
        }`}>
          {singleStockProgress.pastPhaseComplete && <span>✓</span>}
          Phase 1: 과거 데이터
        </div>
        <div className="text-xs text-gray-600">
          DB 최초 날짜부터 역방향 수집
        </div>
      </div>

      {/* Phase 2 */}
      <div className={`p-3 rounded-lg border-2 transition-all ${
        singleStockProgress.recentPhaseComplete
          ? 'bg-green-50 border-green-500'
          : singleStockProgress.phase === 'recent'
          ? 'bg-blue-50 border-blue-500 ring-2 ring-blue-200'
          : 'bg-gray-50 border-gray-300'
      }`}>
        <div className={`text-sm font-medium mb-1 flex items-center gap-2 ${
          singleStockProgress.recentPhaseComplete ? 'text-green-700' : 'text-gray-700'
        }`}>
          {singleStockProgress.recentPhaseComplete && <span>✓</span>}
          Phase 2: 최신 데이터
        </div>
        <div className="text-xs text-gray-600">
          오늘부터 DB 최신 날짜까지 수집
        </div>
      </div>
    </div>

    {/* Current Round Stats */}
    <div className="grid grid-cols-4 gap-4 mb-4">
      <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-center">
        <div className="text-xs text-blue-700 mb-1">요청 횟수</div>
        <div className="text-2xl font-bold text-blue-900">
          {singleStockProgress.totalRequests}
        </div>
      </div>
      <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
        <div className="text-xs text-green-700 mb-1">현재 수신</div>
        <div className="text-2xl font-bold text-green-900">
          {singleStockProgress.receivedCount}
        </div>
      </div>
      <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg text-center">
        <div className="text-xs text-purple-700 mb-1">현재 저장</div>
        <div className="text-2xl font-bold text-purple-900">
          {singleStockProgress.savedCount}
        </div>
      </div>
      <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-center">
        <div className="text-xs text-yellow-700 mb-1">현재 중복</div>
        <div className="text-2xl font-bold text-yellow-900">
          {singleStockProgress.duplicateCount}
        </div>
      </div>
    </div>

    {/* Cumulative Stats */}
    <div className="grid grid-cols-4 gap-4 mb-4">
      <div className="p-3 bg-blue-100 border border-blue-300 rounded-lg text-center">
        <div className="text-xs text-blue-800 mb-1">누적 수신</div>
        <div className="text-xl font-bold text-blue-900">
          {singleStockProgress.cumulativeReceived}
        </div>
      </div>
      <div className="p-3 bg-green-100 border border-green-300 rounded-lg text-center">
        <div className="text-xs text-green-800 mb-1">누적 저장</div>
        <div className="text-xl font-bold text-green-900">
          {singleStockProgress.cumulativeSaved}
        </div>
      </div>
      <div className="p-3 bg-yellow-100 border border-yellow-300 rounded-lg text-center">
        <div className="text-xs text-yellow-800 mb-1">누적 중복</div>
        <div className="text-xl font-bold text-yellow-900">
          {singleStockProgress.cumulativeDuplicate}
        </div>
      </div>
      <div className="p-3 bg-red-100 border border-red-300 rounded-lg text-center">
        <div className="text-xs text-red-800 mb-1">누적 오류</div>
        <div className="text-xl font-bold text-red-900">
          {singleStockProgress.cumulativeError}
        </div>
      </div>
    </div>

    {/* Data Range Update */}
    {dateRange && (
      <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg mb-4">
        <div className="text-xs text-gray-600 mb-1">현재 저장된 데이터 범위</div>
        <div className="text-sm font-semibold text-gray-900">
          {formatDateDisplay(dateRange.startDate)} ~ {formatDateDisplay(dateRange.endDate)}
        </div>
      </div>
    )}

    {/* Errors */}
    {singleStockProgress.errors && singleStockProgress.errors.length > 0 && (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg max-h-60 overflow-y-auto">
        <h4 className="text-sm font-semibold text-red-800 mb-2">
          오류 내역 ({singleStockProgress.errors.length}건)
        </h4>
        <ul className="space-y-1 text-xs text-red-900">
          {singleStockProgress.errors.map((error, idx) => (
            <li key={idx} className="border-b border-red-100 pb-1">{error}</li>
          ))}
        </ul>
      </div>
    )}

    {/* Completion Message */}
    {singleStockProgress.pastPhaseComplete && singleStockProgress.recentPhaseComplete && (
      <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-center">
        <p className="text-sm font-semibold text-green-800">
          ✓ 전체 데이터 수집이 완료되었습니다!
        </p>
      </div>
    )}
  </div>
)}
```

#### 5.1.3 함수 추가/제거

**제거할 함수:**
- `handleSingleSubmit()` - 과거 데이터 수집
- `handleRecentSubmit()` - 최신 데이터 수집
- `handleBatchSubmit()` - 구간 데이터 수집

**추가할 함수:**
- `handleFullDataCollection()` - 통합 전체 데이터 수집 (진행 상황 업데이트 포함)

**추가할 유틸리티 함수:**
```typescript
// 날짜 포맷팅 함수 (YYYYMMDD -> YYYY-MM-DD)
const formatDateForDisplay = (dateStr: string): string => {
  if (!dateStr || dateStr.length !== 8) return dateStr;
  return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
};
```

---

## 6. 동작 시나리오

### 6.1 사용자 플로우

```
1. 사용자가 데이터 수집 페이지 접속
   └─> 인증 상태 확인

2. 종목 검색
   ├─> 검색창에 종목명 입력 (예: "삼성전자")
   ├─> "확인" 버튼 클릭
   └─> 검색 결과 표시 (단일 결과 시 자동 선택, 다중 결과 시 팝업)

3. 종목 선택
   ├─> 선택된 종목 정보 표시
   │   ├─> 종목명 및 코드
   │   └─> 현재 저장된 데이터 범위
   └─> "{종목명} 전체데이터 가져오기" 버튼 활성화

4. 전체 데이터 수집 시작
   ├─> 버튼 클릭
   ├─> "진행 상황" 섹션 표시 ✨
   │   └─> Phase 1 시작 표시
   ├─> Phase 1: 과거 데이터 수집
   │   ├─> 매 요청마다 진행 상황 업데이트 ✨
   │   │   ├─> 현재 처리 날짜
   │   │   ├─> 현재/누적 통계
   │   │   └─> 데이터 범위 갱신
   │   └─> Phase 1 완료 표시 ✨
   ├─> Phase 2: 최신 데이터 수집
   │   ├─> Phase 2 시작 표시 ✨
   │   ├─> 매 요청마다 진행 상황 업데이트 ✨
   │   └─> Phase 2 완료 표시 ✨
   └─> 완료 메시지 표시
   └─> 3초 후 진행 상황 섹션 자동 숨김 ✨

5. 결과 확인
   ├─> 처리 결과 표시 (요청/수신/저장/중복/오류)
   ├─> 저장된 데이터 범위 업데이트
   └─> 저장/중복/오류 상세 내역 표시
```

### 6.2 예시: 삼성전자 데이터 수집 (진행 상황 포함)

**초기 상태:**
- DB에 저장된 데이터 범위: 2023-01-01 ~ 2024-12-01
- 오늘 날짜: 2024-12-16

**진행 상황 표시 시작:**
```
┌─────────────────────────────┐
│ 진행 상황                   │
├─────────────────────────────┤
│ 현재 단계:                  │
│ 📅 Phase 1: 과거 데이터...  │
│ 처리 날짜: 2022-12-31       │
├─────────────────────────────┤
│ [●○] Phase 1  [ ] Phase 2  │
├─────────────────────────────┤
│ 요청: 1  수신: 30           │
│ 저장: 30  중복: 0           │
├─────────────────────────────┤
│ 누적 수신: 30               │
│ 누적 저장: 30               │
└─────────────────────────────┘
```

**Phase 1 진행:**
```
Request #1: dt=20221231
  └─> 진행 상황 업데이트: 요청 1, 수신 30, 저장 30
Request #5: dt=20220831
  └─> 진행 상황 업데이트: 요청 5, 누적 수신 150, 누적 저장 150
  └─> 데이터 범위 갱신: 2022-08-01 ~ 2024-12-01
  └─> 2초 대기
...
```

**Phase 1 완료 → Phase 2 시작:**
```
┌─────────────────────────────┐
│ 진행 상황                   │
├─────────────────────────────┤
│ 현재 단계:                  │
│ 📈 Phase 2: 최신 데이터...  │
│ 처리 날짜: 2024-12-16       │
├─────────────────────────────┤
│ [✓] Phase 1  [●○] Phase 2  │
├─────────────────────────────┤
│ 요청: 51  수신: 15          │
│ 저장: 15  중복: 0           │
├─────────────────────────────┤
│ 누적 수신: 1515             │
│ 누적 저장: 1515             │
└─────────────────────────────┘
```

**완료:**
```
┌─────────────────────────────┐
│ 진행 상황                   │
├─────────────────────────────┤
│ [✓] Phase 1  [✓] Phase 2   │
├─────────────────────────────┤
│ ✓ 전체 데이터 수집 완료!    │
└─────────────────────────────┘

(3초 후 자동 숨김)
```

---

## 7. 에러 처리

### 7.1 에러 시나리오

1. **종목 미선택 상태에서 버튼 클릭**
   ```
   에러 메시지: "종목을 선택해주세요."
   진행 상황: 표시되지 않음
   ```

2. **API 요청 실패**
   ```
   - 오류 카운트 증가
   - 진행 상황에 오류 표시 ✨
   - 오류 내역에 추가: "YYYYMMDD: {오류 메시지}"
   - 해당 Phase 중단, 다음 Phase 진행
   ```

3. **네트워크 타임아웃**
   ```
   - 오류로 처리
   - 진행 상황에 오류 표시 ✨
   - 재시도하지 않고 종료
   - 사용자에게 오류 메시지 표시
   ```

4. **최대 요청 횟수 초과**
   ```
   - Phase별 최대 200회
   - 초과 시 경고 메시지 출력 후 종료
   - 진행 상황에 상태 표시 ✨
   ```

### 7.2 오류 복구 전략

- **부분 성공 허용**: Phase 1 실패 시에도 Phase 2 진행
- **진행 상황 저장**: 매 요청마다 결과 업데이트 → 중단 시에도 실시간 상황 확인 가능 ✨
- **오류 상세 기록**: 날짜별 오류 내역 저장 및 실시간 표시 ✨
- **자동 정리**: 완료 후 3초 뒤 진행 상황 자동 숨김 ✨

---

## 8. 성능 고려사항

### 8.1 API 부하 방지

- **5요청마다 2초 대기**: 서버 부하 방지
- **최대 요청 횟수 제한**: Phase별 200회 (총 400회)
- **진행 상황 업데이트**: 매 요청마다 UI 갱신 ✨

### 8.2 UI 응답성

- **비동기 처리**: async/await 사용
- **실시간 진행 상황 표시** ✨:
  - 현재 처리 중인 Phase 표시
  - 현재 처리 날짜 표시
  - Phase별 완료 표시
  - 현재 라운드 통계 (수신/저장/중복)
  - 누적 통계 실시간 업데이트
  - 오류 내역 실시간 표시
  - 데이터 범위 실시간 갱신

### 8.3 메모리 관리

- **배열 슬라이스**: 5요청마다 `.slice()`로 새 배열 생성 → React 리렌더링 트리거
- **진행 상황 State 관리**: 매 요청마다 업데이트하지만 간결한 구조 유지 ✨
- **자동 정리**: 완료 후 3초 뒤 진행 상황 state null 처리 → 메모리 해제 ✨

---

## 9. 테스트 계획

### 9.1 단위 테스트

1. **handleFullDataCollection 함수**
   - Phase 1 정상 동작 확인
   - Phase 2 정상 동작 확인
   - 오류 발생 시 처리 확인
   - **진행 상황 업데이트 확인** ✨

2. **진행 상황 State 관리** ✨
   - 초기화 확인
   - 매 요청마다 업데이트 확인
   - Phase 전환 시 상태 확인
   - 완료 후 자동 정리 확인

3. **데이터 범위 갱신**
   - 5요청마다 정상 갱신 확인
   - 최종 갱신 확인

### 9.2 통합 테스트

1. **전체 플로우 테스트**
   ```
   종목 검색 → 종목 선택 → 전체 데이터 수집 → 진행 상황 표시 → 결과 확인
   ```

2. **KOSPI200과의 병행 테스트**
   - 단일 종목 모드와 KOSPI200 모드 전환 확인
   - 진행 상황 표시 일관성 확인 ✨

### 9.3 UI 테스트

1. **버튼 활성화/비활성화**
   - 종목 미선택 시 버튼 비활성화
   - 로딩 중 버튼 비활성화

2. **진행 상황 표시** ✨
   - 수집 시작 시 진행 상황 표시 확인
   - Phase별 표시기 전환 확인
   - 실시간 통계 업데이트 확인
   - 완료 후 3초 뒤 자동 숨김 확인
   - 오류 발생 시 오류 내역 표시 확인

3. **처리 결과 표시**
   - 완료 후 결과 표시 확인

---

## 10. 배포 계획

### 10.1 배포 전 체크리스트

- [ ] 기존 수집 모드 선택 UI 제거 확인
- [ ] 전체 데이터 수집 버튼 정상 표시 확인
- [ ] **진행 상황 섹션 정상 표시 확인** ✨
- [ ] Phase 1 + Phase 2 로직 정상 동작 확인
- [ ] **진행 상황 실시간 업데이트 확인** ✨
- [ ] **Phase 전환 시 표시 확인** ✨
- [ ] **완료 후 자동 숨김 확인** ✨
- [ ] 오류 처리 정상 동작 확인
- [ ] KOSPI200 수집 기능 영향 없음 확인

### 10.2 롤백 계획

- **문제 발생 시**: 이전 커밋으로 롤백
- **데이터 정합성**: DB 데이터는 변경되지 않으므로 롤백 필요 없음
- **UI 문제 발생 시**: 진행 상황 표시 부분만 주석 처리 가능

---

## 11. 향후 개선 방향

### 11.1 단기 개선

1. **중단 기능 추가**
   - 수집 중 "중단" 버튼 활성화
   - 현재 Phase만 중단, 다음 Phase 진행 여부 선택 가능
   - 중단 시 진행 상황 유지 ✨

2. **진행률 표시** ✨
   - Phase별 예상 진행률 바 추가
   - 예상 소요 시간 표시
   - 평균 응답 시간 기반 계산

### 11.2 장기 개선

1. **백그라운드 작업 지원**
   - WebSocket 또는 SSE로 백그라운드 수집
   - 페이지 이동 후에도 수집 계속 진행
   - 백그라운드 진행 상황 알림 ✨

2. **스케줄링 기능**
   - 정해진 시간에 자동 수집
   - 매일 종가 이후 자동 업데이트

3. **배치 최적화**
   - 여러 종목 동시 수집
   - 병렬 처리로 수집 속도 향상
   - 통합 진행 상황 표시 ✨

---

## 12. KOSPI200과의 진행 상황 비교 ✨

### 12.1 공통점
- 실시간 통계 업데이트
- 누적 통계 표시
- 오류 내역 표시
- 완료 상태 표시

### 12.2 차이점

| 항목 | KOSPI200 | 단일 종목 |
|------|----------|-----------|
| 진행률 바 | ✓ (N/200 종목) | Phase 표시기 (Phase 1/2) |
| 현재 처리 대상 | 종목명 + 코드 | 날짜 (YYYY-MM-DD) |
| Phase 구분 | 없음 | Phase 1 (과거) / Phase 2 (최신) |
| 완료 후 동작 | 유지 | 3초 후 자동 숨김 |

### 12.3 UI 일관성

두 모드 모두 동일한 디자인 시스템 적용:
- 색상 스키마 (파란색/초록색/노란색/빨간색)
- 박스 레이아웃
- 폰트 크기 및 스타일
- 애니메이션 효과

---

## 13. 결론

본 TRD는 데이터 수집 페이지의 UI를 단순화하고, 사용자가 한 번의 클릭으로 전체 데이터(과거 + 최신)를 수집할 수 있도록 개선하며, **KOSPI200과 동일한 수준의 실시간 진행 상황 표시를 제공**하는 것을 목표로 합니다.

**주요 변경사항:**
- 3개의 수집 모드 버튼 제거
- "{종목명} 전체데이터 가져오기" 단일 버튼으로 통합
- Phase 1 (과거) + Phase 2 (최신) 자동 수행
- **실시간 진행 상황 표시 추가** ✨
  - Phase별 진행 표시
  - 현재 처리 날짜 표시
  - 실시간 통계 업데이트
  - 오류 내역 실시간 표시

**기대 효과:**
- 사용자 경험 개선 (클릭 횟수 감소)
- UI 단순화 (선택 옵션 감소)
- 데이터 누락 방지 (전체 범위 자동 수집)
- **진행 상황 가시성 향상** ✨
- **KOSPI200과 일관된 UX 제공** ✨

---

## 부록 A: 관련 파일 목록

```
/frontend/pages/DataCollection.tsx
/frontend/services/api.ts
/src/main/java/com/stocktrading/kiwoom/controller/InvestorChartController.java
```

---

## 부록 B: API 엔드포인트

```
POST /api/investor-chart/fetch-with-status
POST /api/investor-chart/fetch-raw
POST /api/investor-chart/save-batch
GET  /api/stock-list/date-range/{stkCd}
```

---

## 부록 C: 진행 상황 State 타입 정의 ✨

```typescript
interface SingleStockProgress {
  phase: 'past' | 'recent' | null;
  currentDate: string;              // YYYYMMDD
  totalRequests: number;
  receivedCount: number;
  savedCount: number;
  duplicateCount: number;
  errorCount: number;
  cumulativeReceived: number;
  cumulativeSaved: number;
  cumulativeDuplicate: number;
  cumulativeError: number;
  pastPhaseComplete: boolean;
  recentPhaseComplete: boolean;
  errors: string[];
}
```

---

**문서 버전:** 2.0 ✨
**작성일:** 2024-12-16
**작성자:** Claude Code
**승인자:** -
**최종 수정일:** 2024-12-16 (진행 상황 표시 추가)
**변경 이력:**
- v1.0: 초기 작성
- v2.0: 실시간 진행 상황 표시 기능 추가 ✨
