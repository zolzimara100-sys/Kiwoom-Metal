-- =====================================================
-- tb_stock_investor_chart 파티션 테이블을 일반 테이블로 변경
-- =====================================================

-- 1. 임시 백업 테이블 생성 (기존 데이터 보존)
CREATE TABLE tb_stock_investor_chart_backup AS
SELECT * FROM tb_stock_investor_chart;

-- 2. 기존 파티션 테이블 삭제 (CASCADE로 모든 파티션도 함께 삭제)
DROP TABLE tb_stock_investor_chart CASCADE;

-- 3. 일반 테이블로 재생성
CREATE TABLE tb_stock_investor_chart (
    -- 1. Primary Keys (데이터 식별자)
    stk_cd          VARCHAR(20) NOT NULL,    -- 종목코드 (예: 005930, 039490_NX)
    dt              DATE        NOT NULL,    -- 일자 (YYYY-MM-DD)

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
    CONSTRAINT tb_stock_investor_chart_pkey
        PRIMARY KEY (stk_cd, dt)
);

-- 4. 백업 데이터 복원 (컬럼 명시적 지정)
INSERT INTO tb_stock_investor_chart (
    stk_cd, dt, unit_tp,
    cur_prc, pred_pre, acc_trde_prica,
    ind_invsr, frgnr_invsr, orgn, natfor,
    fnnc_invt, insrnc, invtrt, etc_fnnc, bank, penfnd_etc, samo_fund, natn, etc_corp,
    reg_dt, upd_dt
)
SELECT
    stk_cd, dt, unit_tp,
    cur_prc, pred_pre, acc_trde_prica,
    ind_invsr, frgnr_invsr, orgn, natfor,
    fnnc_invt, insrnc, invtrt, etc_fnnc, bank, penfnd_etc, samo_fund, natn, etc_corp,
    reg_dt, upd_dt
FROM tb_stock_investor_chart_backup
ON CONFLICT (stk_cd, dt) DO NOTHING;

-- 5. 백업 테이블 삭제
DROP TABLE tb_stock_investor_chart_backup;

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

COMMENT ON TABLE tb_stock_investor_chart IS '종목별 투자자 기관별 차트(ka10060) - 일반 테이블';
COMMENT ON COLUMN tb_stock_investor_chart.stk_cd IS '종목코드 (KRX:005930, NXT:005930_NX)';
COMMENT ON COLUMN tb_stock_investor_chart.dt IS '일자';
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
