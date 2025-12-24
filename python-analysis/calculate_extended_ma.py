"""
PRD-0012: 이동평균값 확장 저장 - 1회성 배치 스크립트
새로 추가된 16개 기간에 대한 이동평균을 계산하여 tb_stock_investor_ma에 저장
"""
import psycopg2
from psycopg2.extras import execute_batch
import pandas as pd
from datetime import datetime

# DB 설정
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'kiwoom',
    'user': 'kiwoom',
    'password': 'kiwoom123'
}

# 투자자 유형 (tb_stock_investor_chart의 컬럼명과 매핑)
INVESTORS = {
    'frgnr_invsr': 'frgnr_invsr',
    'orgn': 'orgn',
    'fnnc_invt': 'fnnc_invt',
    'insrnc': 'insrnc',
    'invtrt': 'invtrt',
    'etc_fnnc': 'etc_fnnc',
    'bank': 'bank',
    'penfnd_etc': 'penfnd_etc',
    'samo_fund': 'samo_fund',
    'natn': 'natn',
    'etc_corp': 'etc_corp',
    'natfor': 'natfor'
}

# 새로 추가된 이동평균 기간 (기존 5, 10, 20, 60은 제외)
MA_PERIODS = {
    '30': 30,
    '40': 40,
    '50': 50,
    '90': 90,
    '1m': 20,
    '2m': 40,
    '3m': 60,
    '4m': 80,
    '5m': 100,
    '6m': 120,
    '7m': 140,
    '8m': 160,
    '9m': 180,
    '10m': 200,
    '11m': 220,
    '12m': 240
}

def get_all_stocks(conn):
    """tb_stock_investor_ma에 있는 모든 종목코드 조회"""
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT stk_cd FROM tb_stock_investor_ma ORDER BY stk_cd")
    stocks = [row[0] for row in cursor.fetchall()]
    cursor.close()
    return stocks

def get_chart_data(conn, stk_cd):
    """특정 종목의 일별 수급 데이터 조회 (tb_stock_investor_chart)"""
    query = f"""
        SELECT dt, frgnr_invsr, orgn, fnnc_invt, insrnc, invtrt, etc_fnnc,
               bank, penfnd_etc, samo_fund, natn, etc_corp, natfor
        FROM tb_stock_investor_chart
        WHERE stk_cd = %s
        ORDER BY dt ASC
    """
    df = pd.read_sql(query, conn, params=(stk_cd,))
    return df

def calculate_ma_for_stock(conn, stk_cd, df_chart):
    """특정 종목에 대해 확장 이동평균 계산 및 업데이트"""
    if df_chart.empty:
        return 0
    
    cursor = conn.cursor()
    update_count = 0
    
    # tb_stock_investor_ma의 해당 종목의 모든 날짜 조회
    cursor.execute("SELECT dt FROM tb_stock_investor_ma WHERE stk_cd = %s ORDER BY dt ASC", (stk_cd,))
    ma_dates = [row[0] for row in cursor.fetchall()]
    
    if not ma_dates:
        return 0
    
    # chart 데이터를 날짜 인덱스로 변환
    df_chart['dt'] = pd.to_datetime(df_chart['dt'])
    df_chart.set_index('dt', inplace=True)
    
    for target_date in ma_dates:
        target_dt = pd.to_datetime(target_date)
        
        # 해당 날짜까지의 데이터만 사용
        df_until = df_chart[df_chart.index <= target_dt]
        
        if df_until.empty:
            continue
        
        update_cols = []
        update_vals = []
        
        for inv_key, inv_col in INVESTORS.items():
            for period_key, period_days in MA_PERIODS.items():
                col_name = f"{inv_key}_ma{period_key}"
                
                # 최근 N일 데이터 추출
                recent_data = df_until[inv_col].tail(period_days)
                
                if len(recent_data) >= period_days:
                    ma_value = recent_data.mean()
                    if pd.notna(ma_value):
                        update_cols.append(col_name)
                        update_vals.append(int(ma_value))
        
        if update_cols:
            set_clause = ", ".join([f"{col} = %s" for col in update_cols])
            update_vals.extend([stk_cd, target_date])
            
            update_sql = f"""
                UPDATE tb_stock_investor_ma 
                SET {set_clause}
                WHERE stk_cd = %s AND dt = %s
            """
            cursor.execute(update_sql, update_vals)
            update_count += 1
    
    conn.commit()
    cursor.close()
    return update_count

def main():
    print(f"[{datetime.now()}] PRD-0012 이동평균 확장 배치 시작")
    
    conn = psycopg2.connect(**DB_CONFIG)
    
    # 1. 모든 종목 조회
    stocks = get_all_stocks(conn)
    total = len(stocks)
    print(f"처리 대상 종목 수: {total}")
    
    # 2. 종목별 처리
    for idx, stk_cd in enumerate(stocks, 1):
        try:
            # 수급 데이터 조회
            df_chart = get_chart_data(conn, stk_cd)
            
            if df_chart.empty:
                print(f"[{idx}/{total}] {stk_cd}: 차트 데이터 없음 (Skip)")
                continue
            
            # 이동평균 계산 및 저장
            updated = calculate_ma_for_stock(conn, stk_cd, df_chart)
            print(f"[{idx}/{total}] {stk_cd}: {updated}개 날짜 업데이트 완료")
            
        except Exception as e:
            print(f"[{idx}/{total}] {stk_cd}: 오류 발생 - {e}")
            conn.rollback()
    
    conn.close()
    print(f"[{datetime.now()}] PRD-0012 이동평균 확장 배치 완료")

if __name__ == "__main__":
    main()
