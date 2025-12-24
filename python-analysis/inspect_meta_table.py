import psycopg2

def inspect_meta_table():
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
            WHERE table_name = 'tb_stock_list_meta'
            ORDER BY ordinal_position
        """)
        cols = cursor.fetchall()
        col_names = [c[0] for c in cols]
        
        print(f"Source Table 'tb_stock_list_meta' columns:")
        print(col_names)
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    inspect_meta_table()
