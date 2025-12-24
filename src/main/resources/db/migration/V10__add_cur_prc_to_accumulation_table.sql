-- Add cur_prc column to tb_stock_investor_invest_accumulation
ALTER TABLE tb_stock_investor_invest_accumulation
ADD COLUMN IF NOT EXISTS cur_prc NUMERIC(18, 2);

-- Populate cur_prc column by joining with tb_stock_investor_chart
UPDATE tb_stock_investor_invest_accumulation acc
SET cur_prc = chart.cur_prc
FROM tb_stock_investor_chart chart
WHERE acc.stk_cd = chart.stk_cd
  AND acc.dt = chart.dt
  AND acc.cur_prc IS NULL;
