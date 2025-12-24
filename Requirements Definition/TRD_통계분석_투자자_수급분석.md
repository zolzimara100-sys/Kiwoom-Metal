# TRD: 통계분석 - 투자자 수급분석 (Investor Supply/Demand Analysis) System Design

## 1. Introduction
This Technical Requirements Document (TRD) outlines the system design and implementation details for the Investor Supply/Demand Analysis feature. It is based on the functional requirements defined in `PRD_통계분석_투자자_수급분석.md`.

## 2. Database Design

### 2.1. Schema: `tb_stock_investor_invest_accumulation`
This table stores the daily and cumulative net buy quantities and amounts for various investor types.

```sql
CREATE TABLE IF NOT EXISTS tb_stock_investor_invest_accumulation (
    stk_cd VARCHAR(20) NOT NULL,          -- Stock Code
    dt VARCHAR(8) NOT NULL,               -- Date (YYYYMMDD)
    sector VARCHAR(50),                   -- Sector
    category1 VARCHAR(50),                -- Category 1
    category2 VARCHAR(50),                -- Category 2
    category3 VARCHAR(50),                -- Category 3

    -- Daily Net Buy Quantity (Source: tb_stock_investor_chart)
    ind_invsr      BIGINT, -- Individual
    frgnr_invsr    BIGINT, -- Foreigner
    orgn           BIGINT, -- Institution Total
    fnnc_invt      BIGINT, -- Financial Investment
    insrnc         BIGINT, -- Insurance
    invtrt         BIGINT, -- Investment Trust
    etc_fnnc       BIGINT, -- Other Finance
    bank           BIGINT, -- Bank
    penfnd_etc     BIGINT, -- Pension Fund
    samo_fund      BIGINT, -- Private Equity
    natn           BIGINT, -- Nation
    etc_corp       BIGINT, -- Other Corporation
    natfor         BIGINT, -- Inner/Foreigner
    frgnr_invsr_orgn BIGINT, -- Foreigner + Institution (Calculated)

    -- Cumulative Net Buy Quantity (Calculated)
    ind_invsr_net_buy_qty      BIGINT,
    frgnr_invsr_net_buy_qty    BIGINT,
    orgn_net_buy_qty           BIGINT,
    fnnc_invt_net_buy_qty      BIGINT,
    insrnc_net_buy_qty         BIGINT,
    invtrt_net_buy_qty         BIGINT,
    etc_fnnc_net_buy_qty       BIGINT,
    bank_net_buy_qty           BIGINT,
    penfnd_etc_net_buy_qty     BIGINT,
    samo_fund_net_buy_qty      BIGINT,
    natn_net_buy_qty           BIGINT,
    etc_corp_net_buy_qty       BIGINT,
    natfor_net_buy_qty         BIGINT,
    frgnr_invsr_orgn_net_buy_qty BIGINT,

    -- Cumulative Net Buy Amount (Calculated: Previous + (Price * DailyQty))
    ind_invsr_net_buy_amount      BIGINT,
    frgnr_invsr_net_buy_amount    BIGINT,
    orgn_net_buy_amount           BIGINT,
    fnnc_invt_net_buy_amount      BIGINT,
    insrnc_net_buy_amount         BIGINT,
    invtrt_net_buy_amount         BIGINT,
    etc_fnnc_net_buy_amount       BIGINT,
    bank_net_buy_amount           BIGINT,
    penfnd_etc_net_buy_amount     BIGINT,
    samo_fund_net_buy_amount      BIGINT,
    natn_net_buy_amount           BIGINT,
    etc_corp_net_buy_amount       BIGINT,
    natfor_net_buy_amount         BIGINT,
    frgnr_invsr_orgn_net_buy_amount BIGINT,

    PRIMARY KEY (stk_cd, dt)
);

-- Index for performance (though PK covers most cases)
CREATE INDEX idx_accumulation_stk_dt ON tb_stock_investor_invest_accumulation(stk_cd, dt);
```

## 3. Backend Architecture

### 3.1. Components
*   **Controller**: `InvestorSupplyDemandController`
    *   Expose endpoints for analysis data.
*   **Service**: `InvestorSupplyDemandService`
    *   Handle business logic for accumulation calculation and on-demand updates.
*   **Repository**: `StockInvestorInvestAccumulationRepository` (JPA/MyBatis)
    *   CRUD operations for the new table.
*   **Repository**: `StockInvestorChartRepository`
    *   Source data access.

### 3.2. Data Processing Logic
#### 3.2.1. Initial Loading (One-time Migration)
*   **Trigger**: Explicit run (e.g., via a designated admin API or startup check).
*   **Logic**:
    1.  Fetch all data from `tb_stock_investor_chart` for KOSPI200, order by `stk_cd`, `dt` ASC.
    2.  Iterate through the sorted data stream.
    3.  Maintain running totals for each investor type (Quantity, Amount).
    4.  Amount Calculation: `RunningTotalAmount += (DailyQty * CurrentPrice)`. *Note: Truncate decimal points.*
    5.  Batch insert into `tb_stock_investor_invest_accumulation`.

#### 3.2.2. On-Demand Update (Delta Sync)
*   **Trigger**: When `GET /api/investor-supply-demand/{stk_cd}` is called.
*   **Logic**:
    1.  Get `max(dt)` from `tb_stock_investor_invest_accumulation` for the given `stk_cd`.
    2.  Get `max(dt)` from `tb_stock_investor_chart`.
    3.  If `Chart_MaxDate > Accum_MaxDate`, fetch rows from `tb_stock_investor_chart` where `dt > Accum_MaxDate`.
    4.  Fetch the last accumulated record (for initial values) from `tb_stock_investor_invest_accumulation`.
    5.  Calculate new rows accumulating from the last state.
    6.  Insert new rows.
    7.  Return requested data.

### 3.3. API Specifications

#### 3.3.1. Get Supply/Demand Analysis
*   **Endpoint**: `GET /api/v1/analysis/supply-demand`
*   **Parameters**:
    *   `stk_cd` (Required): Stock Code
    *   `start_dt` (Optional): Start Date (YYYYMMDD)
    *   `end_dt` (Optional): End Date (YYYYMMDD)
*   **Response**:
    ```json
    {
      "stockCode": "005930",
      "data": [
        {
          "date": "20230101",
          "individual": { "dailyQty": 100, "cumQty": 1000, "cumAmount": 50000000 },
          "foreigner": { ... },
          // ... other investors
        },
        // ...
      ],
      "summary": {
        "averagePrice": {
            "individual": 50000,
            "foreigner": 52000
            // (EndCumAmt - StartPrevCumAmt) / (EndCumQty - StartPrevCumQty)
        }
      }
    }
    ```

## 4. Frontend Architecture

### 4.1. Routes & Components
*   **Route**: `/moving-chart` (Unified Chart Page)
*   **Main Dashboard (Home)**:
    *   Add a new block/box for "Chart View" (Chart Analysis) similar to "Stock List" block.
    *   This block links to `/moving-chart`.
*   **Unified Chart Page (`/moving-chart`)**:
    *   **Layout**: Tabbed or button-based navigation to switch between:
        1.  **Moving Average Chart** (Existing logic)
        2.  **Investor Supply/Demand Chart** (New logic)
    *   **Components**:
        *   `ChartContainer`: Wrapper for switching between chart sub-components.
        *   `MovingAverageChart`: Existing component (refactored).
        *   `InvestorSupplyDemandChart`: New component for supply/demand analysis.
    *   **Shared Features**:
        *   Stock Search Bar.
        *   "+4 Years" Data Load Button (Connected to both views).

### 4.2. State Management
*   **Store**: Use existing React Query or Context state.
*   **Actions**:
    *   `fetchSupplyDemandData(stkCd, startDate, endDate)`
    *   `loadMoreHistory()` (Triggered by "+4 Years" button)

### 4.3. User Interaction
*   **Dual Axis Chart**: Display valid single chart with Cumulative Qty on Left Axis and Cumulative Amount on Right Axis.
*   **Investor Toggles**: Buttons to toggle visibility of each investor type (Individual, Foreigner, Institution, etc.), similar to Moving Average Chart.
*   **Zoom & Span**: Inherit the zoom, pan, and initial span features from the Moving Average Chart UX.
*   **History Load**: "+4 Years" button to fetch older data and prepend to current dataset.
*   **Data Formatting**: Implement a utility function to divide raw amount values by 100,000,000 (1억) before passing them to the chart components or displaying them in text.

## 5. Security & Performance
*   **Performance**: The `accumulate` logic should be optimized. For initial load, use batch processing (e.g., commit every 1000 records). For delta updates, since it's "On-Demand", ensure the query is fast (index scan).
*   **Error Handling**: Handle division by zero in Average Price calculation gracefully.

## 6. Implementation Steps
1.  **DB**: Run DDL for `tb_stock_investor_invest_accumulation`.
2.  **Backend**: Implement Entity, Repository, Service (Initial & Delta logic), Controller.
3.  **Frontend**: Update Route, Create Components, Integrate API.
