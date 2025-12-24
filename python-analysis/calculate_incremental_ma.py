#!/usr/bin/env python3
"""
투자자 이동평균 증분 업데이트 스크립트
Incremental Moving Average Calculation for Investor Data

일별 데이터 수집 후 실행하여 신규 데이터에 대해서만 이동평균을 계산합니다.
"""

import psycopg2
import time
import sys
import os
from datetime import datetime, timedelta

def get_stock_date_ranges(cursor, target_stock_code=None):
    """
    각 종목별 chart 테이블과 ma 테이블의 날짜 범위를 조회
    Returns: dict {stk_cd: {'chart_min': date, 'chart_max': date, 'ma_min': date, 'ma_max': date}}
    """
    query = """
    WITH chart_range AS (
        SELECT
            stk_cd,
            MIN(dt) as chart_min,
            MAX(dt) as chart_max
        FROM tb_stock_investor_chart
        {where_clause}
        GROUP BY stk_cd
    ),
    ma_range AS (
        SELECT
            stk_cd,
            MIN(TO_DATE(dt, 'YYYYMMDD')) as ma_min,
            MAX(TO_DATE(dt, 'YYYYMMDD')) as ma_max
        FROM tb_stock_investor_ma
        GROUP BY stk_cd
    )
    SELECT
        c.stk_cd,
        c.chart_min,
        c.chart_max,
        m.ma_min,
        m.ma_max
    FROM chart_range c
    LEFT JOIN ma_range m ON c.stk_cd = m.stk_cd
    ORDER BY c.stk_cd
    """
    
    where_clause = f"WHERE stk_cd = '{target_stock_code}'" if target_stock_code else ""
    query = query.format(where_clause=where_clause)

    cursor.execute(query)
    results = {}

    for row in cursor.fetchall():
        stk_cd, chart_min, chart_max, ma_min, ma_max = row
        results[stk_cd] = {
            'chart_min': chart_min,
            'chart_max': chart_max,
            'ma_min': ma_min,
            'ma_max': ma_max,
            'is_new': ma_min is None  # MA 테이블에 데이터가 없으면 신규 종목
        }

    return results

def generate_incremental_ma_query(stk_cd, start_date, end_date):
    """
    특정 종목의 특정 날짜 범위에 대한 이동평균 계산 쿼리 생성

    Args:
        stk_cd: 종목코드
        start_date: 계산 시작일 (DATE 타입)
        end_date: 계산 종료일 (DATE 타입)
    """

    # 투자자 유형 매핑 (source_column, target_prefix)
    investors = [
        ('frgnr_invsr', 'frgnr_invsr'),
        ('orgn', 'orgn'),
        ('ind_invsr', 'ind_invsr'),  # 개인 추가
        ('fnnc_invt', 'fnnc_invt'),
        ('insrnc', 'insrnc'),
        ('invtrt', 'invtrt'),
        ('etc_fnnc', 'etc_fnnc'),
        ('bank', 'bank'),
        ('penfnd_etc', 'penfnd_etc'),
        ('samo_fund', 'samo_fund'),
        ('natn', 'natn'),
        ('etc_corp', 'etc_corp'),
        ('natfor', 'natfor')
    ]
    
    # 이동평균 기간 설정 (접미사, 윈도우 크기)
    # 기존: 5, 10, 20, 60
    # 신규: 30, 40, 50, 90, 120, 140
    PERIODS_CONFIG = [
        ('5', 5), ('10', 10), ('20', 20), ('30', 30), ('40', 40), 
        ('50', 50), ('60', 60), ('90', 90), ('120', 120), ('140', 140)
    ]

    # MA 계산 SELECT 절 생성
    ma_selects = []
    ma_columns = []

    for src_col, target_prefix in investors:
        for suffix, window_size in PERIODS_CONFIG:
            col_name = f'{target_prefix}_ma{suffix}'
            
            # 충분한 데이터가 있을 때만 계산 (NULL 방지)
            col_expr = f"""
        CASE
            WHEN row_idx >= {window_size} THEN ROUND(AVG({src_col}) OVER (PARTITION BY stk_cd ORDER BY dt ROWS BETWEEN {window_size-1} PRECEDING AND CURRENT ROW), 2)
            ELSE NULL
        END AS {col_name}"""
            ma_selects.append(col_expr)
            ma_columns.append(col_name)

    ma_selects_str = ",".join(ma_selects)

    query = f"""
    WITH base_data AS (
        SELECT
            c.stk_cd,
            c.dt,
            TO_CHAR(c.dt, 'YYYYMMDD') as dt_str,
            m.sector,
            m.main as category1,
            m.sub as category2,
            m.detail as category3,
            -- Row number for each stock (전체 시계열 유지)
            ROW_NUMBER() OVER (PARTITION BY c.stk_cd ORDER BY c.dt) as row_idx,
            -- Original investor columns
            c.ind_invsr, c.frgnr_invsr, c.orgn, c.fnnc_invt, c.insrnc, c.invtrt, c.etc_fnnc,
            c.bank, c.penfnd_etc, c.samo_fund, c.natn, c.etc_corp, c.natfor,
            c.cur_prc
        FROM tb_stock_investor_chart c
        LEFT JOIN tb_stock_list_meta m ON c.stk_cd = m.code
        WHERE c.stk_cd = %s
    ),
    ma_calculated AS (
        SELECT
            stk_cd,
            dt,
            dt_str,
            sector, category1, category2, category3, cur_prc,
            {ma_selects_str}
        FROM base_data
    )
    INSERT INTO tb_stock_investor_ma (
        stk_cd, dt, sector, category1, category2, category3, cur_prc,
        {', '.join(ma_columns)}
    )
    SELECT
        stk_cd,
        dt_str,
        sector, category1, category2, category3, cur_prc,
        {', '.join(ma_columns)}
    FROM ma_calculated
    WHERE dt >= %s AND dt <= %s
    ON CONFLICT (stk_cd, dt)
    DO UPDATE SET
        sector = EXCLUDED.sector,
        category1 = EXCLUDED.category1,
        category2 = EXCLUDED.category2,
        category3 = EXCLUDED.category3,
        cur_prc = EXCLUDED.cur_prc,
        {', '.join([f'{col} = EXCLUDED.{col}' for col in ma_columns])}
    """

    return query

def calculate_incremental_ma(target_stock_code=None, force_full_update=False):
    """
    증분 업데이트 방식으로 이동평균 계산
    force_full_update: True일 경우 기존 데이터 유무와 상관없이 전체 기간 재계산
    """
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', '5432')),
        'database': os.getenv('DB_NAME', 'kiwoom'),
        'user': os.getenv('DB_USER', 'kiwoom'),
        'password': os.getenv('DB_PASSWORD', 'kiwoom123')
    }

    try:
        conn = psycopg2.connect(**db_config)
        cursor = conn.cursor()

        start_time = time.time()
        print("=" * 80)
        mode_str = "전체 재계산 (Full Update)" if force_full_update else "증분 업데이트 (Incremental)"
        print(f"투자자 이동평균 계산 시작 - 모드: {mode_str}")
        print("=" * 80)

        # 0. 스키마 확인 (cur_prc 컬럼 추가)
        print("\n[Step 0] 스키마 확인 중...")
        cursor.execute("ALTER TABLE tb_stock_investor_ma ADD COLUMN IF NOT EXISTS cur_prc BIGINT")
        conn.commit()
        print("  ✓ 스키마 확인 완료")

        # 1. 종목별 날짜 범위 조회
        print("\n[Step 1] 종목별 날짜 범위 조회 중...")
        stock_ranges = get_stock_date_ranges(cursor, target_stock_code)
        print(f"  ✓ 총 {len(stock_ranges)}개 종목 확인")

        # 2. 종목별 처리
        print("\n[Step 2] 종목별 이동평균 계산 중...")

        new_stocks = []
        updated_stocks = []
        skipped_stocks = []

        total_inserted = 0
        total_updated = 0

        for idx, (stk_cd, ranges) in enumerate(stock_ranges.items(), 1):
            chart_min = ranges['chart_min']
            chart_max = ranges['chart_max']
            ma_min = ranges['ma_min']
            ma_max = ranges['ma_max']
            is_new = ranges['is_new']

            # 진행률 표시 (매 10개마다)
            if idx % 10 == 0:
                print(f"  처리 중... {idx}/{len(stock_ranges)} ({(idx/len(stock_ranges)*100):.1f}%)")

            if force_full_update or is_new:
                # 전체 재계산 또는 신규 종목
                start_date = chart_min
                end_date = chart_max
                if is_new:
                    new_stocks.append(stk_cd)
                    action = "INSERT"
                else:
                    updated_stocks.append(stk_cd) # 실제로는 전체 덮어쓰기
                    action = "FULL_UPDATE"
            else:
                # 증분 업데이트: ma_max 다음날부터 chart_max까지 계산
                if ma_max >= chart_max:
                    # 이미 최신 데이터까지 계산됨
                    skipped_stocks.append(stk_cd)
                    continue

                start_date = ma_max + timedelta(days=1)
                end_date = chart_max
                updated_stocks.append(stk_cd)
                action = "UPDATE"

            # 이동평균 계산 쿼리 실행
            query = generate_incremental_ma_query(stk_cd, start_date, end_date)
            cursor.execute(query, (stk_cd, start_date, end_date))
            affected_rows = cursor.rowcount

            if action == "INSERT":
                total_inserted += affected_rows
            else:
                total_updated += affected_rows

        conn.commit()

        # 3. 결과 요약
        elapsed = time.time() - start_time

        print("\n" + "=" * 80)
        print("처리 완료!")
        print("=" * 80)
        print(f"\n📊 처리 결과:")
        print(f"  • 신규/전체재계산 종목: {len(new_stocks)}개 (INSERT)")
        print(f"  • 업데이트/재계산 종목: {len(updated_stocks)}개 (UPSERT)")
        print(f"  • 스킵 종목: {len(skipped_stocks)}개")
        print(f"  • 총 영향 받은 행: {total_inserted + total_updated:,}행")
        print(f"\n⏱️  총 소요 시간: {elapsed:.2f}초")

        cursor.close()
        conn.close()

        print("\n✅ 프로그램 정상 종료")

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    target_stk = None
    force_full = False
    
    # 간단한 인자 파싱
    args = sys.argv[1:]
    if "--full" in args:
        force_full = True
        args.remove("--full")
    
    if len(args) > 0:
        target_stk = args[0]
        print(f"Args: Target Stock = {target_stk}")
        
    calculate_incremental_ma(target_stock_code=target_stk, force_full_update=force_full)
