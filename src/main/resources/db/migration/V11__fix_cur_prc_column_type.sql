-- Fix cur_prc column type from NUMERIC to BIGINT
ALTER TABLE tb_stock_investor_invest_accumulation
ALTER COLUMN cur_prc TYPE BIGINT USING cur_prc::BIGINT;
