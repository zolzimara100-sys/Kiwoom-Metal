import psycopg2

def check_results():
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
        
        print("\n--- Sample Data Preview (First 3 rows) ---")
        cursor.execute("""
            SELECT stk_cd, dt, sector, 
                   frgnr_invsr_ma5, frgnr_invsr_ma60,
                   orgn_ma5, orgn_ma60
            FROM tb_stock_investor_ma
            ORDER BY stk_cd, dt DESC
            LIMIT 3
        """)
        rows = cursor.fetchall()
        for row in rows:
            print(row)
            
        print("\n--- Count by Sector (Top 5) ---")
        cursor.execute("""
            SELECT sector, COUNT(*) as cnt 
            FROM tb_stock_investor_ma 
            GROUP BY sector 
            ORDER BY cnt DESC 
            LIMIT 5
        """)
        sectors = cursor.fetchall()
        for s in sectors:
            print(s)

        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_results()
