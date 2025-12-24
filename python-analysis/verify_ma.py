import psycopg2

def verify_ma_calculation():
    """
    Verify MA calculation for stk_cd = '015760', dates around 2021-11-08 ~ 2021-11-12.
    Expected:
    - Source data: 40881, -134303, -24846, -481581, 1343937
    - Sum = 744088
    - MA5 on 2021-11-12 should be 148817.6
    - MA5 on earlier dates (11-08 to 11-11) should be NULL (if they are the first 4 days of data for this stock)
    """
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
        
        print("=== Source Data (tb_stock_investor_chart) ===")
        cursor.execute("""
            SELECT stk_cd, dt, frgnr_invsr 
            FROM tb_stock_investor_chart 
            WHERE stk_cd = '015760' 
            ORDER BY dt
            LIMIT 10
        """)
        rows = cursor.fetchall()
        for r in rows:
            print(r)
            
        print("\n=== Calculated MA Data (tb_stock_investor_ma) ===")
        cursor.execute("""
            SELECT stk_cd, dt, frgnr_invsr_ma5, frgnr_invsr_ma10
            FROM tb_stock_investor_ma 
            WHERE stk_cd = '015760' 
            ORDER BY dt
            LIMIT 15
        """)
        rows = cursor.fetchall()
        for r in rows:
            print(r)
            
        print("\n=== Specific Check: 2021-11-08 ~ 2021-11-12 ===")
        cursor.execute("""
            SELECT stk_cd, dt, frgnr_invsr_ma5
            FROM tb_stock_investor_ma 
            WHERE stk_cd = '015760' 
              AND dt BETWEEN '20211108' AND '20211112'
            ORDER BY dt
        """)
        rows = cursor.fetchall()
        for r in rows:
            print(r)

        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    verify_ma_calculation()
