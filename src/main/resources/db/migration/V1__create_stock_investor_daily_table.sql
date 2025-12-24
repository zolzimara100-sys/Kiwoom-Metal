-- =====================================================
-- 키움증권 투자자별 거래내역(ka10059) 테이블 생성
-- =====================================================

CREATE TABLE tb_stock_investor_daily (
    -- 1. Primary Keys (데이터 식별자)
    stk_cd          VARCHAR(10) NOT NULL,    -- [Request] 종목코드 (예: 005930)
    dt              DATE        NOT NULL,    -- [Request/Response] 일자 (YYYY-MM-DD)
    trde_tp         VARCHAR(1)  NOT NULL,    -- [Request] 매매구분 (0:순매수, 1:매수, 2:매도)
    amt_qty_tp      VARCHAR(1)  NOT NULL,    -- [Request] 금액수량구분 (1:금액, 2:수량)

    -- 2. Request Metadata (데이터 속성)
    unit_tp         VARCHAR(4)  NOT NULL,    -- [Request] 단위구분 (1:단주, 1000:천주) - 원본 데이터 단위 기록용

    -- 3. Market Data (시세 및 거래 정보 - Response Body)
    cur_prc         BIGINT,                  -- [Response] 현재가 (부호 제거됨)
    pre_sig         VARCHAR(1),              -- [Response] 대비기호 (1:상한, 2:상승 등)
    pred_pre        BIGINT,                  -- [Response] 전일대비
    flu_rt          NUMERIC(5, 2),           -- [Response] 등락율 (예: 6.98)
    acc_trde_qty    BIGINT,                  -- [Response] 누적거래량
    acc_trde_prica  BIGINT,                  -- [Response] 누적거래대금

    -- 4. Investor Breakdown (투자자별 수치 - Response Body)
    ind_invsr       BIGINT DEFAULT 0,        -- [Response] 개인투자자
    frgnr_invsr     BIGINT DEFAULT 0,        -- [Response] 외국인투자자
    orgn            BIGINT DEFAULT 0,        -- [Response] 기관계
    fnnc_invt       BIGINT DEFAULT 0,        -- [Response] 금융투자
    insrnc          BIGINT DEFAULT 0,        -- [Response] 보험
    invtrt          BIGINT DEFAULT 0,        -- [Response] 투신
    etc_fnnc        BIGINT DEFAULT 0,        -- [Response] 기타금융
    bank            BIGINT DEFAULT 0,        -- [Response] 은행
    penfnd_etc      BIGINT DEFAULT 0,        -- [Response] 연기금등
    samo_fund       BIGINT DEFAULT 0,        -- [Response] 사모펀드
    natn            BIGINT DEFAULT 0,        -- [Response] 국가
    etc_corp        BIGINT DEFAULT 0,        -- [Response] 기타법인
    natfor          BIGINT DEFAULT 0,        -- [Response] 내외국인

    -- 5. System Audit (관리 정보)
    reg_dt          TIMESTAMP DEFAULT NOW(), -- 데이터 적재 일시

    -- Primary Key 제약조건 설정
    CONSTRAINT pk_tb_stock_investor_daily
        PRIMARY KEY (stk_cd, dt, trde_tp, amt_qty_tp)
) PARTITION BY RANGE (dt);

-- =====================================================
-- 파티션 테이블 생성 (월별 Range Partitioning)
-- =====================================================

-- 2024년 파티션
CREATE TABLE tb_stock_investor_daily_2024_01 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE tb_stock_investor_daily_2024_02 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE tb_stock_investor_daily_2024_03 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE tb_stock_investor_daily_2024_04 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE tb_stock_investor_daily_2024_05 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE tb_stock_investor_daily_2024_06 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE tb_stock_investor_daily_2024_07 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE tb_stock_investor_daily_2024_08 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE tb_stock_investor_daily_2024_09 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE tb_stock_investor_daily_2024_10 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE tb_stock_investor_daily_2024_11 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE tb_stock_investor_daily_2024_12 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025년 파티션
CREATE TABLE tb_stock_investor_daily_2025_01 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE tb_stock_investor_daily_2025_02 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE tb_stock_investor_daily_2025_03 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE tb_stock_investor_daily_2025_04 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE tb_stock_investor_daily_2025_05 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE tb_stock_investor_daily_2025_06 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE tb_stock_investor_daily_2025_07 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE tb_stock_investor_daily_2025_08 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE tb_stock_investor_daily_2025_09 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE tb_stock_investor_daily_2025_10 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE tb_stock_investor_daily_2025_11 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE tb_stock_investor_daily_2025_12 PARTITION OF tb_stock_investor_daily
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- =====================================================
-- 인덱스 생성 (조회 성능 최적화)
-- =====================================================

-- 기간 조회 성능 향상
CREATE INDEX idx_tb_stock_investor_daily_dt
    ON tb_stock_investor_daily (dt);

-- 특정 종목 조회 성능 향상
CREATE INDEX idx_tb_stock_investor_daily_stk_cd
    ON tb_stock_investor_daily (stk_cd);

-- 종목 + 기간 복합 조회 성능 향상
CREATE INDEX idx_tb_stock_investor_daily_stk_dt
    ON tb_stock_investor_daily (stk_cd, dt);

-- =====================================================
-- 테이블 코멘트
-- =====================================================

COMMENT ON TABLE tb_stock_investor_daily IS '키움증권 투자자별 거래내역(ka10059) - 월별 파티션';
COMMENT ON COLUMN tb_stock_investor_daily.stk_cd IS '종목코드';
COMMENT ON COLUMN tb_stock_investor_daily.dt IS '조회일자';
COMMENT ON COLUMN tb_stock_investor_daily.trde_tp IS '매매구분 (0:순매수, 1:매수, 2:매도)';
COMMENT ON COLUMN tb_stock_investor_daily.amt_qty_tp IS '금액수량구분 (1:금액, 2:수량)';
COMMENT ON COLUMN tb_stock_investor_daily.ind_invsr IS '개인투자자';
COMMENT ON COLUMN tb_stock_investor_daily.frgnr_invsr IS '외국인투자자';
COMMENT ON COLUMN tb_stock_investor_daily.orgn IS '기관계';
