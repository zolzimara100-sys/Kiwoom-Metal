import psycopg2

def list_tables_and_check_kepco():
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
        
        # Check tables
        cursor.execute("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
        """)
        tables = cursor.fetchall()
        print("Tables in DB:")
        for t in tables:
            print(t[0])
            
        # Check specific table user mentioned
        print("\nChecking for 'tb_stock_investor_invest_acccumulation':")
        found = False
        for t in tables:
            if t[0] == 'tb_stock_investor_invest_acccumulation':
                found = True
                break
        if found:
            print("Found tb_stock_investor_invest_acccumulation")
        else:
            print("Did NOT find tb_stock_investor_invest_acccumulation. Checking for similar names...")
            for t in tables:
                if 'investor' in t[0]:
                    print(f"Possible match: {t[0]}")

        # Check Stock Code for 한국전력
        print("\nChecking Stock Code for '한국전력':")
        # Assuming table is tb_stock_list or similar
        try:
            cursor.execute("SELECT stk_cd, stk_nm FROM tb_stock_list WHERE stk_nm = '한국전력'")
            res = cursor.fetchone()
            if res:
                print(f"Stock Code: {res[0]}, Name: {res[1]}")
            else:
                print("Could not find '한국전력' in tb_stock_list. Checking similar...")
                cursor.execute("SELECT stk_cd, stk_nm FROM tb_stock_list WHERE stk_nm LIKE '%전력%'")
                res = cursor.fetchall()
                for r in res:
                    print(f"Found: {r}")
        except Exception as e:
            print(f"Error checking stock list: {e}")

        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    list_tables_and_check_kepco()
