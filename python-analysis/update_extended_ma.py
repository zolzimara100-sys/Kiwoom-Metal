#!/usr/bin/env python3
"""
확장 이동평균 1회성 업데이트 스크립트 (PRD-0012)
기존 tb_stock_investor_ma 테이블에 확장 이동평균 컬럼 값들을 계산하여 업데이트
30일, 40일, 50일, 90일, 1~12개월 이동평균
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

# 투자자 유형 매핑 (tb_stock_investor_chart 컬럼 -> tb_stock_investor_ma 컬럼 접두어)
INVESTORS = [
    ('frgnr_invsr', 'frgnr_invsr'),  # 외국인투자자
    ('orgn', 'orgn'),                # 기관계
    ('fnnc_invt', 'fnnc_invt'),      # 금융투자
    ('insrnc', 'insrnc'),            # 보험
    ('invtrt', 'invtrt'),            # 투신
    ('etc_fnnc', 'etc_fnnc'),        # 기타금융
    ('bank', 'bank'),                # 은행
    ('penfnd_etc', 'penfnd_etc'),    # 연기금등
    ('samo_fund', 'samo_fund'),      # 사모펀드
    ('natn', 'natn'),                # 국가
    ('etc_corp', 'etc_corp'),        # 기타법인
    ('natfor', 'natfor')             # 내외국인
]

# 확장 이동평균 기간 (일 단위)
# 30일, 40일, 50일, 90일, 1~12개월 (1개월=20거래일)
# 리스트로 관리 (기간, 컬럼명 접미어)
EXTENDED_PERIODS = [
    (30, 'ma30'),
    (40, 'ma40'),
    (50, 'ma50'),
    (90, 'ma90'),
    (20, 'ma1m'),   # 1개월
    (40, 'ma2m'),   # 2개월 (40일과 기간은 같지만 컬럼명이 다름)
    (60, 'ma3m'),   # 3개월
    (80, 'ma4m'),   # 4개월
    (100, 'ma5m'),  # 5개월
    (120, 'ma6m'),  # 6개월
    (140, 'ma7m'),  # 7개월
    (160, 'ma8m'),  # 8개월
    (180, 'ma9m'),  # 9개월
    (200, 'ma10m'), # 10개월
    (220, 'ma11m'), # 11개월
    (240, 'ma12m')  # 12개월
]


def generate_update_query(stk_cd, dt):
    """
    특정 종목/날짜의 확장 이동평균을 계산하는 UPDATE 쿼리 생성
    """

    # SET 절 생성
    set_clauses = []

    for src_col, target_prefix in INVESTORS:
        for period_days, period_suffix in EXTENDED_PERIODS:
            # 컬럼명 생성
            target_col = f"{target_prefix}_{period_suffix}"

            # 이동평균 계산 서브쿼리
            # 해당 날짜로부터 과거 period_days 거래일의 평균 (AVG)
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

    # UPDATE 쿼리 조립
    query = f"""
    UPDATE tb_stock_investor_ma
    SET {',\n        '.join(set_clauses)}
    WHERE stk_cd = '{stk_cd}'
      AND dt = '{dt}'
    """

    return query


def get_all_ma_records(cursor):
    """
    tb_stock_investor_ma의 모든 레코드 조회
    """
    query = """
    SELECT stk_cd, dt
    FROM tb_stock_investor_ma
    ORDER BY stk_cd, dt
    """
    cursor.execute(query)
    return cursor.fetchall()


def update_extended_ma(cursor, stk_cd, dt, batch_size=100):
    """
    특정 종목/날짜의 확장 이동평균 업데이트
    """
    query = generate_update_query(stk_cd, dt)
    cursor.execute(query)


def main():
    print("=" * 80)
    print("확장 이동평균 1회성 업데이트 시작 (PRD-0012)")
    print(f"시작 시간: {datetime.now()}")
    print("=" * 80)

    try:
        # DB 연결
        print(f"\n데이터베이스 연결 중... ({DB_CONFIG['host']}:{DB_CONFIG['port']})")
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False  # 트랜잭션 관리
        cursor = conn.cursor()
        print("✓ 연결 성공")

        # 전체 레코드 조회
        print("\n처리 대상 레코드 조회 중...")
        records = get_all_ma_records(cursor)
        total_count = len(records)
        print(f"✓ 총 {total_count:,}개 레코드 발견")

        if total_count == 0:
            print("처리할 데이터가 없습니다.")
            return

        # 배치 처리
        print("\n확장 이동평균 계산 및 업데이트 시작...")
        print("(30일, 40일, 50일, 90일, 1~12개월)")

        success_count = 0
        error_count = 0
        commit_interval = 100  # 100건마다 커밋

        for idx, (stk_cd, dt) in enumerate(records, 1):
            try:
                update_extended_ma(cursor, stk_cd, dt)
                success_count += 1

                # 진행상황 출력
                if idx % 10 == 0:
                    progress = (idx / total_count) * 100
                    print(f"  [{idx:,}/{total_count:,}] {progress:.1f}% - {stk_cd} / {dt}")

                # 주기적 커밋
                if idx % commit_interval == 0:
                    conn.commit()
                    print(f"  ✓ {idx:,}건 커밋 완료")

            except Exception as e:
                error_count += 1
                print(f"  ✗ 오류 발생: {stk_cd} / {dt} - {str(e)}")
                conn.rollback()

        # 최종 커밋
        conn.commit()
        print(f"\n✓ 최종 커밋 완료")

        # 결과 출력
        print("\n" + "=" * 80)
        print("처리 완료!")
        print(f"성공: {success_count:,}건")
        print(f"실패: {error_count:,}건")
        print(f"종료 시간: {datetime.now()}")
        print("=" * 80)

        cursor.close()
        conn.close()

    except Exception as e:
        print(f"\n✗ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1

    return 0


if __name__ == "__main__":
    exit(main())
