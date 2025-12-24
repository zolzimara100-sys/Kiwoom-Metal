-- PRD-0012 스키마 변경: 월 단위 컬럼 삭제 및 120, 140일 컬럼 추가

-- 1. 월 단위 컬럼 삭제 (12개 기간 x 12개 투자자 = 144개 컬럼 삭제)
DO $$ 
DECLARE 
    investors text[] := ARRAY['frgnr_invsr', 'orgn', 'fnnc_invt', 'insrnc', 'invtrt', 'etc_fnnc', 'bank', 'penfnd_etc', 'samo_fund', 'natn', 'etc_corp', 'natfor'];
    month_suffixes text[] := ARRAY['1m', '2m', '3m', '4m', '5m', '6m', '7m', '8m', '9m', '10m', '11m', '12m'];
    new_day_suffixes text[] := ARRAY['120', '140']; -- ma30, 40, 50, 90은 이미 있음
    inv text;
    suf text;
    col_name text;
BEGIN
    -- 월 단위 컬럼 삭제
    FOREACH inv IN ARRAY investors LOOP
        FOREACH suf IN ARRAY month_suffixes LOOP
            col_name := inv || '_ma' || suf;
            EXECUTE 'ALTER TABLE tb_stock_investor_ma DROP COLUMN IF EXISTS ' || col_name;
        END LOOP;
    END LOOP;

    -- 120일, 140일 컬럼 추가 (12개 투자자 x 2개 기간 = 24개 컬럼 추가)
    FOREACH inv IN ARRAY investors LOOP
        FOREACH suf IN ARRAY new_day_suffixes LOOP
            col_name := inv || '_ma' || suf;
            EXECUTE 'ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS ' || col_name || ' NUMERIC(15)';
        END LOOP;
    END LOOP;
END $$;
