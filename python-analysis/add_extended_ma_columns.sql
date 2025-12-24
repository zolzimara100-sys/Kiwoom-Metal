-- PRD-0012: 이동평균값 확장 컬럼 추가
-- 투자자 12종 x 16개 기간 = 192개 컬럼 추가

-- 1. 외국인 (frgnr_invsr)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS frgnr_invsr_ma12m NUMERIC(15);

-- 2. 기관계 (orgn)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS orgn_ma12m NUMERIC(15);

-- 3. 금융투자 (fnnc_invt)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS fnnc_invt_ma12m NUMERIC(15);

-- 4. 보험 (insrnc)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS insrnc_ma12m NUMERIC(15);

-- 5. 투신 (invtrt)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS invtrt_ma12m NUMERIC(15);

-- 6. 기타금융 (etc_fnnc)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_fnnc_ma12m NUMERIC(15);

-- 7. 은행 (bank)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS bank_ma12m NUMERIC(15);

-- 8. 연기금등 (penfnd_etc)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS penfnd_etc_ma12m NUMERIC(15);

-- 9. 사모펀드 (samo_fund)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS samo_fund_ma12m NUMERIC(15);

-- 10. 국가 (natn)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natn_ma12m NUMERIC(15);

-- 11. 기타법인 (etc_corp)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS etc_corp_ma12m NUMERIC(15);

-- 12. 내국인 (natfor)
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma30 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma40 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma50 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma90 NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma1m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma2m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma3m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma4m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma5m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma6m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma7m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma8m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma9m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma10m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma11m NUMERIC(15);
ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS natfor_ma12m NUMERIC(15);
