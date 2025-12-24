# TRD: 통계분석 - 투자자별 투자비율 개발 계획

## REQ-006: 투자자별 거래 비중 계산 로직 구현

### 개발 계획

#### 1. 요구사항 분석
- **목표**: 투자자별 거래 비중을 계산하는 백엔드 로직을 구현하여 API로 제공.
- **데이터 소스**:
  - 테이블: `tb_stock_investor_chart`
  - 조회 대상: 특정 종목의 최신 데이터부터 지난 1년간의 거래 데이터.
- **결과물**: 투자자별 거래 비중(%)을 반환하는 API.

#### 2. 데이터베이스 설계 검토

##### 2-1. 필요한 컬럼
| 컬럼명 | 설명 | 용도 |
|--------|------|------|
| `stk_cd` | 종목 코드 | 조회 조건 |
| `dt` | 거래 날짜 | 1년 범위 필터링 |
| `cur_prc` | 현재가 | 거래규모 계산 |

##### 2-2. 투자자 컬럼 매핑
| 투자자명 | DB 컬럼명 | Frontend Key | 비고 |
|---------|----------|--------------|------|
| 외국인 | `frgnr_invsr` | `frgnr` | 순매수량 |
| 기관계 | `orgn` | `orgn` | 순매수량 |
| 개인 | `ind_invsr` | `ind` | 순매수량 |
| 금융투자 | `fnnc_invt` | `fnncInvt` | 순매수량 |
| 보험 | `insrnc` | `insrnc` | 순매수량 |
| 투신 | `invtrt` | `invtrt` | 순매수량 |
| 은행 | `bank` | `bank` | 순매수량 |
| 기타금융 | `etc_fnnc` | `etcFnnc` | 순매수량 |
| 연기금 | `penfnd_etc` | `penfndEtc` | 순매수량 |
| 사모펀드 | `samo_fund` | `samoFund` | 순매수량 |
| 기타법인 | `etc_corp` | `etcCorp` | 순매수량 |
| 국가/지자체 | `natn` | `natn` | 순매수량 |
| 내국인 | `natfor` | `natfor` | 순매수량 |

#### 3. API 설계

##### 3-1. 엔드포인트
- **Method**: `GET`
- **URL**: `/api/statistics/investor-ratio/{stkCd}`
- **Path Parameter**:
  - `stkCd` (string, 필수): 종목 코드

##### 3-2. 응답 데이터 구조
```json
{
  "stkCd": "005930",
  "dataCount": 250,
  "frgnr": 25.3,
  "orgn": 18.2,
  "ind": 15.5,
  "fnncInvt": 8.2,
  "insrnc": 5.1,
  "invtrt": 4.8,
  "bank": 3.2,
  "etcFnnc": 2.1,
  "penfndEtc": 6.5,
  "samoFund": 4.2,
  "etcCorp": 3.8,
  "natn": 1.5,
  "natfor": 1.6,
  "message": null
}
```

##### 3-3. 에러 응답
```json
{
  "stkCd": "005930",
  "dataCount": 0,
  "message": "데이터가 없습니다."
}
```

#### 4. 개발 과정

##### 4-1. 데이터 조회 로직 구현
- **목표**: 특정 종목의 최신 데이터부터 지난 1년간 데이터를 조회.
- **Repository 메서드**:
```java
// StockInvestorChartRepository
List<StockInvestorChart> findByStkCdAndDtBetweenOrderByDtAsc(
    String stkCd, 
    LocalDate startDate, 
    LocalDate endDate
);
```

##### 4-2. 거래 비중 계산 로직 구현 (Java)
```java
// 1. 1년 범위 계산
LocalDate endDate = LocalDate.now();
LocalDate startDate = endDate.minusYears(1);

// 2. 데이터 조회
List<StockInvestorChart> data = repository.findByStkCdAndDtBetweenOrderByDtAsc(
    stkCd, startDate, endDate);

// 3. 투자자별 거래규모 계산
Map<String, BigDecimal> volumeMap = new HashMap<>();
for (StockInvestorChart row : data) {
    BigDecimal curPrc = row.getCurPrc();
    
    // 각 투자자별 |cur_prc * 순매수량| 누적
    volumeMap.merge("frgnr", curPrc.multiply(row.getFrgnrInvsr()).abs(), BigDecimal::add);
    volumeMap.merge("orgn", curPrc.multiply(row.getOrgn()).abs(), BigDecimal::add);
    volumeMap.merge("ind", curPrc.multiply(row.getIndInvsr()).abs(), BigDecimal::add);
    // ... 나머지 투자자 동일 패턴
}

// 4. 전체 거래규모 합계
BigDecimal totalVolume = volumeMap.values().stream()
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// 5. 비중 계산 (0으로 나누기 방지)
if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
    return emptyResponse();
}

Map<String, BigDecimal> ratioMap = new HashMap<>();
for (Map.Entry<String, BigDecimal> entry : volumeMap.entrySet()) {
    BigDecimal ratio = entry.getValue()
        .divide(totalVolume, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .setScale(1, RoundingMode.HALF_UP);
    ratioMap.put(entry.getKey(), ratio);
}
```

##### 4-3. API 컨트롤러 구현
```java
@GetMapping("/investor-ratio/{stkCd}")
public ResponseEntity<InvestorRatioResponse> getInvestorRatio(
        @PathVariable String stkCd) {
    
    log.info("투자자별 거래 비중 조회: 종목코드={}", stkCd);
    
    try {
        // 1년 범위 계산
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        
        // 데이터 조회
        List<StockInvestorChart> data = stockInvestorChartRepository
            .findByStkCdAndDtBetweenOrderByDtAsc(stkCd, startDate, endDate);
        
        if (data.isEmpty()) {
            return ResponseEntity.ok(InvestorRatioResponse.builder()
                .stkCd(stkCd)
                .dataCount(0)
                .message("데이터가 없습니다.")
                .build());
        }
        
        // 비중 계산 로직 호출
        InvestorRatioResponse response = calculateRatios(stkCd, data);
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("투자자별 거래 비중 계산 실패: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(InvestorRatioResponse.builder()
                .stkCd(stkCd)
                .message("서버 오류: " + e.getMessage())
                .build());
    }
}
```

##### 4-4. 응답 DTO
```java
@Builder
public record InvestorRatioResponse(
    String stkCd,
    Integer dataCount,
    BigDecimal frgnr,
    BigDecimal orgn,
    BigDecimal ind,
    BigDecimal fnncInvt,
    BigDecimal insrnc,
    BigDecimal invtrt,
    BigDecimal bank,
    BigDecimal etcFnnc,
    BigDecimal penfndEtc,
    BigDecimal samoFund,
    BigDecimal natn,
    BigDecimal etcCorp,
    BigDecimal natfor,
    String message
) {}
```

#### 5. 예외 처리

| 상황 | 처리 방법 |
|------|----------|
| 종목 코드 없음 | 400 Bad Request 반환 |
| 데이터 없음 | 빈 응답 + 메시지 반환 |
| totalVolume == 0 | 모든 비중을 0.0으로 반환 |
| 서버 오류 | 500 Internal Server Error + 에러 메시지 |

#### 6. 테스트 케이스

| 테스트 시나리오 | 예상 결과 |
|----------------|----------|
| 정상 종목 코드 (삼성전자: 005930) | 투자자별 비중 % 반환 |
| 데이터 없는 종목 코드 | `dataCount: 0`, 메시지 반환 |
| 잘못된 종목 코드 형식 | 400 Bad Request |
| 모든 거래량이 0인 경우 | 모든 비중 0.0 반환 |

---

### 참고 사항

#### 계산 공식
```
투자자 거래규모 = SUM(|cur_prc × 순매수량|) over 1년
비중(%) = (투자자 거래규모 / 전체 거래규모 합계) × 100
```

#### 제약 조건
- 데이터는 `tb_stock_investor_chart` 테이블에 최신 상태로 적재되어 있어야 함.
- 1년간 데이터가 없는 경우 비중 계산 불가.
- 비중은 소수점 첫째 자리까지 반올림하여 표시.

#### 확장 가능성
- 기간 파라미터 추가 (예: 6개월, 2년)
- 투자자별 추가 통계 (예: 평균 거래 금액) 제공

---

### 작업 일정 (수정)
| 작업 단계 | 예상 소요 시간 |
|----------|---------------|
| 요구사항 분석 | 0.5일 |
| 투자자 컬럼 매핑 확인 | 0.5일 |
| API 및 DTO 구현 | 1일 |
| 비중 계산 로직 구현 | 1일 |
| 테스트 및 검증 | 1일 |
| **총합** | **4일** |