import psycopg2
import pandas as pd
import numpy as np
import time
import sys
import os
from datetime import datetime

# DB Config
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', '5432')),
    'database': os.getenv('DB_NAME', 'kiwoom'),
    'user': os.getenv('DB_USER', 'kiwoom'),
    'password': os.getenv('DB_PASSWORD', 'kiwoom123')
}

# Investor Types Mapping (DB Column Name -> Corr Column Prefix)
INVESTOR_COLS = [
    'frgnr_invsr', 'orgn', 'ind_invsr', 
    'fnnc_invt', 'insrnc', 'invtrt', 'etc_fnnc', 
    'bank', 'penfnd_etc', 'samo_fund', 'natn', 
    'etc_corp', 'natfor'
]

# Analysis Periods (Window Sizes)
# Note: Correlation needs minimal data points. 5 days is very volatile, but included as per request.
CORR_PERIODS = [5, 10, 20, 60]

def get_db_connection():
    return psycopg2.connect(**DB_CONFIG)

def setup_database():
    """Create the target table if it doesn't exist."""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    print("[Step 1] Setting up Database Schema...")
    
    # Generate column definitions for all investors
    # e.g., frgnr_invsr_corr DECIMAL(5,3)
    corr_cols = []
    for inv in INVESTOR_COLS:
        corr_cols.append(f"{inv}_corr DECIMAL(5,3)")
    
    corr_cols_sql = ",\n        ".join(corr_cols)

    ddl = f"""
    CREATE TABLE IF NOT EXISTS tb_stock_investor_corr_daily (
        stk_cd VARCHAR(20) NOT NULL,
        dt VARCHAR(8) NOT NULL,
        corr_days INT NOT NULL,
        
        cur_prc BIGINT,
        sector VARCHAR(50), 
        category1 VARCHAR(50), 
        category2 VARCHAR(50), 
        category3 VARCHAR(50),
        
        {corr_cols_sql},
        
        reg_dt TIMESTAMP DEFAULT NOW(),
        
        PRIMARY KEY (stk_cd, dt, corr_days)
    );
    
    CREATE INDEX IF NOT EXISTS idx_corr_stk_dt ON tb_stock_investor_corr_daily(stk_cd, dt);
    """
    
    cursor.execute(ddl)
    conn.commit()
    cursor.close()
    conn.close()
    print(" - Table 'tb_stock_investor_corr_daily' is ready.")

def fetch_stock_data(target_stock_code=None):
    """Fetch all necessary data for calculation."""
    conn = get_db_connection()

    print("[Step 2] Fetching source data from tb_stock_investor_chart...")

    # Prepare investor columns string
    investor_cols_str = ', '.join([f'c.{col}' for col in INVESTOR_COLS])

    # Prepare where clause
    if target_stock_code:
        where_clause = f"WHERE c.stk_cd = '{target_stock_code}'"
    else:
        where_clause = ""

    query = f"""
        SELECT
            c.stk_cd,
            TO_CHAR(c.dt, 'YYYYMMDD') as dt,
            c.cur_prc,
            m.sector, m.main, m.sub, m.detail,
            {investor_cols_str}
        FROM tb_stock_investor_chart c
        LEFT JOIN tb_stock_list_meta m ON c.stk_cd = m.code
        {where_clause}
        ORDER BY c.stk_cd, c.dt
    """

    # Use pandas for efficient processing
    df = pd.read_sql(query, conn)
    conn.close()

    print(f" - Loaded {len(df)} rows.")
    return df

def calculate_and_save(target_stock_code=None):
    setup_database()
    df = fetch_stock_data(target_stock_code)
    
    print("[Step 3] Calculating Rolling Correlations...")
    start_time = time.time()
    
    # List to hold data for bulk insert
    # Using execute_batch for performance if possible, or constructing large Insert statement
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    if target_stock_code:
        print(f" - [Single Mode] Deleting existing data for {target_stock_code}...")
        cursor.execute(f"DELETE FROM tb_stock_investor_corr_daily WHERE stk_cd = '{target_stock_code}'")
    else:
        # Clean existing data? Optional. Let's truncate for clean slat or use ON CONFLICT.
        # User might want cumulative updates, but for now, full recalculation is safer.
        print(" - [Full Mode] Truncating table...")
        cursor.execute("TRUNCATE TABLE tb_stock_investor_corr_daily")
    conn.commit()
    
    # Group by Stock Code
    grouped = df.groupby('stk_cd')
    total_stocks = len(grouped)
    processed_count = 0
    
    # Prepare Insert Query
    cols = ['stk_cd', 'dt', 'corr_days', 'cur_prc', 'sector', 'category1', 'category2', 'category3'] + [f"{c}_corr" for c in INVESTOR_COLS]
    placeholders = ",".join(["%s"] * len(cols))
    insert_sql = f"INSERT INTO tb_stock_investor_corr_daily ({','.join(cols)}) VALUES ({placeholders})"
    
    batch_data = []
    BATCH_SIZE = 10000
    
    for stk_cd, group in grouped:
        # Sort by date just in case
        group = group.sort_values('dt')
        
        # 1. Calculate Daily Returns (Percentage Change of Price)
        # using 'cur_prc'. pct_change() computes (curr - prev) / prev
        group['price_ret'] = group['cur_prc'].pct_change()
        
        # Handle Inf or NaN in returns
        group['price_ret'] = group['price_ret'].replace([np.inf, -np.inf], np.nan).fillna(0)
        
        # 2. Iterate over periods
        for period in CORR_PERIODS:
            # Skip if not enough data
            if len(group) < period:
                continue
                
            temp_df = pd.DataFrame()
            temp_df['stk_cd'] = group['stk_cd']
            temp_df['dt'] = group['dt']
            temp_df['corr_days'] = period
            temp_df['cur_prc'] = group['cur_prc']
            temp_df['sector'] = group['sector']
            temp_df['category1'] = group['main']
            temp_df['category2'] = group['sub']
            temp_df['category3'] = group['detail']
            
            # Calculate Correlation for each investor type
            for inv_col in INVESTOR_COLS:
                # Rolling Correlation between Price Return and Investor Net Buy (Absolute Amount)
                # Note: Investor value is Net Buy Amount/Volume. 
                # Correlations are scale-invariant, so raw values are fine.
                
                # Rolling correlation
                # price_ret vs inv_col
                corr_series = group['price_ret'].rolling(window=period).corr(group[inv_col])
                
                # Replace NaN with None (NULL in DB)
                # Round to 3 decimal places
                temp_df[f"{inv_col}_corr"] = corr_series.round(3)
            
            # Drop rows where all correlations are NaN (initial N days)? 
            # Or keep them with NULLs? Let's drop rows where calc is impossible (start of series)
            # Actually, rolling result is NaN for first (period-1) rows.
            # We filter rows where we have at least valid date.
            
            
            # Replace Infinity with NaN before insertion
            temp_df.replace([np.inf, -np.inf], np.nan, inplace=True)
            
            # Convert to list of tuples for insertion
            # We only insert rows that have at least one valid correlation or valid data
            records = temp_df.where(pd.notnull(temp_df), None).values.tolist()
            
            # Filter out initial NaNs if desired, but for charting continuity, keeping NULLs might be okay.
            # However, DB expects 'dt' and PK.
            # Let's Insert ALL rows including initial NaNs (frontend will handle gaps).
            
            batch_data.extend(records)
            
            if len(batch_data) >= BATCH_SIZE:
                 cursor.executemany(insert_sql, batch_data)
                 conn.commit()
                 batch_data = []

        processed_count += 1
        if processed_count % 100 == 0:
            print(f" - Processed {processed_count}/{total_stocks} stocks...")
            
    # Insert remaining
    if batch_data:
        cursor.executemany(insert_sql, batch_data)
        conn.commit()
        
    cursor.close()
    conn.close()
    
    elapsed = time.time() - start_time
    print(f"[Success] Correlation Analysis Completed in {elapsed:.2f} seconds.")

if __name__ == "__main__":
    try:
        target_stk = None
        if len(sys.argv) > 1:
            target_stk = sys.argv[1]
            print(f"Args: Target Stock = {target_stk}")
            
        calculate_and_save(target_stock_code=target_stk)
    except Exception as e:
        print(f"[Error] {e}")
        import traceback
        traceback.print_exc()

