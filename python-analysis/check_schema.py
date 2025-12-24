import psycopg2

def check_schema():
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
        
        tables = ['tb_stock_investor_invest_accumulation', 'tb_stock_list']
        
        for table in tables:
            print(f"\n--- Columns in {table} ---")
            cursor.execute(f"""
                SELECT column_name, data_type 
                FROM information_schema.columns 
                WHERE table_name = '{table}'
            """)
            cols = cursor.fetchall()
            for c in cols:
                print(f"{c[0]}: {c[1]}")
                
        # Try to find Korea Electric Power again with correct column
        # Let's guess the column based on output, but I'll add a lookup here assuming 'stock_code' or similar if I see it in output. 
        # But I can't interactively wait. I'll just print the columns first.
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_schema()
