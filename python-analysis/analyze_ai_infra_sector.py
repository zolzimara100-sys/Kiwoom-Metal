"""
AI Infra 섹터 종목 분석 스크립트
- tb_stock_list_meta.detail = 'ai_infra' AND tb_stock_list_meta.main = 'SECTOR' 조건으로 종목 추출
- 해당 종목들의 20251204 ~ 20251210 날짜별 sum(cur_prc * frgnr_invsr) 계산
- 20251210의 5일 이동평균값 계산
"""

import psycopg2
from decimal import Decimal

def analyze_ai_infra_sector():
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
        
        # 1. AI Infra 섹터 종목 추출
        print("=" * 80)
        print("1. AI Infra 섹터 종목 추출")
        print("   조건: detail = 'ai_infra' AND main = 'SECTOR'")
        print("=" * 80)
        
        cursor.execute("""
            SELECT code, name, main, sub, detail, sector
            FROM tb_stock_list_meta
            WHERE detail = 'ai_infra' AND main = 'SECTOR'
            ORDER BY code
        """)
        stocks = cursor.fetchall()
        
        if not stocks:
            print("조건에 맞는 종목이 없습니다.")
            return
        
        print(f"\n총 {len(stocks)}개 종목 추출:")
        print("-" * 80)
        print(f"{'코드':<10} {'종목명':<20} {'main':<10} {'sub':<10} {'detail':<10} {'sector':<15}")
        print("-" * 80)
        
        stock_codes = []
        for stock in stocks:
            code, name, main, sub, detail, sector = stock
            stock_codes.append(code)
            print(f"{code:<10} {name:<20} {main:<10} {sub or '':<10} {detail or '':<10} {sector or '':<15}")
        
        # 2. 날짜별 sum(cur_prc * frgnr_invsr) 계산
        print("\n" + "=" * 80)
        print("2. 날짜별 외국인 투자자 금액 합계")
        print("   계산식: sum(cur_prc * frgnr_invsr)")
        print("   기간: 20251204 ~ 20251210")
        print("=" * 80)
        
        # 종목 코드 리스트를 SQL IN 절에 맞게 변환
        stock_codes_str = ", ".join([f"'{code}'" for code in stock_codes])
        
        query = f"""
            SELECT dt, 
                   SUM(cur_prc * frgnr_invsr) as total_amount
            FROM tb_stock_investor_chart
            WHERE stk_cd IN ({stock_codes_str})
              AND dt BETWEEN '20251204' AND '20251210'
            GROUP BY dt
            ORDER BY dt
        """
        
        cursor.execute(query)
        daily_sums = cursor.fetchall()
        
        print(f"\n{'날짜':<15} {'합계 (cur_prc * frgnr_invsr)':<30}")
        print("-" * 50)
        
        daily_data = {}
        for row in daily_sums:
            dt, total_amount = row
            dt_str = str(dt)
            daily_data[dt_str] = total_amount
            # 금액 포맷팅 (천 단위 콤마)
            formatted_amount = f"{total_amount:,.0f}" if total_amount else "0"
            print(f"{dt_str:<15} {formatted_amount:>30}")
        
        # 3. 5일 이동평균 계산을 위해 더 많은 날짜 데이터 조회
        print("\n" + "=" * 80)
        print("3. 20251210의 5일 이동평균 계산")
        print("   5일: 20251210, 20251209, 20251208, 20251207, 20251206 (또는 해당 기간의 거래일)")
        print("=" * 80)
        
        # 20251210 기준 최근 5거래일 데이터 조회
        query_ma = f"""
            SELECT dt, 
                   SUM(cur_prc * frgnr_invsr) as total_amount
            FROM tb_stock_investor_chart
            WHERE stk_cd IN ({stock_codes_str})
              AND dt <= '20251210'
            GROUP BY dt
            ORDER BY dt DESC
            LIMIT 5
        """
        
        cursor.execute(query_ma)
        ma_data = cursor.fetchall()
        
        print("\n5일 이동평균 계산에 사용된 데이터:")
        print("-" * 50)
        print(f"{'날짜':<15} {'합계 (cur_prc * frgnr_invsr)':<30}")
        print("-" * 50)
        
        total_for_ma = Decimal('0')
        count = 0
        for row in ma_data:
            dt, total_amount = row
            dt_str = str(dt)
            if total_amount:
                total_for_ma += Decimal(str(total_amount))
                count += 1
            formatted_amount = f"{total_amount:,.0f}" if total_amount else "0"
            print(f"{dt_str:<15} {formatted_amount:>30}")
        
        if count > 0:
            ma_5 = total_for_ma / count
            print("-" * 50)
            print(f"\n5일 이동평균 (MA5) = 합계 / {count}일")
            print(f"                  = {total_for_ma:,.0f} / {count}")
            print(f"                  = {ma_5:,.0f}")
        else:
            print("\n이동평균 계산을 위한 데이터가 없습니다.")
        
        # 4. 종목별 상세 데이터 (추가 분석용)
        print("\n" + "=" * 80)
        print("4. 종목별 상세 데이터 (20251204 ~ 20251210)")
        print("=" * 80)
        
        query_detail = f"""
            SELECT stk_cd, dt, cur_prc, frgnr_invsr, 
                   (cur_prc * frgnr_invsr) as amount
            FROM tb_stock_investor_chart
            WHERE stk_cd IN ({stock_codes_str})
              AND dt BETWEEN '20251204' AND '20251210'
            ORDER BY stk_cd, dt
        """
        
        cursor.execute(query_detail)
        details = cursor.fetchall()
        
        print(f"\n{'종목코드':<10} {'날짜':<12} {'현재가':<15} {'외국인순매수':<18} {'금액(cur_prc*frgnr)':<20}")
        print("-" * 80)
        
        for row in details:
            stk_cd, dt, cur_prc, frgnr_invsr, amount = row
            dt_str = str(dt)
            cur_prc_fmt = f"{cur_prc:,.0f}" if cur_prc else "0"
            frgnr_fmt = f"{frgnr_invsr:,.0f}" if frgnr_invsr else "0"
            amount_fmt = f"{amount:,.0f}" if amount else "0"
            print(f"{stk_cd:<10} {dt_str:<12} {cur_prc_fmt:>15} {frgnr_fmt:>18} {amount_fmt:>20}")
        
        cursor.close()
        conn.close()
        
        print("\n" + "=" * 80)
        print("분석 완료")
        print("=" * 80)
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    analyze_ai_infra_sector()
