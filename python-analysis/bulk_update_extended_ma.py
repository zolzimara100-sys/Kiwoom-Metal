#!/usr/bin/env python3
"""
확장 이동평균 일괄 업데이트 스크립트
윈도우 함수를 사용하여 효율적으로 전체 데이터를 한 번에 업데이트
"""

import psycopg2
import os
from datetime import datetime

# 데이터베이스 연결 정보
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': os.getenv('DB_PORT', '5432'),
    'database': os.getenv('DB_NAME', 'kiwoom'),
    'user': os.getenv('DB_USER', 'kiwoom'),
    'password': os.getenv('DB_PASSWORD', 'kiwoom123')
}

# 투자자 유형
INVESTORS = [
    ('frgnr_invsr', 'frgnr_invsr'),
    ('orgn', 'orgn'),
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

# 확장 이동평균 기간
EXTENDED_PERIODS = [
    (30, 'ma30'),
    (40, 'ma40'),
    (50, 'ma50'),
    (90, 'ma90'),
    (20, 'ma1m'),
    (40, 'ma2m'),
    (60, 'ma3m'),
    (80, 'ma4m'),
    (100, 'ma5m'),
    (120, 'ma6m'),
    (140, 'ma7m'),
    (160, 'ma8m'),
    (180, 'ma9m'),
    (200, 'ma10m'),
    (220, 'ma11m'),
    (240, 'ma12m')
]


def generate_bulk_update_query():
    """
    전체 레코드를 한 번에 업데이트하는 쿼리 생성
    윈도우 함수 사용
    """

    # 각 투자자/기간별 MA 계산 SELECT 절 생성
    ma_calcs = []
    for src_col, target_prefix in INVESTORS:
        for period_days, period_suffix in EXTENDED_PERIODS:
            target_col = f"{target_prefix}_{period_suffix}"

            # 윈도우 함수로 이동평균 계산
            ma_calc = f"""
        ROUND(AVG({src_col}) OVER (
            PARTITION BY stk_cd
            ORDER BY dt
            ROWS BETWEEN {period_days - 1} PRECEDING AND CURRENT ROW
        ), 2) AS {target_col}"""
            ma_calcs.append(ma_calc)

    ma_calcs_str = ",".join(ma_calcs)

    # SET 절 생성
    set_clauses = []
    for src_col, target_prefix in INVESTORS:
        for period_days, period_suffix in EXTENDED_PERIODS:
            target_col = f"{target_prefix}_{period_suffix}"
            set_clauses.append(f"{target_col} = calc.{target_col}")

    set_clauses_str = ",\n        ".join(set_clauses)

    # 전체 쿼리
    query = f"""
    WITH calculated AS (
        SELECT
            c.stk_cd,
            TO_CHAR(c.dt, 'YYYYMMDD') AS dt_str,
            {ma_calcs_str}
        FROM tb_stock_investor_chart c
        ORDER BY c.stk_cd, c.dt
    )
    UPDATE tb_stock_investor_ma AS ma
    SET {set_clauses_str}
    FROM calculated AS calc
    WHERE ma.stk_cd = calc.stk_cd
      AND ma.dt = calc.dt_str
    """

    return query


def main():
    print("=" * 80)
    print("확장 이동평균 일괄 업데이트 (전체 레코드)")
    print(f"시작 시간: {datetime.now()}")
    print("=" * 80)

    try:
        # DB 연결
        print(f"\n데이터베이스 연결 중...")
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        cursor = conn.cursor()
        print("✓ 연결 성공")

        # 총 레코드 수 확인
        cursor.execute("SELECT COUNT(*) FROM tb_stock_investor_ma")
        total_count = cursor.fetchone()[0]
        print(f"\n총 {total_count:,}개 레코드 업데이트 예정")

        # 쿼리 생성
        print("\n쿼리 생성 중...")
        query = generate_bulk_update_query()
        print("✓ 쿼리 생성 완료")

        # 쿼리 일부 출력
        print("\n생성된 쿼리 샘플 (첫 500자):")
        print(query[:500] + "...")

        print("\n\n경고: 전체 데이터를 업데이트합니다!")
        print("계속하시겠습니까? (yes/no): ", end='')
        answer = input().strip().lower()

        if answer != 'yes':
            print("취소되었습니다.")
            return 0

        # 실행
        print("\n업데이트 실행 중... (시간이 걸릴 수 있습니다)")
        start_time = datetime.now()

        cursor.execute(query)
        updated_count = cursor.rowcount

        elapsed = datetime.now() - start_time
        print(f"✓ 업데이트 완료: {updated_count:,}개 레코드")
        print(f"  소요시간: {elapsed.total_seconds():.1f}초")

        # 샘플 검증
        print("\n샘플 검증 중...")
        cursor.execute("""
            SELECT stk_cd, dt,
                   orgn_ma30, orgn_ma40, orgn_ma50, orgn_ma90,
                   orgn_ma4m, orgn_ma6m, orgn_ma8m, orgn_ma10m, orgn_ma12m
            FROM tb_stock_investor_ma
            ORDER BY dt DESC, stk_cd
            LIMIT 3
        """)

        for row in cursor.fetchall():
            stk_cd, dt = row[0], row[1]
            print(f"\n{stk_cd} / {dt}:")
            print(f"  ma30={row[2]}, ma40={row[3]}, ma50={row[4]}, ma90={row[5]}")
            print(f"  ma4m={row[6]}, ma6m={row[7]}, ma8m={row[8]}, ma10m={row[9]}, ma12m={row[10]}")

        # 커밋 확인
        print("\n\n커밋하시겠습니까? (yes/no): ", end='')
        answer = input().strip().lower()

        if answer == 'yes':
            conn.commit()
            print("✓ 커밋 완료")
        else:
            conn.rollback()
            print("✗ 롤백 완료")

        cursor.close()
        conn.close()

        print("\n" + "=" * 80)
        print("완료!")
        print(f"종료 시간: {datetime.now()}")
        print("=" * 80)

    except Exception as e:
        print(f"\n✗ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1

    return 0


if __name__ == "__main__":
    exit(main())
