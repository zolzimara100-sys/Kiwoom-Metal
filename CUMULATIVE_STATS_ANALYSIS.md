# 누적 통계 처리 방식 비교 분석

## 📌 현재 상황 (문제점)

### Backend
- **전송 데이터**: 현재 종목의 통계만 전송
  ```java
  progress.receivedCount = 150;  // 현재 종목의 수신 건수
  progress.savedCount = 145;     // 현재 종목의 저장 건수
  // 누적 통계 없음
  ```

### Frontend (Delta 계산 방식)
```typescript
// 종목 변경 시
if (isStockChanged) {
  previousReceived = 0;  // 리셋
  previousSaved = 0;     // 리셋
}

// Delta 계산
const deltaReceived = progress.receivedCount - previousReceived;
const deltaSaved = progress.savedCount - previousSaved;

// 누적 업데이트
cumulativeReceived += deltaReceived;  // ❌ 제대로 작동 안함
cumulativeSaved += deltaSaved;        // ❌ 제대로 작동 안함
```

### Progressive Bar
- **문제**: 100건씩 수신할 때마다 진행바 업데이트
- 종목이 완료되지 않았는데도 `processedCount`가 증가
```java
// fetchStockUntilTargetDate 시작 시
int current = processedCount.incrementAndGet();  // ❌ 너무 빨리 증가
```

---

## ✅ 요구사항

### 1. 누적 통계는 서버에서 관리

**새 종목 시작 시**:
- ✅ **현재 종목 수신/저장**: 0으로 리셋
- ✅ **누적 수신/저장**: 계속 증가 (리셋 안됨)

**Backend 전송**:
```java
progress.receivedCount = 80;              // 현재 종목 수신
progress.savedCount = 75;                 // 현재 종목 저장
progress.cumulativeReceivedCount = 450;   // 전체 누적 수신 (모든 종목 합계)
progress.cumulativeSavedCount = 430;      // 전체 누적 저장 (모든 종목 합계)
```

**Frontend**:
```typescript
// Delta 계산 없음 - 백엔드 값 그대로 사용
setKospi200TotalStats({
  totalReceived: progress.cumulativeReceivedCount,  // 백엔드 값
  totalSaved: progress.cumulativeSavedCount,        // 백엔드 값
});
```

### 2. Progressive Bar는 종목 완료 시에만 증가

**현재 (문제)**:
```
삼성전자 시작 → processedCount = 1 (1/200)
  100건 수신 → progress emit (진행바 업데이트)
  200건 수신 → progress emit (진행바 업데이트)
  300건 수신 → progress emit (진행바 업데이트)
삼성전자 완료

SK하이닉스 시작 → processedCount = 2 (2/200)
  ...
```

**요구사항**:
```
삼성전자 시작 → processedCount = 0 (0/200)
  100건 수신 → progress emit (진행바 그대로)
  200건 수신 → progress emit (진행바 그대로)
  300건 수신 → progress emit (진행바 그대로)
삼성전자 완료 → processedCount = 1 (1/200) ✅ 진행바 증가

SK하이닉스 시작 → processedCount = 1 (1/200)
  100건 수신 → progress emit (진행바 그대로)
  ...
SK하이닉스 완료 → processedCount = 2 (2/200) ✅ 진행바 증가
```

---

## 🔧 변경 계획

### Backend 변경사항

1. **Kospi200BatchProgress DTO 수정**:
   ```java
   // 기존 필드
   private int receivedCount;     // 현재 종목 수신
   private int savedCount;        // 현재 종목 저장

   // 추가 필드
   private int cumulativeReceivedCount;  // ✨ 전체 누적 수신
   private int cumulativeSavedCount;     // ✨ 전체 누적 저장
   ```

2. **fetchKospi200Batch 수정**:
   - 누적 카운터 추가: `AtomicInteger totalReceivedAll`, `totalSavedAll`
   - 각 종목 완료 시 누적 통계 업데이트
   - progress에 누적 통계 포함하여 전송

3. **fetchStockUntilTargetDate 수정**:
   - 종목 시작 시 `processedCount` 증가 ❌
   - 종목 완료 시에만 `processedCount` 증가 ✅
   - 모든 progress에 누적 통계 포함

### Frontend 변경사항

1. **Delta 계산 로직 제거**:
   ```typescript
   // ❌ 삭제
   let cumulativeReceived = 0;
   let previousReceived = 0;
   const deltaReceived = progress.receivedCount - previousReceived;
   cumulativeReceived += deltaReceived;
   ```

2. **백엔드 값 직접 사용**:
   ```typescript
   // ✅ 간단하게
   setKospi200TotalStats({
     totalReceived: progress.cumulativeReceivedCount,
     totalSaved: progress.cumulativeSavedCount,
   });
   ```

---

## 📈 예시: 3개 종목 처리 (삼성전자, SK하이닉스, LG전자)

| 이벤트 | 종목 | processedCount | receivedCount<br/>(현재 종목) | savedCount<br/>(현재 종목) | cumulativeReceived<br/>(전체 누적) | cumulativeSaved<br/>(전체 누적) |
|--------|------|----------------|-------------------------------|---------------------------|----------------------------------|-------------------------------|
| 1 | 삼성전자 | **0/200** | 100 | 95 | **100** | **95** |
| 2 | 삼성전자 | **0/200** | 200 | 190 | **200** | **190** |
| 3 | 삼성전자 | **0/200** | 250 | 245 | **250** | **245** |
| **완료** | 삼성전자 | **1/200** ✅ | 250 | 245 | **250** | **245** |
| 4 | SK하이닉스 | **1/200** | 80 | 75 | **330** | **320** |
| 5 | SK하이닉스 | **1/200** | 150 | 145 | **400** | **390** |
| **완료** | SK하이닉스 | **2/200** ✅ | 150 | 145 | **400** | **390** |
| 6 | LG전자 | **2/200** | 60 | 60 | **460** | **450** |
| **완료** | LG전자 | **3/200** ✅ | 60 | 60 | **460** | **450** |

**핵심**:
- ✅ 현재 종목 수신/저장: 새 종목 시작 시 리셋 (250 → 80)
- ✅ 누적 수신/저장: 계속 증가 (250 → 330)
- ✅ Progressive bar: 종목 완료 시에만 증가 (0 → 1 → 2 → 3)
