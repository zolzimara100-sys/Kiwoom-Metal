-- =====================================================
-- 종목별 투자자 기관별 차트(ka10060) 테이블 생성
-- =====================================================

CREATE TABLE tb_stock_investor_chart (
    -- 1. Primary Keys (데이터 식별자)
    stk_cd          VARCHAR(20) NOT NULL,    -- 종목코드 (예: 005930, 039490_NX)
    dt              DATE        NOT NULL,    -- 일자 (YYYY-MM-DD)
    trde_tp         VARCHAR(1)  NOT NULL,    -- 매매구분 (0:순매수, 1:매수, 2:매도)
    amt_qty_tp      VARCHAR(1)  NOT NULL,    -- 금액수량구분 (1:금액, 2:수량)

    -- 2. Request Metadata
    unit_tp         VARCHAR(4)  NOT NULL,    -- 단위구분 (1:단주, 1000:천주)

    -- 3. Market Data (시세 정보)
    cur_prc         BIGINT,                  -- 현재가
    pred_pre        BIGINT,                  -- 전일대비
    acc_trde_prica  BIGINT,                  -- 누적거래대금

    -- 4. 주요 투자자별 데이터
    ind_invsr       BIGINT DEFAULT 0,        -- 개인투자자
    frgnr_invsr     BIGINT DEFAULT 0,        -- 외국인투자자
    orgn            BIGINT DEFAULT 0,        -- 기관계
    natfor          BIGINT DEFAULT 0,        -- 내외국인

    -- 5. 기관 세부 내역 (추이 분석용)
    fnnc_invt       BIGINT DEFAULT 0,        -- 금융투자
    insrnc          BIGINT DEFAULT 0,        -- 보험
    invtrt          BIGINT DEFAULT 0,        -- 투신
    etc_fnnc        BIGINT DEFAULT 0,        -- 기타금융
    bank            BIGINT DEFAULT 0,        -- 은행
    penfnd_etc      BIGINT DEFAULT 0,        -- 연기금등
    samo_fund       BIGINT DEFAULT 0,        -- 사모펀드
    natn            BIGINT DEFAULT 0,        -- 국가
    etc_corp        BIGINT DEFAULT 0,        -- 기타법인

    -- 6. System Audit
    reg_dt          TIMESTAMP DEFAULT NOW(), -- 등록일시
    upd_dt          TIMESTAMP,               -- 수정일시

    -- Primary Key 제약조건
    CONSTRAINT pk_tb_stock_investor_chart
        PRIMARY KEY (stk_cd, dt, trde_tp, amt_qty_tp)
) PARTITION BY RANGE (dt);

-- =====================================================
-- 파티션 테이블 생성 (월별 Range Partitioning)
-- =====================================================

-- 2024년 파티션
CREATE TABLE tb_stock_investor_chart_2024_01 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE tb_stock_investor_chart_2024_02 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE tb_stock_investor_chart_2024_03 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE tb_stock_investor_chart_2024_04 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE tb_stock_investor_chart_2024_05 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE tb_stock_investor_chart_2024_06 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE tb_stock_investor_chart_2024_07 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE tb_stock_investor_chart_2024_08 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE tb_stock_investor_chart_2024_09 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE tb_stock_investor_chart_2024_10 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE tb_stock_investor_chart_2024_11 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE tb_stock_investor_chart_2024_12 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- 2025년 파티션
CREATE TABLE tb_stock_investor_chart_2025_01 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE tb_stock_investor_chart_2025_02 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE tb_stock_investor_chart_2025_03 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE tb_stock_investor_chart_2025_04 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE tb_stock_investor_chart_2025_05 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE tb_stock_investor_chart_2025_06 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE tb_stock_investor_chart_2025_07 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE tb_stock_investor_chart_2025_08 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE tb_stock_investor_chart_2025_09 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE tb_stock_investor_chart_2025_10 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE tb_stock_investor_chart_2025_11 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE tb_stock_investor_chart_2025_12 PARTITION OF tb_stock_investor_chart
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- =====================================================
-- 인덱스 생성 (조회 성능 최적화)
-- =====================================================

-- 종목+일자 복합 조회
CREATE INDEX idx_chart_stk_dt ON tb_stock_investor_chart (stk_cd, dt);

-- 일자 조회
CREATE INDEX idx_chart_dt ON tb_stock_investor_chart (dt);

-- 외국인+기관 동반 매수 조회
CREATE INDEX idx_chart_frgnr_orgn ON tb_stock_investor_chart (dt, frgnr_invsr, orgn);

-- 연기금 순매수 조회
CREATE INDEX idx_chart_penfnd ON tb_stock_investor_chart (dt, penfnd_etc);

-- 기관 세부 유형별 조회
CREATE INDEX idx_chart_fnnc_invt ON tb_stock_investor_chart (dt, fnnc_invt);
CREATE INDEX idx_chart_insrnc ON tb_stock_investor_chart (dt, insrnc);
CREATE INDEX idx_chart_invtrt ON tb_stock_investor_chart (dt, invtrt);

-- =====================================================
-- 테이블 코멘트
-- =====================================================

COMMENT ON TABLE tb_stock_investor_chart IS '종목별 투자자 기관별 차트(ka10060) - 월별 파티션';
COMMENT ON COLUMN tb_stock_investor_chart.stk_cd IS '종목코드 (KRX:005930, NXT:005930_NX)';
COMMENT ON COLUMN tb_stock_investor_chart.dt IS '일자';
COMMENT ON COLUMN tb_stock_investor_chart.trde_tp IS '매매구분 (0:순매수, 1:매수, 2:매도)';
COMMENT ON COLUMN tb_stock_investor_chart.amt_qty_tp IS '금액수량구분 (1:금액, 2:수량)';
COMMENT ON COLUMN tb_stock_investor_chart.unit_tp IS '단위구분 (1:단주, 1000:천주)';
COMMENT ON COLUMN tb_stock_investor_chart.cur_prc IS '현재가';
COMMENT ON COLUMN tb_stock_investor_chart.pred_pre IS '전일대비';
COMMENT ON COLUMN tb_stock_investor_chart.acc_trde_prica IS '누적거래대금';
COMMENT ON COLUMN tb_stock_investor_chart.ind_invsr IS '개인투자자';
COMMENT ON COLUMN tb_stock_investor_chart.frgnr_invsr IS '외국인투자자';
COMMENT ON COLUMN tb_stock_investor_chart.orgn IS '기관계';
COMMENT ON COLUMN tb_stock_investor_chart.natfor IS '내외국인';
COMMENT ON COLUMN tb_stock_investor_chart.fnnc_invt IS '금융투자';
COMMENT ON COLUMN tb_stock_investor_chart.insrnc IS '보험';
COMMENT ON COLUMN tb_stock_investor_chart.invtrt IS '투신';
COMMENT ON COLUMN tb_stock_investor_chart.etc_fnnc IS '기타금융';
COMMENT ON COLUMN tb_stock_investor_chart.bank IS '은행';
COMMENT ON COLUMN tb_stock_investor_chart.penfnd_etc IS '연기금등';
COMMENT ON COLUMN tb_stock_investor_chart.samo_fund IS '사모펀드';
COMMENT ON COLUMN tb_stock_investor_chart.natn IS '국가';
COMMENT ON COLUMN tb_stock_investor_chart.etc_corp IS '기타법인';
