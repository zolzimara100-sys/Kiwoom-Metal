-- Add cur_prc column to tb_stock_investor_invest_accumulation
ALTER TABLE tb_stock_investor_invest_accumulation
ADD COLUMN cur_prc NUMERIC(18, 2);

-- Populate cur_prc column by joining with b_stock_investor_char
UPDATE tb_stock_investor_invest_accumulation
SET cur_prc = b.cur_prc
FROM b_stock_investor_char b
WHERE tb_stock_investor_invest_accumulation.stk_cd = b.stk_cd
  AND tb_stock_investor_invest_accumulation.dt = b.dt;