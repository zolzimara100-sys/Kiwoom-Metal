#!/usr/bin/env python3
"""
GPU Maximum Load Test
GPU 사용률을 최대로 올리는 연속 계산 테스트
"""

import torch
import numpy as np
import time

def gpu_max_load_test(duration_seconds=60):
    """GPU 최대 부하 테스트 - 지정된 시간동안 실행"""

    if torch.backends.mps.is_available():
        device = torch.device("mps")
        print(f"✅ Metal GPU (MPS) detected")
    else:
        print("❌ GPU not available")
        return

    print(f"\n🎮 GPU 최대 부하 테스트 시작!")
    print(f"   - Duration: {duration_seconds} seconds")
    print(f"   - Device: {device}")
    print(f"\n⚡ GPU 모니터링을 시작하세요!")
    print(f"   macOS: sudo powermetrics --samplers gpu_power -i 500")
    print(f"   Activity Monitor > Window > GPU History\n")

    time.sleep(3)

    start_time = time.time()
    iteration = 0

    # 대용량 데이터로 연속 GPU 연산
    while (time.time() - start_time) < duration_seconds:
        # 대량의 랜덤 데이터 생성 (시뮬레이션: 1000개 종목 x 1000일)
        data_size = 1000000
        data = np.random.randn(data_size).astype(np.float32)

        # GPU로 전송 (optimized with explicit dtype)
        tensor = torch.from_numpy(data).to(device=device, dtype=torch.float32)

        # 여러 윈도우 크기로 동시 계산 (GPU 부하 극대화)
        windows = [5, 10, 20, 30, 40, 50, 60, 90, 120, 200, 300]

        for window in windows:
            # GPU Convolution (Heavy computation, optimized with explicit dtype)
            kernel = torch.ones(window, device=device, dtype=torch.float32) / window
            padded = torch.nn.functional.pad(
                tensor.unsqueeze(0).unsqueeze(0),
                (window-1, 0)
            )
            ma = torch.nn.functional.conv1d(
                padded,
                kernel.unsqueeze(0).unsqueeze(0)
            ).squeeze()

            # 추가 GPU 연산 (행렬 곱셈)
            matrix = torch.randn(1000, 1000).to(device)
            result = torch.matmul(matrix, matrix)

        iteration += 1
        elapsed = time.time() - start_time

        if iteration % 5 == 0:
            print(f"🔥 Iteration: {iteration:,} | "
                  f"Elapsed: {elapsed:.1f}s | "
                  f"GPU Load: MAXIMUM | "
                  f"Calculations: {iteration * len(windows):,}")

    total_time = time.time() - start_time

    print(f"\n✅ GPU Maximum Load Test Complete!")
    print(f"   - Total iterations: {iteration:,}")
    print(f"   - Total calculations: {iteration * len(windows):,}")
    print(f"   - Time: {total_time:.1f} seconds")
    print(f"   - Device: {device}")
    print(f"   - Average GPU operations/sec: {(iteration * len(windows))/total_time:.1f}")

if __name__ == "__main__":
    # 60초간 GPU 최대 부하 실행
    gpu_max_load_test(duration_seconds=60)
