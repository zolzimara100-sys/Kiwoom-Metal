#!/usr/bin/env python3
"""
확장 이동평균 일괄 업데이트 스크립트 - DuckDB 버전
GPU 가속을 사용하여 효율적으로 전체 데이터를 한 번에 업데이트
"""

import duckdb
import torch
import numpy as np
import pandas as pd
from pathlib import Path
from datetime import datetime

# 데이터 경로
DATA_DIR = Path('/Users/juhyunhwang/kiwoom-metal/data')
PARQUET_DIR = DATA_DIR / 'parquet'
DB_PATH = DATA_DIR / 'kiwoom.duckdb'

# 투자자 유형 (소스 컬럼명)
INVESTORS = [
    'frgnr_invsr',
    'orgn',
    'fnnc_invt',
    'insrnc',
    'invtrt',
    'etc_fnnc',
    'bank',
    'penfnd_etc',
    'samo_fund',
    'natn',
    'etc_corp',
    'natfor'
]

# 확장 이동평균 기간
EXTENDED_PERIODS = [30, 40, 50, 60, 90, 120, 140]


def calculate_ma_gpu(data: np.ndarray, window: int, device: torch.device) -> np.ndarray:
    """
    GPU를 사용한 이동평균 계산
    """
    if len(data) < window:
        return np.full(len(data), np.nan)

    # Convert to PyTorch tensor on GPU (optimized with explicit dtype)
    tensor = torch.from_numpy(data).to(device=device, dtype=torch.float32)

    # Create convolution kernel for moving average (optimized with explicit dtype)
    kernel = torch.ones(window, device=device, dtype=torch.float32) / window

    # Pad and apply 1D convolution (GPU accelerated!)
    padded = torch.nn.functional.pad(tensor.unsqueeze(0).unsqueeze(0), (window-1, 0))
    ma = torch.nn.functional.conv1d(
        padded,
        kernel.unsqueeze(0).unsqueeze(0)
    ).squeeze()

    return ma.cpu().numpy()


def main():
    print("=" * 80)
    print("확장 이동평균 일괄 업데이트 - DuckDB + GPU 가속")
    print(f"시작 시간: {datetime.now()}")
    print("=" * 80)

    # GPU 설정
    if torch.backends.mps.is_available():
        device = torch.device("mps")
        print(f"\n✅ GPU 가속 사용: {device}")
    else:
        device = torch.device("cpu")
        print(f"\n⚠️  GPU 없음, CPU 사용: {device}")

    try:
        # DuckDB 연결
        print(f"\nDuckDB 연결 중: {DB_PATH}")
        conn = duckdb.connect(str(DB_PATH))
        print("✓ 연결 성공")

        # 원본 데이터 로드
        chart_path = PARQUET_DIR / 'tb_stock_investor_chart.parquet'
        print(f"\n데이터 로드 중: {chart_path}")

        df = conn.execute(f"""
            SELECT
                stk_cd,
                dt,
                {', '.join(INVESTORS)}
            FROM read_parquet('{chart_path}')
            ORDER BY stk_cd, dt
        """).df()

        print(f"✓ 로드 완료: {len(df):,} 레코드")
        print(f"  종목 수: {df['stk_cd'].nunique():,}")

        # GPU로 이동평균 계산
        print(f"\nGPU 이동평균 계산 중...")
        start_time = datetime.now()

        result_data = []
        stocks = df['stk_cd'].unique()

        for idx, stk_cd in enumerate(stocks, 1):
            stock_data = df[df['stk_cd'] == stk_cd].sort_values('dt')

            if len(stock_data) < max(EXTENDED_PERIODS):
                continue

            stock_result = {
                'stk_cd': stk_cd,
                'dt': stock_data['dt'].values
            }

            # 각 투자자 유형별 이동평균 계산
            for investor in INVESTORS:
                investor_data = stock_data[investor].fillna(0).values

                for period in EXTENDED_PERIODS:
                    ma_values = calculate_ma_gpu(investor_data, period, device)
                    col_name = f"{investor}_ma{period}"
                    stock_result[col_name] = ma_values

            result_data.append(pd.DataFrame(stock_result))

            if idx % 100 == 0:
                elapsed = (datetime.now() - start_time).total_seconds()
                rate = idx / elapsed if elapsed > 0 else 0
                remaining = (len(stocks) - idx) / rate if rate > 0 else 0
                print(f"  진행: {idx}/{len(stocks)} ({idx/len(stocks)*100:.1f}%) "
                      f"| {rate:.1f} stocks/sec | 남은 시간: {remaining:.0f}초")

        # 결과 병합
        print("\n결과 병합 중...")
        result_df = pd.concat(result_data, ignore_index=True)

        elapsed = datetime.now() - start_time
        print(f"✓ 계산 완료: {elapsed.total_seconds():.1f}초")
        print(f"  결과 레코드: {len(result_df):,}")

        # Parquet 저장
        output_path = PARQUET_DIR / 'tb_stock_investor_ma_extended.parquet'
        print(f"\nParquet 저장 중: {output_path}")
        result_df.to_parquet(output_path, engine='pyarrow', index=False)
        print("✓ 저장 완료")

        # DuckDB 테이블 생성/교체
        print("\nDuckDB 테이블 업데이트 중...")
        conn.execute(f"""
            CREATE OR REPLACE TABLE tb_stock_investor_ma_extended AS
            SELECT * FROM read_parquet('{output_path}')
        """)
        print("✓ 테이블 생성 완료")

        # 샘플 검증
        print("\n샘플 검증 (최근 3개 레코드):")
        sample = conn.execute("""
            SELECT stk_cd, dt,
                   orgn_ma30, orgn_ma40, orgn_ma50, orgn_ma90,
                   orgn_ma120, orgn_ma140
            FROM tb_stock_investor_ma_extended
            ORDER BY dt DESC, stk_cd
            LIMIT 3
        """).df()

        print(sample.to_string(index=False))

        conn.close()

        print("\n" + "=" * 80)
        print("✅ 완료!")
        print(f"종료 시간: {datetime.now()}")
        print(f"총 소요시간: {(datetime.now() - start_time).total_seconds():.1f}초")
        print(f"GPU 디바이스: {device}")
        print("=" * 80)

    except Exception as e:
        print(f"\n✗ 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1

    return 0


if __name__ == "__main__":
    exit(main())
