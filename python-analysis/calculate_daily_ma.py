import psycopg2
import time

def generate_ma_calculation_query():
    """
    Generate SQL query for Simple Moving Average (SMA) calculation.
    - NO cumulative calculation.
    - MA5 = Average of (current day + past 4 days) of ORIGINAL daily net values.
    - Days with less than N data points get NULL for MAN.
    """
    
    # 1. Target Investors Mapping (source_column, target_prefix)
    investors = [
        ('frgnr_invsr', 'frgnr_invsr'),
        ('orgn', 'orgn'),
        ('fnnc_invt', 'fnnc_invt'),
        ('insrnc', 'insrnc'),
        ('invtrt', 'invtrt'),
        ('etc_fnnc', 'etc_fnnc'),
        ('bank', 'bank'),
        ('penfnd_etc', 'penfnd_etc'),
        ('samo_fund', 'samo_fund'),
        ('natn', 'natn'),
        ('etc_corp', 'etc_corp'),
        ('natfor', 'natfor')
    ]
    periods = [5, 10, 20, 60]

    # --- Build MA Select Clauses ---
    # Logic: Use CASE WHEN to ensure we only calculate MA when we have enough data points.
    # row_idx is the row number for each stock ordered by date.
    # For MA5, we need row_idx >= 5 (meaning 5th day or later).
    ma_selects = []
    
    for src_col, target_prefix in investors:
        for p in periods:
            # AVG of original column over window: current row + (p-1) preceding rows
            col_expr = f"""
        CASE 
            WHEN row_idx >= {p} THEN ROUND(AVG({src_col}) OVER (PARTITION BY stk_cd ORDER BY dt ROWS BETWEEN {p-1} PRECEDING AND CURRENT ROW), 2)
            ELSE NULL 
        END AS {target_prefix}_ma{p}"""
            ma_selects.append(col_expr)

    ma_selects_str = ",".join(ma_selects)

    # --- Construct Full Query ---
    query = f"""
    WITH base_data AS (
        SELECT 
            c.stk_cd,
            c.dt,
            TO_CHAR(c.dt, 'YYYYMMDD') as dt_str,
            m.sector,
            m.main as category1,
            m.sub as category2,
            m.detail as category3,
            -- Row number for each stock, ordered by date
            ROW_NUMBER() OVER (PARTITION BY c.stk_cd ORDER BY c.dt) as row_idx,
            -- Original investor columns (NOT cumulative)
            c.frgnr_invsr, c.orgn, c.fnnc_invt, c.insrnc, c.invtrt, c.etc_fnnc,
            c.bank, c.penfnd_etc, c.samo_fund, c.natn, c.etc_corp, c.natfor,
            c.cur_prc -- Add Current Price
        FROM tb_stock_investor_chart c
        LEFT JOIN tb_stock_list_meta m ON c.stk_cd = m.code
    )
    INSERT INTO tb_stock_investor_ma (
        stk_cd, dt, sector, category1, category2, category3, cur_prc,
        {', '.join([f'{target}_ma{p}' for _, target in investors for p in periods])}
    )
    SELECT
        stk_cd, 
        dt_str,
        sector, category1, category2, category3, cur_prc,
        {ma_selects_str}
    FROM base_data
    """
    
    return query

def calculate_daily_ma():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    try:
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()
        
        start_time = time.time()
        print("[Step 1] Initializing Simple Moving Average Calculation...")
        
        # 0. Schema Migration
        print("[Step 0] Checking Schema (Adding cur_prc column if not exists)...")
        cursor.execute("ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS cur_prc BIGINT")
        conn.commit()
        
        # 1. Truncate
        print("[Step 2] Truncating target table 'tb_stock_investor_ma'...")
        cursor.execute("TRUNCATE TABLE tb_stock_investor_ma")
        
        # 2. Execute Calculation
        print("[Step 3] Calculating Simple Moving Average (SMA) for all investors...")
        print("         (This calculates AVG of original daily net values, NOT cumulative)")
        
        insert_sql = generate_ma_calculation_query()
        cursor.execute(insert_sql)
        row_count = cursor.rowcount
        
        conn.commit()
        
        elapsed = time.time() - start_time
        print(f"[Success] Task Completed!")
        print(f" - Rows Inserted: {row_count}")
        print(f" - Total Time: {elapsed:.2f} seconds")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"[Error] {e}")

if __name__ == "__main__":
    calculate_daily_ma()
