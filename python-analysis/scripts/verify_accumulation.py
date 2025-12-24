import pandas as pd
from sqlalchemy import create_engine
import sys

# DB Configuration
DB_USER = "kiwoom"
DB_PASS = "kiwoom123"
DB_HOST = "localhost"
DB_PORT = "5432"
DB_NAME = "kiwoom"

DB_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

def verify_data():
    print(f"Connecting to DB: {DB_URL}")
    try:
        engine = create_engine(DB_URL)
        conn = engine.connect()
    except Exception as e:
        print(f"DB Connection Failed: {e}")
        return

    # 1. Check Column Names
    print("\n[1] Checking Column Names in 'tb_stock_investor_invest_accumulation'...")
    try:
        # Fetch one row to get columns
        df_cols = pd.read_sql("SELECT * FROM tb_stock_investor_invest_accumulation LIMIT 1", conn)
        columns = df_cols.columns.tolist()
        
        # Check mapping for 'ind_invsr'
        print(f"Total Columns: {len(columns)}")
        
        expected_qty = 'ind_invsr_net_buy_qty'
        expected_amt = 'ind_invsr_net_buy_amount'
        
        if expected_qty in columns:
            print(f"SUCCESS: Found column '{expected_qty}'")
        else:
            print(f"FAILURE: Missing column '{expected_qty}'")
            
        if expected_amt in columns:
            print(f"SUCCESS: Found column '{expected_amt}'")
        else:
            print(f"FAILURE: Missing column '{expected_amt}'")
            
        print("Sample Columns:", columns[:5], "...", columns[-5:])
    except Exception as e:
        print(f"Error checking columns: {e}")
        return

    # 2. Verify Values (Logic Check)
    print("\n[2] Verifying Calculation Logic for a random stock...")
    try:
        # Get a random stock code from the table
        stk_cd_df = pd.read_sql("SELECT stk_cd FROM tb_stock_investor_invest_accumulation LIMIT 1", conn)
        target_stk = stk_cd_df['stk_cd'].iloc[0]
        print(f"Target Stock: {target_stk}")
        
        # Fetch Raw Data (First 5 days)
        raw_query = f"""
            SELECT dt, ind_invsr, cur_prc 
            FROM tb_stock_investor_chart 
            WHERE stk_cd = '{target_stk}' 
            ORDER BY dt ASC LIMIT 5
        """
        raw_df = pd.read_sql(raw_query, conn)
        
        # Fetch Accumulated Data (First 5 days)
        acc_query = f"""
            SELECT dt, ind_invsr_net_buy_qty, ind_invsr_net_buy_amount 
            FROM tb_stock_investor_invest_accumulation 
            WHERE stk_cd = '{target_stk}' 
            ORDER BY dt ASC LIMIT 5
        """
        acc_df = pd.read_sql(acc_query, conn)
        
        print("\n--- Data Comparison (First 5 Rows) ---")
        # Merge for comparison
        merged = pd.merge(raw_df, acc_df, on='dt')
        
        # Manual Calculation
        # Qty
        merged['manual_qty_cumsum'] = merged['ind_invsr'].cumsum()
        
        # Amount (Qty * Price) -> Cumsum
        merged['ind_invsr'] = merged['ind_invsr'].fillna(0)
        merged['cur_prc'] = merged['cur_prc'].fillna(0)
        merged['daily_amt'] = merged['ind_invsr'] * merged['cur_prc']
        merged['manual_amt_cumsum'] = merged['daily_amt'].cumsum().astype('int64')
        
        # Check
        merged['qty_match'] = merged['ind_invsr_net_buy_qty'] == merged['manual_qty_cumsum']
        merged['amt_match'] = merged['ind_invsr_net_buy_amount'] == merged['manual_amt_cumsum']
        
        print(merged[['dt', 'ind_invsr', 'ind_invsr_net_buy_qty', 'manual_qty_cumsum', 'qty_match']])
        print("-" * 50)
        print(merged[['dt', 'daily_amt', 'ind_invsr_net_buy_amount', 'manual_amt_cumsum', 'amt_match']])
        
        if merged['qty_match'].all() and merged['amt_match'].all():
            print("\n>>> VERIFICATION SUCCESS: All calculated values match.")
        else:
            print("\n>>> VERIFICATION FAILED: Mismatch detected.")
            
    except Exception as e:
        print(f"Error during value verification: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    verify_data()
