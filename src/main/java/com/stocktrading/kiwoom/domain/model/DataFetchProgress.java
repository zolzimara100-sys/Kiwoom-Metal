package com.stocktrading.kiwoom.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * 데이터 조회 진행률 추적
 * Redis에 저장하여 실시간 모니터링 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFetchProgress {

    /**
     * 작업 ID (예: "stock-005930-20250101-20251231")
     */
    private String jobId;

    /**
     * 종목 코드
     */
    private String stockCode;

    /**
     * 현재 상태 (PENDING, RUNNING, COMPLETED, FAILED)
     */
    @Builder.Default
    private FetchStatus status = FetchStatus.PENDING;

    /**
     * 현재 처리된 건수
     */
    @Builder.Default
    private int currentCount = 0;

    /**
     * 전체 예상 건수
     */
    private int totalCount;

    /**
     * 현재 페이지 번호
     */
    @Builder.Default
    private int currentPage = 0;

    /**
     * 성공 건수
     */
    @Builder.Default
    private int successCount = 0;

    /**
     * 실패 건수
     */
    @Builder.Default
    private int failureCount = 0;

    /**
     * 재시도 횟수
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * 시작 시간
     */
    private Instant startTime;

    /**
     * 종료 시간
     */
    private Instant endTime;

    /**
     * 마지막 업데이트 시간
     */
    private Instant lastUpdateTime;

    /**
     * 에러 메시지
     */
    private String errorMessage;

    /**
     * 진행률 계산 (%)
     */
    public double getProgressPercentage() {
        if (totalCount == 0) return 0.0;
        return (currentCount * 100.0) / totalCount;
    }

    /**
     * 경과 시간 계산
     */
    public Duration getElapsedTime() {
        if (startTime == null) return Duration.ZERO;
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    /**
     * 예상 남은 시간 계산
     */
    public Duration getEstimatedRemainingTime() {
        if (currentCount == 0 || totalCount == 0) {
            return Duration.ZERO;
        }

        long elapsedSeconds = getElapsedTime().getSeconds();
        if (elapsedSeconds == 0) return Duration.ZERO;

        double itemsPerSecond = (double) currentCount / elapsedSeconds;
        int remainingItems = totalCount - currentCount;
        long remainingSeconds = (long) (remainingItems / itemsPerSecond);

        return Duration.ofSeconds(remainingSeconds);
    }

    /**
     * 처리 속도 계산 (초당 건수)
     */
    public double getProcessingRate() {
        long elapsedSeconds = getElapsedTime().getSeconds();
        if (elapsedSeconds == 0) return 0.0;
        return (double) currentCount / elapsedSeconds;
    }

    /**
     * 성공률 계산 (%)
     */
    public double getSuccessRate() {
        int totalProcessed = successCount + failureCount;
        if (totalProcessed == 0) return 0.0;
        return (successCount * 100.0) / totalProcessed;
    }

    /**
     * 진행률 문자열 출력
     */
    public String getProgressString() {
        return String.format("[%s] %d/%d (%.1f%%) - 성공: %d, 실패: %d, 재시도: %d",
                status, currentCount, totalCount, getProgressPercentage(),
                successCount, failureCount, retryCount);
    }

    /**
     * 작업 상태
     */
    public enum FetchStatus {
        PENDING("대기중"),
        RUNNING("실행중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨");

        private final String description;

        FetchStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
