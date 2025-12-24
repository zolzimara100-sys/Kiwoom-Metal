"""
PRD-0012: 이동평균값 확장 저장 - 최적화된 배치 스크립트
배치 UPDATE로 성능 개선
"""
import psycopg2
from psycopg2.extras import execute_values
import pandas as pd
import numpy as np
from datetime import datetime

# DB 설정
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'kiwoom',
    'user': 'kiwoom',
    'password': 'kiwoom123'
}

# 투자자 유형
INVESTORS = ['frgnr_invsr', 'orgn', 'fnnc_invt', 'insrnc', 'invtrt', 'etc_fnnc',
             'bank', 'penfnd_etc', 'samo_fund', 'natn', 'etc_corp', 'natfor']

# 새로 추가된 이동평균 기간
MA_PERIODS = {
    '30': 30, '40': 40, '50': 50, '90': 90,
    '1m': 20, '2m': 40, '3m': 60, '4m': 80, '5m': 100,
    '6m': 120, '7m': 140, '8m': 160, '9m': 180,
    '10m': 200, '11m': 220, '12m': 240
}

def get_all_stocks(conn):
    """처리 대상 종목 조회"""
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT stk_cd FROM tb_stock_investor_ma ORDER BY stk_cd")
    stocks = [row[0] for row in cursor.fetchall()]
    cursor.close()
    return stocks

def process_stock(conn, stk_cd):
    """종목 단위로 모든 날짜의 확장 MA 계산 및 일괄 업데이트"""
    cursor = conn.cursor()
    
    # 1. 해당 종목의 원본 차트 데이터 조회
    cursor.execute("""
        SELECT dt, frgnr_invsr, orgn, fnnc_invt, insrnc, invtrt, etc_fnnc,
               bank, penfnd_etc, samo_fund, natn, etc_corp, natfor
        FROM tb_stock_investor_chart
        WHERE stk_cd = %s
        ORDER BY dt ASC
    """, (stk_cd,))
    chart_rows = cursor.fetchall()
    
    if not chart_rows:
        return 0
    
    # 2. 데이터프레임으로 변환
    cols = ['dt'] + INVESTORS
    df = pd.DataFrame(chart_rows, columns=cols)
    df.set_index('dt', inplace=True)
    
    # 3. 해당 종목의 MA 테이블 날짜 목록 조회
    cursor.execute("SELECT dt FROM tb_stock_investor_ma WHERE stk_cd = %s ORDER BY dt ASC", (stk_cd,))
    ma_dates = [row[0] for row in cursor.fetchall()]
    
    if not ma_dates:
        return 0
    
    # 4. 각 기간별 이동평균 미리 계산 (rolling)
    ma_data = {}
    for inv in INVESTORS:
        for period_key, period_days in MA_PERIODS.items():
            col_name = f"{inv}_ma{period_key}"
            # rolling mean 계산
            ma_data[col_name] = df[inv].rolling(window=period_days, min_periods=period_days).mean()
    
    ma_df = pd.DataFrame(ma_data, index=df.index)
    
    # 5. 업데이트 SQL 생성 (한 종목의 모든 날짜를 한번에)
    # 동적으로 컬럼 목록 생성
    col_names = [f"{inv}_ma{period_key}" for inv in INVESTORS for period_key in MA_PERIODS.keys()]
    
    update_count = 0
    batch_size = 500
    updates = []
    
    for target_date in ma_dates:
        if target_date not in ma_df.index:
            continue
        
        row = ma_df.loc[target_date]
        values = []
        has_data = False
        
        for col in col_names:
            val = row.get(col, None)
            if pd.notna(val):
                values.append(int(val))
                has_data = True
            else:
                values.append(None)
        
        if has_data:
            updates.append((stk_cd, target_date, *values))
    
    if not updates:
        cursor.close()
        return 0
    
    # 6. 배치 UPDATE 실행
    set_clause = ", ".join([f"{col} = data.{col}" for col in col_names])
    col_list = ", ".join(col_names)
    
    # TEMP 테이블 방식으로 대량 업데이트
    temp_cols = "stk_cd VARCHAR, dt DATE, " + ", ".join([f"{col} NUMERIC(15)" for col in col_names])
    
    cursor.execute(f"CREATE TEMP TABLE temp_ma_update ({temp_cols}) ON COMMIT DROP")
    
    # execute_values로 TEMP 테이블에 데이터 삽입
    insert_cols = "stk_cd, dt, " + col_list
    insert_sql = f"INSERT INTO temp_ma_update ({insert_cols}) VALUES %s"
    
    execute_values(cursor, insert_sql, updates, page_size=1000)
    
    # TEMP 테이블에서 원본 테이블로 UPDATE
    update_sql = f"""
        UPDATE tb_stock_investor_ma AS m
        SET {set_clause}
        FROM temp_ma_update AS data
        WHERE m.stk_cd = data.stk_cd AND m.dt = data.dt
    """
    cursor.execute(update_sql)
    update_count = cursor.rowcount
    
    conn.commit()
    cursor.close()
    
    return update_count

def main():
    print(f"[{datetime.now()}] PRD-0012 이동평균 확장 배치 시작 (최적화 버전)")
    
    conn = psycopg2.connect(**DB_CONFIG)
    
    stocks = get_all_stocks(conn)
    total = len(stocks)
    print(f"처리 대상 종목 수: {total}")
    
    total_updated = 0
    for idx, stk_cd in enumerate(stocks, 1):
        try:
            updated = process_stock(conn, stk_cd)
            total_updated += updated
            print(f"[{idx}/{total}] {stk_cd}: {updated}개 날짜 업데이트")
        except Exception as e:
            print(f"[{idx}/{total}] {stk_cd}: 오류 - {e}")
            conn.rollback()
    
    conn.close()
    print(f"[{datetime.now()}] 배치 완료. 총 {total_updated}개 레코드 업데이트")

if __name__ == "__main__":
    main()
