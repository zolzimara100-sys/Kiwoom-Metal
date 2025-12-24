"""
AI Infra 섹터 120일 분석 - 외국인+기관 합산 (frgnr_orgn_ma120)
테이블: tb_stock_investor_sector_ma
"""

import psycopg2
from decimal import Decimal

def analyze():
    db_config = {'host': 'localhost', 'port': 5432, 'database': 'kiwoom', 'user': 'kiwoom', 'password': 'kiwoom123'}
    conn = psycopg2.connect(**db_config)
    cursor = conn.cursor()

    print('=' * 100)
    print('AI Infra 섹터 120일 분석 - 외국인+기관 합산 (frgnr_orgn_ma120)')
    print('테이블: tb_stock_investor_sector_ma')
    print('=' * 100)

    # ai_infra 섹터의 데이터 조회 (최근 120일)
    cursor.execute("""
        SELECT dt, sector_nm, frgnr_orgn_ma120
        FROM tb_stock_investor_sector_ma
        WHERE sector_cd = 'ai_infra' AND dt <= '20251210'
        ORDER BY dt DESC
        LIMIT 120
    """)
    data = cursor.fetchall()

    if not data:
        print('ai_infra 섹터 데이터가 없습니다. 사용 가능한 섹터 확인:')
        cursor.execute("SELECT DISTINCT sector_cd, sector_nm FROM tb_stock_investor_sector_ma ORDER BY sector_cd")
        for cd, nm in cursor.fetchall():
            print(f'  {cd}: {nm}')
    else:
        data_sorted = sorted(data, key=lambda x: x[0])
        
        print(f'\n조회된 거래일 수: {len(data_sorted)}일')
        print('-' * 70)
        
        total = Decimal('0')
        for idx, (dt, nm, val) in enumerate(data_sorted, 1):
            if val:
                total += val
            if idx <= 5 or idx > len(data_sorted) - 5:
                val_str = f'{val:,.0f}' if val else '0'
                print(f'{idx:<5} {str(dt):<15} {nm:<20} {val_str:>25}')
            elif idx == 6:
                print(f"{'...':<5} {'...':<15} {'...':<20} {'... (중간 생략) ...':>25}")
        
        count = len(data_sorted)
        ma = total / count if count > 0 else 0
        
        print('-' * 70)
        print(f'\n★ 120일간 frgnr_orgn_ma120 평균 = {total:,.0f} / {count}')
        print(f'                                 = {ma:,.0f}원')
        
        # 20251210 단일 값
        latest = [d for d in data_sorted if str(d[0]) == '2025-12-10']
        if latest:
            print(f'\n★ 20251210의 frgnr_orgn_ma120 값 = {latest[0][2]:,.0f}원')

    cursor.close()
    conn.close()

if __name__ == "__main__":
    analyze()
