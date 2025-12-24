#!/usr/bin/env python3
"""
확장 이동평균 테스트 스크립트
최근 10개 레코드만 처리해서 테스트
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

# 투자자 유형 매핑
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


def generate_update_query(stk_cd, dt):
    """UPDATE 쿼리 생성"""
    set_clauses = []

    for src_col, target_prefix in INVESTORS:
        for period_days, period_suffix in EXTENDED_PERIODS:
            target_col = f"{target_prefix}_{period_suffix}"

            subquery = f"""(
                SELECT ROUND(AVG({src_col}), 2)
                FROM (
                    SELECT {src_col}
                    FROM tb_stock_investor_chart
                    WHERE stk_cd = '{stk_cd}'
                      AND dt <= TO_DATE('{dt}', 'YYYYMMDD')
                    ORDER BY dt DESC
                    LIMIT {period_days}
                ) recent_data
            )"""

            set_clauses.append(f"{target_col} = {subquery}")

    query = f"""
    UPDATE tb_stock_investor_ma
    SET {',\n        '.join(set_clauses)}
    WHERE stk_cd = '{stk_cd}'
      AND dt = '{dt}'
    """

    return query


def get_test_records(cursor, limit=10):
    """테스트용 최근 레코드 조회"""
    query = f"""
    SELECT stk_cd, dt
    FROM tb_stock_investor_ma
    ORDER BY dt DESC, stk_cd
    LIMIT {limit}
    """
    cursor.execute(query)
    return cursor.fetchall()


def verify_update(cursor, stk_cd, dt):
    """업데이트된 데이터 확인"""
    query = f"""
    SELECT stk_cd, dt,
           orgn_ma30, orgn_ma40, orgn_ma50, orgn_ma90,
           orgn_ma4m, orgn_ma6m, orgn_ma8m, orgn_ma10m, orgn_ma12m
    FROM tb_stock_investor_ma
    WHERE stk_cd = '{stk_cd}'
      AND dt = '{dt}'
    """
    cursor.execute(query)
    return cursor.fetchone()


def main():
    print("=" * 80)
    print("확장 이동평균 테스트 (최근 10개 레코드)")
    print(f"시작 시간: {datetime.now()}")
    print("=" * 80)

    try:
        # DB 연결
        print(f"\n데이터베이스 연결 중...")
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        cursor = conn.cursor()
        print("✓ 연결 성공")

        # 테스트 레코드 조회
        print("\n테스트 대상 레코드 조회 중...")
        records = get_test_records(cursor, limit=10)
        print(f"✓ {len(records)}개 레코드 발견")

        if not records:
            print("처리할 데이터가 없습니다.")
            return

        # 첫 번째 레코드 업데이트 전 상태 확인
        first_stk_cd, first_dt = records[0]
        print(f"\n[업데이트 전] {first_stk_cd} / {first_dt}")
        before = verify_update(cursor, first_stk_cd, first_dt)
        if before:
            print(f"  orgn_ma30={before[2]}, orgn_ma40={before[3]}, orgn_ma4m={before[6]}")

        # 처리
        print("\n확장 이동평균 계산 및 업데이트...")
        for idx, (stk_cd, dt) in enumerate(records, 1):
            try:
                print(f"\n[{idx}/10] {stk_cd} / {dt}")
                query = generate_update_query(stk_cd, dt)

                # 쿼리 일부 출력 (디버깅용)
                if idx == 1:
                    print("생성된 쿼리 샘플 (첫 100자):")
                    print(query[:200] + "...")

                cursor.execute(query)
                print("  ✓ 업데이트 완료")

                # 결과 확인
                result = verify_update(cursor, stk_cd, dt)
                if result:
                    print(f"  검증: orgn_ma30={result[2]}, orgn_ma40={result[3]}, orgn_ma4m={result[6]}")

            except Exception as e:
                print(f"  ✗ 오류: {str(e)[:100]}")
                conn.rollback()
                raise

        # 커밋 확인
        print("\n커밋하시겠습니까? (y/n): ", end='')
        answer = input().strip().lower()

        if answer == 'y':
            conn.commit()
            print("✓ 커밋 완료")
        else:
            conn.rollback()
            print("✗ 롤백 완료")

        cursor.close()
        conn.close()

        print("\n" + "=" * 80)
        print("테스트 완료!")
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
