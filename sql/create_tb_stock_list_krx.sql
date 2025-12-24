-- KRX 종목 마스터 테이블 생성
-- 한국거래소 상장 종목 정보 및 KSIC 산업분류 정보 포함

DROP TABLE IF EXISTS tb_stock_list_krx;

CREATE TABLE tb_stock_list_krx (
    symbol VARCHAR(10) NOT NULL,                    -- 종목코드 (6자리, PK)
    name VARCHAR(100) NOT NULL,                     -- 종목명
    market VARCHAR(10) NOT NULL,                    -- 시장구분 (KOSPI/KOSDAQ/KONEX)
    listing BOOLEAN NOT NULL DEFAULT true,         -- 상장여부 (true: 상장, false: 상장폐지)
    industry VARCHAR(500),                          -- 거래소 산업구분 (사업내용)
    sector VARCHAR(100),                            -- 거래소 섹터 구분
    industry_code VARCHAR(10),                      -- DART 산업분류 코드 (KSIC)
    industry_name VARCHAR(200),                     -- DART 산업 이름
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 생성일시
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 수정일시
    
    CONSTRAINT pk_stock_list_krx PRIMARY KEY (symbol)
);

-- 인덱스 생성
CREATE INDEX idx_stock_list_krx_market ON tb_stock_list_krx(market);
CREATE INDEX idx_stock_list_krx_listing ON tb_stock_list_krx(listing);
CREATE INDEX idx_stock_list_krx_sector ON tb_stock_list_krx(sector);
CREATE INDEX idx_stock_list_krx_industry_code ON tb_stock_list_krx(industry_code);

-- 테이블 코멘트 추가
COMMENT ON TABLE tb_stock_list_krx IS 'KRX 종목 마스터 - 한국거래소 상장종목 및 KSIC 산업분류 정보';
COMMENT ON COLUMN tb_stock_list_krx.symbol IS '종목코드 (6자리)';
COMMENT ON COLUMN tb_stock_list_krx.name IS '종목명';
COMMENT ON COLUMN tb_stock_list_krx.market IS '시장구분 (KOSPI/KOSDAQ/KONEX)';
COMMENT ON COLUMN tb_stock_list_krx.listing IS '상장여부 (true: 상장, false: 상장폐지)';
COMMENT ON COLUMN tb_stock_list_krx.industry IS '거래소 산업구분 (회사 사업내용)';
COMMENT ON COLUMN tb_stock_list_krx.sector IS '거래소 섹터 구분';
COMMENT ON COLUMN tb_stock_list_krx.industry_code IS 'DART 산업분류 코드 (KSIC)';
COMMENT ON COLUMN tb_stock_list_krx.industry_name IS 'DART 산업 이름';