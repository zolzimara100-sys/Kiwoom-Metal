
import pandas as pd
import psycopg2
from datetime import timedelta

def get_db_connection():
    return psycopg2.connect(
        host="localhost",
        database="kiwoom",
        user="kiwoom",
        password="kiwoom123",
        port="5432"
    )

def analyze_samo_fund_growth():
    conn = get_db_connection()
    
    # 1. Get the latest date
    query_date = "SELECT MAX(dt) as max_dt FROM tb_stock_investor_invest_accumulation"
    df_date = pd.read_sql(query_date, conn)
    latest_date = df_date['max_dt'][0]
    
    if not latest_date:
        print("No data found.")
        return

    # 2. Calculate 1 year ago date
    one_year_ago = latest_date - timedelta(days=365)
    
    print(f"Latest Date: {latest_date}")
    print(f"1 Year Ago Target: {one_year_ago}")

    # 3. Fetch data for Latest Date and Closest to 1 Year Ago
    # We'll fetch the row with dt <= one_year_ago ORDER BY dt DESC LIMIT 1 for each stock? 
    # Or just fetch all data around that time.
    # Efficient approach: Fetch all data for latest_date and a date closest to one_year_ago.
    
    # To find the actual available date closest to 1 year ago for the market (assuming most stocks share dates)
    query_past_date = f"""
        SELECT MAX(dt) as past_dt 
        FROM tb_stock_investor_invest_accumulation 
        WHERE dt <= '{one_year_ago}'
    """
    df_past_date = pd.read_sql(query_past_date, conn)
    past_date = df_past_date['past_dt'][0]
    print(f"Actual Past Date Used: {past_date}")

    # Fetch Data
    query = f"""
        SELECT 
            t.stk_cd, 
            m.name as stk_nm,
            t.dt, 
            t.samo_fund_net_buy_qty, 
            t.samo_fund_net_buy_amount
        FROM tb_stock_investor_invest_accumulation t
        LEFT JOIN tb_stock_list_meta m ON t.stk_cd = m.code
        WHERE t.dt = '{latest_date}' OR t.dt = '{past_date}'
    """
    df = pd.read_sql(query, conn)
    
    # Separate into Latest and Past
    df_latest = df[df['dt'] == latest_date].set_index('stk_cd')
    df_past = df[df['dt'] == past_date].set_index('stk_cd')

    # Merge
    merged = df_latest.join(df_past, lsuffix='_curr', rsuffix='_past')
    
    # Deduplicate based on index (stk_cd)
    merged = merged[~merged.index.duplicated(keep='first')]

    # Calculate Growth Rates
    # Growth Rate = (Curr - Past) / abs(Past) * 100
    
    # Qty
    merged['qty_diff'] = merged['samo_fund_net_buy_qty_curr'] - merged['samo_fund_net_buy_qty_past']
    # Use abs() for denominator to correctly represent direction of growth
    merged['qty_growth_rate'] = (merged['qty_diff'] / merged['samo_fund_net_buy_qty_past'].abs()) * 100
    
    # Amount
    merged['amt_diff'] = merged['samo_fund_net_buy_amount_curr'] - merged['samo_fund_net_buy_amount_past']
    merged['amt_growth_rate'] = (merged['amt_diff'] / merged['samo_fund_net_buy_amount_past'].abs()) * 100

    # formatting
    pd.set_option('display.max_rows', 50)
    pd.set_option('display.width', 1000)
    pd.set_option('display.float_format', '{:,.2f}'.format)

    print("\n\n=== [사모펀드] 최근 1년 순매수 수량(Qty) 증가율 상위 종목 ===")
    top_qty = merged.sort_values(by='qty_growth_rate', ascending=False).head(20)
    print(top_qty[['stk_nm_curr', 'samo_fund_net_buy_qty_past', 'samo_fund_net_buy_qty_curr', 'qty_diff', 'qty_growth_rate']])

    print("\n\n=== [사모펀드] 최근 1년 순매수 금액(Amount) 증가율 상위 종목 ===")
    top_amt = merged.sort_values(by='amt_growth_rate', ascending=False).head(20)
    print(top_amt[['stk_nm_curr', 'samo_fund_net_buy_amount_past', 'samo_fund_net_buy_amount_curr', 'amt_diff', 'amt_growth_rate']])

    conn.close()

if __name__ == "__main__":
    analyze_samo_fund_growth()
