-- state 컬럼 크기 변경 (VARCHAR(20) -> VARCHAR(100))
ALTER TABLE tb_stock_list ALTER COLUMN state TYPE VARCHAR(100);
