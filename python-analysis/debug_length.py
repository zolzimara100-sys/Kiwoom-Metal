import psycopg2

def check_data_length():
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
        
        # Check dt length (cast to text)
        cursor.execute("SELECT dt, LENGTH(CAST(dt AS TEXT)) FROM tb_stock_investor_chart WHERE LENGTH(CAST(dt AS TEXT)) > 8 LIMIT 5")
        bad_dt = cursor.fetchall()
        if bad_dt:
            print("Found dt > 8 chars:", bad_dt)
        else:
            print("No dt > 8 chars found.")
            
        # Check stk_cd length
        cursor.execute("SELECT stk_cd, LENGTH(stk_cd) FROM tb_stock_investor_chart WHERE LENGTH(stk_cd) > 20 LIMIT 5")
        bad_stk = cursor.fetchall()
        if bad_stk:
            print("Found stk_cd > 20 chars:", bad_stk)
        else:
             print("No stk_cd > 20 chars found.")

        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_data_length()
