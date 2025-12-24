import psycopg2

def check_kepco():
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
        
        # 한국전력 종목코드 찾기
        print("=== 한국전력 종목 검색 ===")
        cursor.execute("SELECT code, name FROM tb_stock_list_meta WHERE name LIKE '%한국전력%'")
        stocks = cursor.fetchall()
        for s in stocks:
            print(f"코드: {s[0]}, 이름: {s[1]}")
        
        # tb_stock_investor_ma에 데이터 있는지 확인
        if stocks:
            stk_cd = stocks[0][0]
            print(f"\n=== tb_stock_investor_ma에서 {stk_cd} 데이터 조회 ===")
            cursor.execute(f"SELECT COUNT(*) FROM tb_stock_investor_ma WHERE stk_cd = '{stk_cd}'")
            count = cursor.fetchone()[0]
            print(f"데이터 건수: {count}")
            
            if count > 0:
                cursor.execute(f"SELECT stk_cd, dt, frgnr_invsr_ma5 FROM tb_stock_investor_ma WHERE stk_cd = '{stk_cd}' ORDER BY dt DESC LIMIT 5")
                rows = cursor.fetchall()
                print("최근 데이터:")
                for r in rows:
                    print(r)

        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_kepco()
