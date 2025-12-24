import pandas as pd
from sqlalchemy import create_engine, text
import time
import sys

# ==========================================
# DB Configuration
# ==========================================
# Assuming local default settings based on typical environments.
# Please update these if your configuration differs.
DB_USER = "kiwoom"
DB_PASS = "kiwoom123"
DB_HOST = "localhost"
DB_PORT = "5432"
# Based on previous context, the database name might be 'kiwoom' or 'stock_trading'.
# Trying 'kiwoom' first as it matches the project name.
DB_NAME = "kiwoom" 

DB_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

def init_accumulation_data():
    print(f"Connecting to DB: {DB_URL}")
    try:
        engine = create_engine(DB_URL)
        with engine.connect() as conn:
            print("DB Connection Successful.")
    except Exception as e:
        print(f"DB Connection Failed: {e}")
        # Validating fallback to 'stock_trading' if kiwoom fails isn't easy in script without restart.
        # User should check this.
        return

    print("1. Truncating target table (tb_stock_investor_invest_accumulation)...")
    try:
        with engine.begin() as conn:
            conn.execute(text("TRUNCATE TABLE tb_stock_investor_invest_accumulation"))
    except Exception as e:
        print(f"Error truncating table: {e}")
        return
        
    print("2. Fetching stock list...")
    try:
        # Fetch distinct stocks from source table to ensure we process actual data
        stk_codes = pd.read_sql("SELECT DISTINCT stk_cd FROM tb_stock_investor_chart ORDER BY stk_cd", engine)
    except Exception as e:
        print(f"Error fetching stock list: {e}")
        return

    total = len(stk_codes)
    print(f"Total stocks to process: {total}")
    
    count = 0
    start_time = time.time()
    
    # Columns to process
    numeric_cols = [
        'ind_invsr', 'frgnr_invsr', 'orgn', 'fnnc_invt', 'insrnc', 'invtrt',
        'etc_fnnc', 'bank', 'penfnd_etc', 'samo_fund', 'natn', 'etc_corp', 'natfor'
    ]
    
    for idx, row in stk_codes.iterrows():
        stk = row['stk_cd']
        
        # Load Raw Data
        query = f"SELECT * FROM tb_stock_investor_chart WHERE stk_cd = '{stk}' ORDER BY dt ASC"
        df = pd.read_sql(query, engine)
        
        if df.empty:
            continue
            
        # 0. Pre-processing
        # Fill NaN for numeric calculations
        df[numeric_cols] = df[numeric_cols].fillna(0)
        # Handle cur_prc (Close Price)
        if 'cur_prc' not in df.columns:
             # Should not happen based on schema, but safety check
            print(f"Warning: cur_prc missing for {stk}")
            df['cur_prc'] = 0
            
        cur_prc = df['cur_prc'].fillna(0)
        
        # 1. Prepare Base DataFrame for Calculation
        calc_df = pd.DataFrame()
        calc_df['stk_cd'] = df['stk_cd']
        calc_df['dt'] = df['dt']
        
        # Copy metadata columns if they exist
        for meta_col in ['sector', 'category1', 'category2', 'category3']:
            if meta_col in df.columns:
                calc_df[meta_col] = df[meta_col]

        # Copy Daily Raw Data (Requirement 3.2 - Daily Net Buy Qty)
        for col in numeric_cols:
            calc_df[col] = df[col]
            
        # Foreign + Institution (Daily)
        calc_df['frgnr_invsr_orgn'] = df['frgnr_invsr'] + df['orgn']

        # 2. Calculate Cumulative Data (Requirement 3.2 & 3.3)
        for col in numeric_cols:
            # Daily Amount = Qty * Price
            daily_amt = df[col] * cur_prc
            
            # Cumulative Qty = cumsum(Daily Qty)
            calc_df[f"{col}_net_buy_qty"] = df[col].cumsum()
            
            # Cumulative Amount = cumsum(Daily Amount)
            # requirement: Truncate decimals (astype int64 helps, assuming no fractional currency in source)
            calc_df[f"{col}_net_buy_amount"] = daily_amt.cumsum().fillna(0).astype('int64')

        # Foreign + Institution (Cumulative)
        calc_df['frgnr_invsr_orgn_net_buy_qty'] = \
            calc_df['frgnr_invsr_net_buy_qty'] + calc_df['orgn_net_buy_qty']
            
        calc_df['frgnr_invsr_orgn_net_buy_amount'] = \
            calc_df['frgnr_invsr_net_buy_amount'] + calc_df['orgn_net_buy_amount']

        # 3. Bulk Insert
        try:
            # 'if_exists=append' adds to the table
            calc_df.to_sql('tb_stock_investor_invest_accumulation', engine, if_exists='append', index=False)
        except Exception as e:
            print(f"Error inserting {stk}: {e}")
            # Continue to next stock even if one fails
            continue
        
        count += 1
        if count % 10 == 0:
            print(f"Processed {count}/{total} - {stk}")
            
    elapsed = time.time() - start_time
    print(f"Done. Processed {count} stocks in {elapsed:.1f} seconds.")

if __name__ == "__main__":
    init_accumulation_data()
