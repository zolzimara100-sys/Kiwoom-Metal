import psycopg2
import pandas as pd

def verify_data():
    db_config = {
        'host': 'localhost',
        'port': 5432,
        'database': 'kiwoom',
        'user': 'kiwoom',
        'password': 'kiwoom123'
    }

    try:
        conn = psycopg2.connect(**db_config)
        
        # 1. Get stock code
        cursor = conn.cursor()
        cursor.execute("SELECT code FROM tb_stock_list WHERE name = '한국전력'")
        res = cursor.fetchone()
        if not res:
            print("Stock not found")
            return
        stk_cd = res[0]
        print(f"Stock Code: {stk_cd}")
        
        # 2. Get data for 2025
        # Using pandas for easier display
        query = f"""
            SELECT dt, 
                   orgn_net_buy_amount, 
                   frgnr_invsr_net_buy_amount 
            FROM tb_stock_investor_invest_accumulation 
            WHERE stk_cd = '{stk_cd}' 
              AND dt >= '2025-01-01'
            ORDER BY dt ASC
        """
        df = pd.read_sql(query, conn)
        
        print(f"\nData count since 2025-01-01: {len(df)}")
        if not df.empty:
            print(df.head())
            print(df.tail())
            
            # Check if values look cumulative
            # If they are cumulative, they should be roughly continuous. 
            # If daily, they will jump around 0.
            print("\nStats:")
            print(df.describe())
        else:
            print("No data found for 2025.")
            
            # Check if there is data at all
            cursor.execute(f"SELECT count(*) FROM tb_stock_investor_invest_accumulation WHERE stk_cd = '{stk_cd}'")
            count = cursor.fetchone()[0]
            print(f"Total rows for {stk_cd}: {count}")

        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    verify_data()
