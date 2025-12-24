"""
AI Infra 섹터 종목 60일 분석 스크립트
"""

import psycopg2
from decimal import Decimal

def analyze_ai_infra_60days():
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
        
        print("=" * 100)
        print("AI Infra 섹터 종목 60일 분석")
        print("조건: detail = 'ai_infra' AND main = 'SECTOR'")
        print("=" * 100)
        
        cursor.execute("""
            SELECT code, name FROM tb_stock_list_meta
            WHERE detail = 'ai_infra' AND main = 'SECTOR' ORDER BY code
        """)
        stocks = cursor.fetchall()
        
        print(f"\n대상 종목: {', '.join([f'{s[0]}({s[1]})' for s in stocks])}")
        
        stock_codes_str = ", ".join([f"'{s[0]}'" for s in stocks])
        
        query = f"""
            SELECT dt, SUM(cur_prc * frgnr_invsr) as total_amount
            FROM tb_stock_investor_chart
            WHERE stk_cd IN ({stock_codes_str}) AND dt <= '20251210'
            GROUP BY dt ORDER BY dt DESC LIMIT 60
        """
        
        cursor.execute(query)
        daily_data = sorted(cursor.fetchall(), key=lambda x: x[0])
        
        print(f"\n조회된 거래일 수: {len(daily_data)}일")
        print("-" * 70)
        print(f"{'#':<5} {'날짜':<15} {'합계 (cur_prc * frgnr_invsr)':<35}")
        print("-" * 70)
        
        total = Decimal('0')
        amounts = []
        
        for idx, (dt, amount) in enumerate(daily_data, 1):
            if amount:
                total += Decimal(str(amount))
                amounts.append((str(dt), Decimal(str(amount))))
            if idx <= 5 or idx > len(daily_data) - 5:
                print(f"{idx:<5} {str(dt):<15} {amount:>35,.0f}")
            elif idx == 6:
                print(f"{'...':<5} {'...':<15} {'... (중간 생략) ...':<35}")
        
        print("-" * 70)
        
        count = len(amounts)
        ma_60 = total / count if count > 0 else 0
        
        print(f"\n★ 60일 이동평균 (MA60) = {total:,.0f} / {count}")
        print(f"                       = {ma_60:,.0f}원")
        
        max_item = max(amounts, key=lambda x: x[1])
        min_item = min(amounts, key=lambda x: x[1])
        
        print(f"\n추가 통계:")
        print(f"  최대 순매수일: {max_item[0]} ({max_item[1]:,.0f}원)")
        print(f"  최소 순매수일: {min_item[0]} ({min_item[1]:,.0f}원)")
        print(f"  순매수 일수: {sum(1 for _, a in amounts if a > 0)}일")
        print(f"  순매도 일수: {sum(1 for _, a in amounts if a < 0)}일")
        print(f"\n분석 기간: {daily_data[0][0]} ~ {daily_data[-1][0]} ({count}거래일)")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    analyze_ai_infra_60days()
