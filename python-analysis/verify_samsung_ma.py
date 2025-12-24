#!/usr/bin/env python3
"""
삼성전자 이동평균 검증 스크립트
Verify Moving Averages for Samsung Electronics (005930)
"""

import psycopg2
from tabulate import tabulate

def verify_samsung_ma():
    """
    삼성전자(005930)의 이동평균이 올바르게 계산되었는지 검증
    """
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
        print("삼성전자(005930) 이동평균 검증")
        print("=" * 100)

        # 1. 전체 데이터 개수 확인
        print("\n[1] 데이터 개수 확인")
        cursor.execute("""
            SELECT COUNT(*) as chart_count
            FROM tb_stock_investor_chart
            WHERE stk_cd = '005930'
        """)
        chart_count = cursor.fetchone()[0]

        cursor.execute("""
            SELECT COUNT(*) as ma_count
            FROM tb_stock_investor_ma
            WHERE stk_cd = '005930'
        """)
        ma_count = cursor.fetchone()[0]

        print(f"  • Chart 테이블: {chart_count:,}건")
        print(f"  • MA 테이블: {ma_count:,}건")

        if chart_count == ma_count:
            print(f"  ✅ 데이터 개수 일치")
        else:
            print(f"  ⚠️  데이터 개수 불일치 (차이: {abs(chart_count - ma_count)}건)")

        # 2. 날짜 범위 확인
        print("\n[2] 날짜 범위 확인")
        cursor.execute("""
            SELECT
                MIN(dt) as min_dt,
                MAX(dt) as max_dt
            FROM tb_stock_investor_chart
            WHERE stk_cd = '005930'
        """)
        chart_min, chart_max = cursor.fetchone()

        cursor.execute("""
            SELECT
                MIN(dt) as min_dt,
                MAX(dt) as max_dt
            FROM tb_stock_investor_ma
            WHERE stk_cd = '005930'
        """)
        ma_min, ma_max = cursor.fetchone()

        print(f"  • Chart: {chart_min} ~ {chart_max}")
        print(f"  • MA:    {ma_min} ~ {ma_max}")

        if str(chart_min) == ma_min and str(chart_max) == ma_max:
            print(f"  ✅ 날짜 범위 일치")
        else:
            print(f"  ⚠️  날짜 범위 불일치")

        # 3. 최근 10일 이동평균 샘플 확인
        print("\n[3] 최근 10일 이동평균 샘플 (외국인)")
        cursor.execute("""
            SELECT
                dt,
                frgnr_invsr_ma5,
                frgnr_invsr_ma10,
                frgnr_invsr_ma20,
                frgnr_invsr_ma60
            FROM tb_stock_investor_ma
            WHERE stk_cd = '005930'
            ORDER BY dt DESC
            LIMIT 10
        """)

        headers = ["날짜", "MA5", "MA10", "MA20", "MA60"]
        rows = cursor.fetchall()
        print(tabulate(rows, headers=headers, floatfmt=".2f", tablefmt="grid"))

        # 4. NULL 값 확인 (초기 기간)
        print("\n[4] NULL 값 분포 확인")
        cursor.execute("""
            SELECT
                CASE
                    WHEN frgnr_invsr_ma5 IS NULL THEN 'MA5_NULL'
                    WHEN frgnr_invsr_ma10 IS NULL THEN 'MA10_NULL'
                    WHEN frgnr_invsr_ma20 IS NULL THEN 'MA20_NULL'
                    WHEN frgnr_invsr_ma60 IS NULL THEN 'MA60_NULL'
                    ELSE 'ALL_OK'
                END as null_status,
                COUNT(*) as cnt
            FROM tb_stock_investor_ma
            WHERE stk_cd = '005930'
            GROUP BY null_status
            ORDER BY null_status
        """)

        headers = ["상태", "건수"]
        rows = cursor.fetchall()
        print(tabulate(rows, headers=headers, tablefmt="grid"))

        # 5. 이동평균 수동 검증 (최근 1일)
        print("\n[5] 이동평균 수동 검증 (최근 1일 - 외국인)")
        cursor.execute("""
            WITH recent_data AS (
                SELECT
                    dt,
                    frgnr_invsr,
                    ROW_NUMBER() OVER (ORDER BY dt DESC) as rn
                FROM tb_stock_investor_chart
                WHERE stk_cd = '005930'
                ORDER BY dt DESC
                LIMIT 60
            ),
            manual_calc AS (
                SELECT
                    (SELECT dt FROM recent_data WHERE rn = 1) as latest_dt,
                    ROUND(AVG(frgnr_invsr), 2) as manual_ma5,
                    (SELECT frgnr_invsr_ma5 FROM tb_stock_investor_ma
                     WHERE stk_cd = '005930'
                     ORDER BY dt DESC LIMIT 1) as stored_ma5
                FROM recent_data
                WHERE rn <= 5
            ),
            manual_calc10 AS (
                SELECT
                    ROUND(AVG(frgnr_invsr), 2) as manual_ma10,
                    (SELECT frgnr_invsr_ma10 FROM tb_stock_investor_ma
                     WHERE stk_cd = '005930'
                     ORDER BY dt DESC LIMIT 1) as stored_ma10
                FROM recent_data
                WHERE rn <= 10
            ),
            manual_calc20 AS (
                SELECT
                    ROUND(AVG(frgnr_invsr), 2) as manual_ma20,
                    (SELECT frgnr_invsr_ma20 FROM tb_stock_investor_ma
                     WHERE stk_cd = '005930'
                     ORDER BY dt DESC LIMIT 1) as stored_ma20
                FROM recent_data
                WHERE rn <= 20
            ),
            manual_calc60 AS (
                SELECT
                    ROUND(AVG(frgnr_invsr), 2) as manual_ma60,
                    (SELECT frgnr_invsr_ma60 FROM tb_stock_investor_ma
                     WHERE stk_cd = '005930'
                     ORDER BY dt DESC LIMIT 1) as stored_ma60
                FROM recent_data
                WHERE rn <= 60
            )
            SELECT
                c.latest_dt as 날짜,
                c.manual_ma5 as 수동계산_MA5,
                c.stored_ma5 as 저장된_MA5,
                CASE WHEN c.manual_ma5 = c.stored_ma5 THEN '✅' ELSE '❌' END as MA5검증,
                c10.manual_ma10 as 수동계산_MA10,
                c10.stored_ma10 as 저장된_MA10,
                CASE WHEN c10.manual_ma10 = c10.stored_ma10 THEN '✅' ELSE '❌' END as MA10검증,
                c20.manual_ma20 as 수동계산_MA20,
                c20.stored_ma20 as 저장된_MA20,
                CASE WHEN c20.manual_ma20 = c20.stored_ma20 THEN '✅' ELSE '❌' END as MA20검증,
                c60.manual_ma60 as 수동계산_MA60,
                c60.stored_ma60 as 저장된_MA60,
                CASE WHEN c60.manual_ma60 = c60.stored_ma60 THEN '✅' ELSE '❌' END as MA60검증
            FROM manual_calc c
            CROSS JOIN manual_calc10 c10
            CROSS JOIN manual_calc20 c20
            CROSS JOIN manual_calc60 c60
        """)

        row = cursor.fetchone()
        if row:
            print(f"\n  날짜: {row[0]}")
            print(f"\n  MA5:")
            print(f"    수동 계산: {row[1]}")
            print(f"    저장된 값: {row[2]}")
            print(f"    검증 결과: {row[3]}")

            print(f"\n  MA10:")
            print(f"    수동 계산: {row[4]}")
            print(f"    저장된 값: {row[5]}")
            print(f"    검증 결과: {row[6]}")

            print(f"\n  MA20:")
            print(f"    수동 계산: {row[7]}")
            print(f"    저장된 값: {row[8]}")
            print(f"    검증 결과: {row[9]}")

            print(f"\n  MA60:")
            print(f"    수동 계산: {row[10]}")
            print(f"    저장된 값: {row[11]}")
            print(f"    검증 결과: {row[12]}")

            # 최종 판정
            all_ok = (row[3] == '✅' and row[6] == '✅' and
                     row[9] == '✅' and row[12] == '✅')

            print("\n" + "=" * 100)
            if all_ok:
                print("✅ 모든 이동평균이 정확하게 계산되었습니다!")
            else:
                print("⚠️  일부 이동평균에 오차가 있습니다. 재계산이 필요할 수 있습니다.")
            print("=" * 100)

        # 6. 기관별 이동평균 전체 확인 (최근 1일)
        print("\n[6] 모든 투자자 유형 이동평균 확인 (최근 1일)")
        cursor.execute("""
            SELECT
                dt,
                frgnr_invsr_ma5, frgnr_invsr_ma10, frgnr_invsr_ma20, frgnr_invsr_ma60,
                orgn_ma5, orgn_ma10, orgn_ma20, orgn_ma60,
                fnnc_invt_ma5, fnnc_invt_ma10, fnnc_invt_ma20, fnnc_invt_ma60,
                penfnd_etc_ma5, penfnd_etc_ma10, penfnd_etc_ma20, penfnd_etc_ma60
            FROM tb_stock_investor_ma
            WHERE stk_cd = '005930'
            ORDER BY dt DESC
            LIMIT 1
        """)

        row = cursor.fetchone()
        if row:
            print(f"\n  날짜: {row[0]}")
            print(f"\n  외국인:")
            print(f"    MA5={row[1]}, MA10={row[2]}, MA20={row[3]}, MA60={row[4]}")
            print(f"  기관:")
            print(f"    MA5={row[5]}, MA10={row[6]}, MA20={row[7]}, MA60={row[8]}")
            print(f"  금융투자:")
            print(f"    MA5={row[9]}, MA10={row[10]}, MA20={row[11]}, MA60={row[12]}")
            print(f"  연기금:")
            print(f"    MA5={row[13]}, MA10={row[14]}, MA20={row[15]}, MA60={row[16]}")

            # NULL 체크
            has_null = any(v is None for v in row[1:])
            if has_null:
                print("\n  ⚠️  일부 NULL 값이 존재합니다.")
            else:
                print("\n  ✅ 모든 이동평균이 계산되었습니다.")

        cursor.close()
        conn.close()

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    verify_samsung_ma()
