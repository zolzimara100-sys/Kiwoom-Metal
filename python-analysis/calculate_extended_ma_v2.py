"""
PRD-0012: 이동평균값 확장 저장 - 최적화된 배치 스크립트 (날짜 형식 수정)
"""
import psycopg2
from psycopg2.extras import execute_values
import pandas as pd
import numpy as np
from datetime import datetime

DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'kiwoom',
    'user': 'kiwoom',
    'password': 'kiwoom123'
}

INVESTORS = ['frgnr_invsr', 'orgn', 'fnnc_invt', 'insrnc', 'invtrt', 'etc_fnnc',
             'bank', 'penfnd_etc', 'samo_fund', 'natn', 'etc_corp', 'natfor']

MA_PERIODS = {
    '30': 30, '40': 40, '50': 50, '90': 90,
    '1m': 20, '2m': 40, '3m': 60, '4m': 80, '5m': 100,
    '6m': 120, '7m': 140, '8m': 160, '9m': 180,
    '10m': 200, '11m': 220, '12m': 240
}

def get_all_stocks(conn):
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT stk_cd FROM tb_stock_investor_ma ORDER BY stk_cd")
    stocks = [row[0] for row in cursor.fetchall()]
    cursor.close()
    return stocks

def process_stock(conn, stk_cd):
    cursor = conn.cursor()
    
    # Chart 데이터 조회 (datetime.date)
    cursor.execute("""
        SELECT dt, frgnr_invsr, orgn, fnnc_invt, insrnc, invtrt, etc_fnnc,
               bank, penfnd_etc, samo_fund, natn, etc_corp, natfor
        FROM tb_stock_investor_chart
        WHERE stk_cd = %s
        ORDER BY dt ASC
    """, (stk_cd,))
    chart_rows = cursor.fetchall()
    
    if not chart_rows:
        cursor.close()
        return 0
    
    # DataFrame 생성
    cols = ['dt'] + INVESTORS
    df = pd.DataFrame(chart_rows, columns=cols)
    
    # dt를 문자열 YYYYMMDD로 변환 (MA 테이블과 매칭용)
    df['dt_str'] = df['dt'].apply(lambda x: x.strftime('%Y%m%d'))
    df.set_index('dt_str', inplace=True)
    
    # MA 테이블 날짜 목록 조회 (문자열)
    cursor.execute("SELECT dt FROM tb_stock_investor_ma WHERE stk_cd = %s ORDER BY dt ASC", (stk_cd,))
    ma_dates = [row[0] for row in cursor.fetchall()]
    
    if not ma_dates:
        cursor.close()
        return 0
    
    # 이동평균 계산 (rolling)
    ma_data = {}
    for inv in INVESTORS:
        for period_key, period_days in MA_PERIODS.items():
            col_name = f"{inv}_ma{period_key}"
            ma_data[col_name] = df[inv].rolling(window=period_days, min_periods=period_days).mean()
    
    ma_df = pd.DataFrame(ma_data, index=df.index)
    
    col_names = [f"{inv}_ma{period_key}" for inv in INVESTORS for period_key in MA_PERIODS.keys()]
    
    updates = []
    
    for target_date_str in ma_dates:
        if target_date_str not in ma_df.index:
            continue
        
        row = ma_df.loc[target_date_str]
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
            updates.append((stk_cd, target_date_str, *values))
    
    if not updates:
        cursor.close()
        return 0
    
    # TEMP 테이블 방식 업데이트
    temp_cols = "stk_cd VARCHAR, dt VARCHAR, " + ", ".join([f"{col} NUMERIC(15)" for col in col_names])
    cursor.execute(f"CREATE TEMP TABLE temp_ma_update ({temp_cols}) ON COMMIT DROP")
    
    insert_cols = "stk_cd, dt, " + ", ".join(col_names)
    insert_sql = f"INSERT INTO temp_ma_update ({insert_cols}) VALUES %s"
    execute_values(cursor, insert_sql, updates, page_size=1000)
    
    set_clause = ", ".join([f"{col} = data.{col}" for col in col_names])
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
    print(f"[{datetime.now()}] PRD-0012 이동평균 확장 배치 시작")
    
    conn = psycopg2.connect(**DB_CONFIG)
    
    stocks = get_all_stocks(conn)
    total = len(stocks)
    print(f"처리 대상 종목 수: {total}")
    
    total_updated = 0
    for idx, stk_cd in enumerate(stocks, 1):
        try:
            updated = process_stock(conn, stk_cd)
            total_updated += updated
            if updated > 0:
                print(f"[{idx}/{total}] {stk_cd}: {updated}개 날짜 업데이트")
            else:
                print(f"[{idx}/{total}] {stk_cd}: 매칭 데이터 없음")
        except Exception as e:
            print(f"[{idx}/{total}] {stk_cd}: 오류 - {e}")
            conn.rollback()
    
    conn.close()
    print(f"[{datetime.now()}] 배치 완료. 총 {total_updated}개 레코드 업데이트")

if __name__ == "__main__":
    main()
