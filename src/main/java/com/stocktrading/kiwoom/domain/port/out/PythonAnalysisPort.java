package com.stocktrading.kiwoom.domain.port.out;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Python 분석 API 호출 Port
 */
public interface PythonAnalysisPort {

    /**
     * 기술적 지표 계산
     */
    Mono<TechnicalIndicatorResult> calculateTechnicalIndicators(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate,
            List<String> indicators
    );

    /**
     * 추세 분석
     */
    Mono<TrendAnalysisResult> analyzeTrend(
            String stockCode,
            int periodDays
    );

    /**
     * 투자자 패턴 분석
     */
    Mono<InvestorPatternResult> analyzeInvestorPattern(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 상관관계 분석
     */
    Mono<CorrelationResult> analyzeCorrelation(
            String stockCode,
            List<String> targetStocks,
            int periodDays
    );

    /**
     * 기술적 지표 결과
     */
    record TechnicalIndicatorResult(
            String stockCode,
            Map<String, Object> indicators,
            String calculatedAt
    ) {}

    /**
     * 추세 분석 결과
     */
    record TrendAnalysisResult(
            String stockCode,
            String trendDirection,
            double trendStrength,
            String signal,
            double confidence,
            String description
    ) {}

    /**
     * 투자자 패턴 결과
     */
    record InvestorPatternResult(
            String stockCode,
            String dominantInvestor,
            double foreignStrength,
            double institutionStrength,
            double individualStrength,
            double supplyDemandScore,
            String analysis
    ) {}

    /**
     * 상관관계 분석 결과
     */
    record CorrelationResult(
            String stockCode,
            Map<String, Double> correlations,
            String mostCorrelated,
            double correlationStrength
    ) {}
}
