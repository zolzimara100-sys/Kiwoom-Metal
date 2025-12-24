CREATE TABLE IF NOT EXISTS tb_stock_investor_ma (
    stk_cd VARCHAR(20) NOT NULL,
    dt VARCHAR(8) NOT NULL,
    sector VARCHAR(50), 

    -- 외국인 (Foreigner)
    frgnr_invsr_ma5 NUMERIC(15,2),
    frgnr_invsr_ma10 NUMERIC(15,2),
    frgnr_invsr_ma20 NUMERIC(15,2),
    frgnr_invsr_ma60 NUMERIC(15,2),

    -- 기관계 (Organ)
    orgn_ma5 NUMERIC(15,2),
    orgn_ma10 NUMERIC(15,2),
    orgn_ma20 NUMERIC(15,2),
    orgn_ma60 NUMERIC(15,2),

    -- 금융투자 (Financial Investment)
    fnnc_invt_ma5 NUMERIC(15,2),
    fnnc_invt_ma10 NUMERIC(15,2),
    fnnc_invt_ma20 NUMERIC(15,2),
    fnnc_invt_ma60 NUMERIC(15,2),

    -- 보험 (Insurance)
    insrnc_ma5 NUMERIC(15,2),
    insrnc_ma10 NUMERIC(15,2),
    insrnc_ma20 NUMERIC(15,2),
    insrnc_ma60 NUMERIC(15,2),

    -- 투신 (Investment Trust)
    invtrt_ma5 NUMERIC(15,2),
    invtrt_ma10 NUMERIC(15,2),
    invtrt_ma20 NUMERIC(15,2),
    invtrt_ma60 NUMERIC(15,2),

    -- 기타금융 (Etc Finance)
    etc_fnnc_ma5 NUMERIC(15,2),
    etc_fnnc_ma10 NUMERIC(15,2),
    etc_fnnc_ma20 NUMERIC(15,2),
    etc_fnnc_ma60 NUMERIC(15,2),

    -- 은행 (Bank)
    bank_ma5 NUMERIC(15,2),
    bank_ma10 NUMERIC(15,2),
    bank_ma20 NUMERIC(15,2),
    bank_ma60 NUMERIC(15,2),

    -- 연기금등 (Pension Fund)
    penfnd_etc_ma5 NUMERIC(15,2),
    penfnd_etc_ma10 NUMERIC(15,2),
    penfnd_etc_ma20 NUMERIC(15,2),
    penfnd_etc_ma60 NUMERIC(15,2),

    -- 사모펀드 (Private Equity)
    samo_fund_ma5 NUMERIC(15,2),
    samo_fund_ma10 NUMERIC(15,2),
    samo_fund_ma20 NUMERIC(15,2),
    samo_fund_ma60 NUMERIC(15,2),

    -- 국가 (National)
    natn_ma5 NUMERIC(15,2),
    natn_ma10 NUMERIC(15,2),
    natn_ma20 NUMERIC(15,2),
    natn_ma60 NUMERIC(15,2),

    -- 기타법인 (Etc Corp)
    etc_corp_ma5 NUMERIC(15,2),
    etc_corp_ma10 NUMERIC(15,2),
    etc_corp_ma20 NUMERIC(15,2),
    etc_corp_ma60 NUMERIC(15,2),

    -- 기타외국인 (Net Foreigner etc)
    natfor_ma5 NUMERIC(15,2),
    natfor_ma10 NUMERIC(15,2),
    natfor_ma20 NUMERIC(15,2),
    natfor_ma60 NUMERIC(15,2),

    CONSTRAINT pk_tb_stock_investor_ma PRIMARY KEY (stk_cd, dt)
);

CREATE INDEX IF NOT EXISTS idx_stock_investor_ma_dt ON tb_stock_investor_ma(dt);
CREATE INDEX IF NOT EXISTS idx_stock_investor_ma_sector ON tb_stock_investor_ma(sector);
