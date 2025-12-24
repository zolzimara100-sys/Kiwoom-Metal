package com.stocktrading.kiwoom.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stocktrading.kiwoom.config.KiwoomApiConfig;
import com.stocktrading.kiwoom.dto.DailyBalanceRequest;
import com.stocktrading.kiwoom.dto.DailyBalanceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 일별 잔고 수익률 조회 서비스
 * API ID: ka01690
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBalanceService {
    
    private final WebClient webClient;
    private final KiwoomApiConfig config;
    private final KiwoomAuthService authService;
    
    /**
     * 일별 잔고 수익률 조회
     * @param queryDate 조회일자 (YYYYMMDD)
     * @return 일별 잔고 수익률 응답
     */
    public Mono<DailyBalanceResponse> getDailyBalance(String queryDate) {
        return getDailyBalance(queryDate, "N", "");
    }
    
    /**
     * 일별 잔고 수익률 조회 (연속조회 지원)
     * @param queryDate 조회일자 (YYYYMMDD)
     * @param contYn 연속조회여부 (Y/N)
     * @param nextKey 연속조회키
     * @return 일별 잔고 수익률 응답
     */
    public Mono<DailyBalanceResponse> getDailyBalance(String queryDate, String contYn, String nextKey) {
        String token = authService.getValidToken();
        
        // 요청 데이터 생성
        DailyBalanceRequest request = DailyBalanceRequest.builder()
                .queryDate(queryDate)
                .build();
        
        // API 엔드포인트 URL
        String url = config.getBaseUrl() + "/api/dostk/acnt";
        
        log.info("일별 잔고 수익률 조회 시작: 조회일자={}, 연속조회={}", queryDate, contYn);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 일별 잔고 수익률 조회 요청");
        System.out.println("=".repeat(80));
        System.out.println("조회일자: " + queryDate);
        System.out.println("연속조회: " + contYn);
        if (!"N".equals(contYn) && !nextKey.isEmpty()) {
            System.out.println("연속키: " + nextKey);
        }
        System.out.println("=".repeat(80) + "\n");
        
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("authorization", "Bearer " + token)
                .header("cont-yn", contYn)
                .header("next-key", nextKey)
                .header("api-id", "ka01690")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("API 에러 응답: {}", errorBody);
                                System.out.println("\n" + "=".repeat(80));
                                System.out.println("❌ API 에러 응답:");
                                System.out.println("=".repeat(80));
                                System.out.println(errorBody);
                                System.out.println("=".repeat(80) + "\n");
                                return Mono.error(new RuntimeException("API Error: " + errorBody));
                            });
                })
                .bodyToMono(DailyBalanceResponse.class)
                .doOnSuccess(response -> {
                    log.info("일별 잔고 수익률 조회 성공: return_code={}, return_msg={}", 
                            response.getReturnCode(), response.getReturnMessage());
                    
                    System.out.println("\n" + "=".repeat(80));
                    System.out.println("✅ 일별 잔고 수익률 조회 성공!");
                    System.out.println("=".repeat(80));
                    System.out.println("조회일자: " + response.getDate());
                    System.out.println("총매수금액: " + response.getTotalBuyAmount() + " 원");
                    System.out.println("총평가금액: " + response.getTotalEvaluationAmount() + " 원");
                    System.out.println("총평가손익: " + response.getTotalEvaluationProfit() + " 원");
                    System.out.println("총수익률: " + response.getTotalProfitRate() + " %");
                    System.out.println("예수금잔액: " + response.getDepositBalance() + " 원");
                    System.out.println("=".repeat(80));
                    
                    if (response.getDailyBalanceList() != null && !response.getDailyBalanceList().isEmpty()) {
                        System.out.println("\n📈 보유 종목 목록 (" + response.getDailyBalanceList().size() + "개)");
                        System.out.println("-".repeat(80));
                        
                        for (DailyBalanceResponse.DailyBalanceItem item : response.getDailyBalanceList()) {
                            System.out.println(String.format("종목: %s (%s)", item.getStockName(), item.getStockCode()));
                            System.out.println(String.format("  현재가: %s원 | 보유수량: %s주 | 매수단가: %s원", 
                                    item.getCurrentPrice(), item.getRemainQuantity(), item.getBuyUnitValue()));
                            System.out.println(String.format("  평가손익: %s원 | 수익률: %s%% | 매수비중: %s%%", 
                                    item.getEvaluationProfit(), item.getProfitRate(), item.getBuyWeight()));
                            System.out.println("-".repeat(80));
                        }
                    }
                    System.out.println("응답메시지: " + response.getReturnMessage());
                    System.out.println("=".repeat(80) + "\n");
                })
                .doOnError(error -> {
                    log.error("일별 잔고 수익률 조회 실패: {}", error.getMessage());
                    System.out.println("\n❌ 일별 잔고 수익률 조회 실패: " + error.getMessage() + "\n");
                });
    }
}
