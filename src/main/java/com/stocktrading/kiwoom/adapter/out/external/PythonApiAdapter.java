package com.stocktrading.kiwoom.adapter.out.external;

import com.stocktrading.kiwoom.domain.port.out.PythonAnalysisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python 분석 API Adapter
 * PythonAnalysisPort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonApiAdapter implements PythonAnalysisPort {

    private final WebClient webClient;

    @Value("${python.analysis.base-url:http://localhost:8000}")
    private String pythonApiBaseUrl;

    @Override
    public Mono<TechnicalIndicatorResult> calculateTechnicalIndicators(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate,
            List<String> indicators
    ) {
        log.info("Python API 호출 - 기술적 지표 계산: {}", stockCode);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("stock_code", stockCode);
        requestBody.put("start_date", startDate.toString());
        requestBody.put("end_date", endDate.toString());
        requestBody.put("indicators", indicators);

        return webClient.post()
                .uri(pythonApiBaseUrl + "/api/analysis/technical-indicators")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new TechnicalIndicatorResult(
                        (String) response.get("stock_code"),
                        (Map<String, Object>) response.get("indicators"),
                        (String) response.get("calculated_at")
                ))
                .doOnSuccess(result -> log.info("기술적 지표 계산 완료 - {}", stockCode))
                .doOnError(error -> log.error("기술적 지표 계산 실패 - {}: {}", stockCode, error.getMessage()));
    }

    @Override
    public Mono<TrendAnalysisResult> analyzeTrend(String stockCode, int periodDays) {
        log.info("Python API 호출 - 추세 분석: {}", stockCode);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("stock_code", stockCode);
        requestBody.put("period_days", periodDays);

        return webClient.post()
                .uri(pythonApiBaseUrl + "/api/analysis/trend-analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new TrendAnalysisResult(
                        (String) response.get("stock_code"),
                        (String) response.get("trend_direction"),
                        ((Number) response.get("trend_strength")).doubleValue(),
                        (String) response.get("signal"),
                        ((Number) response.get("confidence")).doubleValue(),
                        (String) response.get("description")
                ))
                .doOnSuccess(result -> log.info("추세 분석 완료 - {}", stockCode))
                .doOnError(error -> log.error("추세 분석 실패 - {}: {}", stockCode, error.getMessage()));
    }

    @Override
    public Mono<InvestorPatternResult> analyzeInvestorPattern(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("Python API 호출 - 투자자 패턴 분석: {}", stockCode);

        String uri = String.format("%s/api/analysis/investor-pattern/%s?start_date=%s&end_date=%s",
                pythonApiBaseUrl, stockCode, startDate, endDate);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new InvestorPatternResult(
                        (String) response.get("stock_code"),
                        (String) response.get("dominant_investor"),
                        ((Number) response.get("foreign_strength")).doubleValue(),
                        ((Number) response.get("institution_strength")).doubleValue(),
                        ((Number) response.get("individual_strength")).doubleValue(),
                        ((Number) response.get("supply_demand_score")).doubleValue(),
                        (String) response.get("analysis")
                ))
                .doOnSuccess(result -> log.info("투자자 패턴 분석 완료 - {}", stockCode))
                .doOnError(error -> log.error("투자자 패턴 분석 실패 - {}: {}", stockCode, error.getMessage()));
    }

    @Override
    public Mono<CorrelationResult> analyzeCorrelation(
            String stockCode,
            List<String> targetStocks,
            int periodDays
    ) {
        log.info("Python API 호출 - 상관관계 분석: {}", stockCode);

        String targetStocksParam = String.join(",", targetStocks);
        String uri = String.format("%s/api/analysis/correlation/%s?target_stocks=%s&period_days=%d",
                pythonApiBaseUrl, stockCode, targetStocksParam, periodDays);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new CorrelationResult(
                        (String) response.get("stock_code"),
                        (Map<String, Double>) response.get("correlations"),
                        (String) response.get("most_correlated"),
                        ((Number) response.get("correlation_strength")).doubleValue()
                ))
                .doOnSuccess(result -> log.info("상관관계 분석 완료 - {}", stockCode))
                .doOnError(error -> log.error("상관관계 분석 실패 - {}: {}", stockCode, error.getMessage()));
    }
}
