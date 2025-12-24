-- 종목 리스트 테이블 생성
CREATE TABLE tb_stock_list (
    code VARCHAR(20) NOT NULL,
    name VARCHAR(40),
    list_count BIGINT,
    audit_info VARCHAR(20),
    reg_day VARCHAR(20),
    last_price BIGINT,
    state VARCHAR(20),
    market_code VARCHAR(20),
    market_name VARCHAR(20),
    up_name VARCHAR(20),
    up_size_name VARCHAR(20),
    company_class_name VARCHAR(20),
    order_warning VARCHAR(20),
    nxt_enable VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (code)
);

-- 인덱스 생성
CREATE INDEX idx_market_code ON tb_stock_list(market_code);
CREATE INDEX idx_market_name ON tb_stock_list(market_name);
CREATE INDEX idx_state ON tb_stock_list(state);

COMMENT ON TABLE tb_stock_list IS '종목 정보 Master 테이블';
COMMENT ON COLUMN tb_stock_list.code IS '종목코드 (단축코드)';
COMMENT ON COLUMN tb_stock_list.name IS '종목명';
COMMENT ON COLUMN tb_stock_list.list_count IS '상장주식수';
COMMENT ON COLUMN tb_stock_list.audit_info IS '감리구분';
COMMENT ON COLUMN tb_stock_list.reg_day IS '상장일';
COMMENT ON COLUMN tb_stock_list.last_price IS '전일종가';
COMMENT ON COLUMN tb_stock_list.state IS '종목상태';
COMMENT ON COLUMN tb_stock_list.market_code IS '시장구분코드';
COMMENT ON COLUMN tb_stock_list.market_name IS '시장명';
COMMENT ON COLUMN tb_stock_list.up_name IS '업종명';
COMMENT ON COLUMN tb_stock_list.up_size_name IS '회사크기분류';
COMMENT ON COLUMN tb_stock_list.company_class_name IS '회사분류 (코스닥만)';
COMMENT ON COLUMN tb_stock_list.order_warning IS '투자유의종목여부 (0:해당없음, 2:정리매매, 3:단기과열, 4:투자위험, 5:투자경과, 1:ETF투자주의)';
COMMENT ON COLUMN tb_stock_list.nxt_enable IS 'NXT가능여부 (Y:가능)';
