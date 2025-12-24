"""
AI Infra 섹터 종목 10일 분석 스크립트
- tb_stock_list_meta.detail = 'ai_infra' AND tb_stock_list_meta.main = 'SECTOR' 조건으로 종목 추출
- 해당 종목들의 20251210 포함 10거래일 데이터 조회
- 날짜별 sum(cur_prc * frgnr_invsr) 계산
- 20251210의 10일 이동평균값 계산
"""

import psycopg2
from decimal import Decimal

def analyze_ai_infra_10days():
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
        print("=" * 100)
        print("1. AI Infra 섹터 종목 추출")
        print("   조건: detail = 'ai_infra' AND main = 'SECTOR'")
        print("=" * 100)
        
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
        print("-" * 100)
        print(f"{'코드':<10} {'종목명':<20} {'main':<10} {'sub':<10} {'detail':<10} {'sector':<20}")
        print("-" * 100)
        
        stock_codes = []
        for stock in stocks:
            code, name, main, sub, detail, sector = stock
            stock_codes.append(code)
            print(f"{code:<10} {name:<20} {main:<10} {sub or '':<10} {detail or '':<10} {sector or '':<20}")
        
        # 종목 코드 리스트를 SQL IN 절에 맞게 변환
        stock_codes_str = ", ".join([f"'{code}'" for code in stock_codes])
        
        # 2. 20251210 기준 10거래일 데이터 조회
        print("\n" + "=" * 100)
        print("2. 10거래일 날짜별 외국인 투자자 금액 합계")
        print("   계산식: sum(cur_prc * frgnr_invsr)")
        print("   기간: 20251210 포함 최근 10거래일")
        print("=" * 100)
        
        # 20251210 기준으로 10거래일 데이터 조회
        query_10days = f"""
            SELECT dt, 
                   SUM(cur_prc * frgnr_invsr) as total_amount
            FROM tb_stock_investor_chart
            WHERE stk_cd IN ({stock_codes_str})
              AND dt <= '20251210'
            GROUP BY dt
            ORDER BY dt DESC
            LIMIT 10
        """
        
        cursor.execute(query_10days)
        daily_data = cursor.fetchall()
        
        # 날짜순 정렬 (오래된 날짜 먼저)
        daily_data_sorted = sorted(daily_data, key=lambda x: x[0])
        
        print(f"\n조회된 거래일 수: {len(daily_data_sorted)}일")
        print("-" * 70)
        print(f"{'#':<5} {'날짜':<15} {'합계 (cur_prc * frgnr_invsr)':<35}")
        print("-" * 70)
        
        total_for_ma = Decimal('0')
        count = 0
        
        for idx, row in enumerate(daily_data_sorted, 1):
            dt, total_amount = row
            dt_str = str(dt)
            if total_amount:
                total_for_ma += Decimal(str(total_amount))
                count += 1
            formatted_amount = f"{total_amount:,.0f}" if total_amount else "0"
            print(f"{idx:<5} {dt_str:<15} {formatted_amount:>35}")
        
        print("-" * 70)
        
        # 3. 10일 이동평균 계산
        print("\n" + "=" * 100)
        print("3. 20251210 기준 10일 이동평균 (MA10) 계산")
        print("=" * 100)
        
        if count > 0:
            ma_10 = total_for_ma / count
            print(f"\n거래일 수: {count}일")
            print(f"합계: {total_for_ma:,.0f}원")
            print("-" * 70)
            print(f"\n★ 10일 이동평균 (MA10) = {total_for_ma:,.0f} / {count}")
            print(f"                       = {ma_10:,.0f}원")
            
            # 추가 통계
            amounts = [Decimal(str(row[1])) for row in daily_data_sorted if row[1]]
            max_amount = max(amounts)
            min_amount = min(amounts)
            
            # 최대값/최소값 날짜 찾기
            max_date = [str(row[0]) for row in daily_data_sorted if row[1] and Decimal(str(row[1])) == max_amount][0]
            min_date = [str(row[0]) for row in daily_data_sorted if row[1] and Decimal(str(row[1])) == min_amount][0]
            
            print("\n" + "-" * 70)
            print("추가 통계:")
            print(f"  최대 순매수일: {max_date} ({max_amount:,.0f}원)")
            print(f"  최소 순매수일: {min_date} ({min_amount:,.0f}원)")
            
            # 양수/음수 일수 계산
            positive_days = sum(1 for a in amounts if a > 0)
            negative_days = sum(1 for a in amounts if a < 0)
            print(f"  순매수 일수: {positive_days}일")
            print(f"  순매도 일수: {negative_days}일")
        else:
            print("\n이동평균 계산을 위한 데이터가 없습니다.")
        
        # 4. 기간 정보 출력
        if daily_data_sorted:
            start_date = str(daily_data_sorted[0][0])
            end_date = str(daily_data_sorted[-1][0])
            print("\n" + "=" * 100)
            print(f"분석 기간: {start_date} ~ {end_date} ({count}거래일)")
            print("=" * 100)
        
        cursor.close()
        conn.close()
        
        print("\n분석 완료")
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    analyze_ai_infra_10days()
