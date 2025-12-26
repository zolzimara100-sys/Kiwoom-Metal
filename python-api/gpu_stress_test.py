#!/usr/bin/env python3
"""
GPU Stress Test - Maximum GPU Utilization
PyTorch MPS를 사용하여 GPU 사용률을 극대화하는 테스트
"""

import torch
import numpy as np
import time
import duckdb
from pathlib import Path

def gpu_stress_test():
    """GPU 최대 사용률 테스트"""

    # GPU 설정
    if torch.backends.mps.is_available():
        device = torch.device("mps")
        print(f"✅ Metal GPU (MPS) detected")
    else:
        print("❌ GPU not available")
        return

    # DuckDB 연결
    db_path = '/Users/juhyunhwang/kiwoom-metal/data/kiwoom.duckdb'
    print(f"📊 Loading data from DuckDB...")

    conn = duckdb.connect(db_path, read_only=True)

    # 모든 종목의 데이터 로드
    query = """
        SELECT
            stk_cd,
            dt,
            frgnr,
            orgn,
            indiv,
            cur_prc
        FROM read_parquet('/Users/juhyunhwang/kiwoom-metal/data/parquet/tb_stock_investor_daily.parquet')
        ORDER BY stk_cd, dt
    """

    df = conn.execute(query).df()
    conn.close()

    print(f"📈 Loaded {len(df):,} rows")
    print(f"📈 Total stocks: {df['stk_cd'].nunique()}")

    # GPU로 대량 이동평균 계산
    ma_windows = [5, 10, 20, 30, 40, 50, 60, 90, 120]

    print(f"\n🎮 Starting GPU intensive calculations...")
    print(f"   - Windows: {ma_windows}")
    print(f"   - Device: {device}")
    print(f"\n⏱️  GPU 모니터링을 시작하세요!")
    print(f"   macOS: sudo powermetrics --samplers gpu_power -i 500")
    print(f"   또는: Activity Monitor > Window > GPU History\n")

    time.sleep(3)

    start_time = time.time()
    processed_count = 0

    # 종목별로 GPU 계산 수행
    for stk_cd, group in df.groupby('stk_cd'):
        group = group.sort_values('dt')

        # 각 투자자 유형별 처리
        for investor_col in ['frgnr', 'orgn', 'indiv']:
            data = group[investor_col].values

            if len(data) < 10:
                continue

            # GPU로 데이터 전송 (optimized with explicit dtype)
            tensor = torch.from_numpy(data).to(device=device, dtype=torch.float32)

            # 모든 윈도우에 대해 이동평균 계산 (GPU intensive!)
            for window in ma_windows:
                if len(data) >= window:
                    # GPU Convolution (optimized with explicit dtype)
                    kernel = torch.ones(window, device=device, dtype=torch.float32) / window
                    padded = torch.nn.functional.pad(
                        tensor.unsqueeze(0).unsqueeze(0),
                        (window-1, 0)
                    )
                    ma = torch.nn.functional.conv1d(
                        padded,
                        kernel.unsqueeze(0).unsqueeze(0)
                    ).squeeze()

                    # CPU로 결과 전송 (동기화)
                    result = ma.cpu().numpy()
                    processed_count += 1

        # 진행상황 출력
        if processed_count % 100 == 0:
            elapsed = time.time() - start_time
            rate = processed_count / elapsed if elapsed > 0 else 0
            print(f"🎮 Processed: {processed_count:,} calculations | "
                  f"Rate: {rate:.1f} calc/sec | "
                  f"Stock: {stk_cd}")

    elapsed = time.time() - start_time

    print(f"\n✅ GPU Stress Test Complete!")
    print(f"   - Total calculations: {processed_count:,}")
    print(f"   - Time: {elapsed:.1f} seconds")
    print(f"   - Rate: {processed_count/elapsed:.1f} calculations/second")
    print(f"   - Device: {device}")

if __name__ == "__main__":
    gpu_stress_test()
