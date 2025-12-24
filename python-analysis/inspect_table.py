import psycopg2

def check_structure():
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
        
        cursor.execute("""
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'tb_stock_investor_ma'
        """)
        cols = cursor.fetchall()
        col_names = [c[0] for c in cols]
        
        print(f"Table columns ({len(col_names)}):")
        print(col_names)
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_structure()
