package com.stocktrading.kiwoom.application.service;

import com.stocktrading.kiwoom.domain.model.StockInfo;
import com.stocktrading.kiwoom.domain.port.in.FetchStockListUseCase;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.KiwoomApiPort;
import com.stocktrading.kiwoom.domain.port.out.StockListPort;
import com.stocktrading.kiwoom.domain.tr.StockListTR;
import com.stocktrading.kiwoom.service.OAuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 종목 리스트 Application Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockListApplicationService implements FetchStockListUseCase {

    private final KiwoomApiPort kiwoomApiPort;
    private final AuthPort authPort;
    private final StockListPort stockListPort;
    private final OAuthTokenService oAuthTokenService;

    // 시장 구분 코드
    private static final Map<String, String> MARKET_TYPES = Map.of(
            "0", "코스피"
    );

    @Override
    public Mono<FetchResult> fetchByMarket(String marketType) {
        log.info("종목 리스트 조회 시작 - 시장구분: {}", marketType);
        long startTime = System.currentTimeMillis();

        return fetchMarketData(marketType)
                .collectList()
                .map(stocks -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    log.info("종목 리스트 조회 완료 - 시장: {}, 건수: {}, 소요시간: {}ms",
                            marketType, stocks.size(), executionTime);

                    Map<String, Integer> marketCounts = Map.of(marketType, stocks.size());
                    return FetchResult.success(stocks.size(), marketCounts, executionTime);
                })
                .onErrorResume(e -> {
                    log.error("종목 리스트 조회 실패 - 시장: {}, 오류: {}", marketType, e.getMessage(), e);
                    return Mono.just(FetchResult.failure("조회 실패: " + e.getMessage()));
                });
    }

    @Override
    public Mono<FetchResult> fetchAllMarketsAndRefresh() {
        log.info("=".repeat(60));
        log.info("전체 시장 종목 리스트 갱신 시작");
        log.info("=".repeat(60));
        long startTime = System.currentTimeMillis();

        Map<String, Integer> marketCounts = new ConcurrentHashMap<>();
        List<StockInfo> allStocks = Collections.synchronizedList(new ArrayList<>());

        return Flux.fromIterable(MARKET_TYPES.keySet())
                .flatMap(marketType ->
                    fetchMarketData(marketType)
                            .doOnNext(allStocks::add)
                            .collectList()
                            .doOnNext(stocks -> {
                                marketCounts.put(marketType, stocks.size());
                                log.info("[{}] {} 종목 수집 완료: {}건",
                                        marketType, MARKET_TYPES.get(marketType), stocks.size());
                            })
                )
                .then(Mono.fromCallable(() -> {
                    log.info("전체 수집 완료 - 총 {}건", allStocks.size());

                    // 1. 기존 데이터 삭제
                    log.info("기존 데이터 삭제 시작");
                    int deletedCount = stockListPort.deleteAll();
                    log.info("기존 데이터 삭제 완료 - {}건", deletedCount);

                    // 2. 새 데이터 저장
                    log.info("신규 데이터 저장 시작 - {}건", allStocks.size());
                    int savedCount = stockListPort.saveAll(allStocks);
                    log.info("신규 데이터 저장 완료 - {}건", savedCount);

                    long executionTime = System.currentTimeMillis() - startTime;

                    log.info("=".repeat(60));
                    log.info("전체 시장 종목 리스트 갱신 완료");
                    log.info("총 종목 수: {}", savedCount);
                    marketCounts.forEach((market, count) ->
                        log.info("  [{}] {}: {}건", market, MARKET_TYPES.get(market), count)
                    );
                    log.info("소요 시간: {}.{}초", executionTime / 1000, executionTime % 1000);
                    log.info("=".repeat(60));

                    return FetchResult.success(savedCount, new HashMap<>(marketCounts), executionTime);
                }))
                .onErrorResume(e -> {
                    log.error("전체 시장 갱신 실패: {}", e.getMessage(), e);
                    return Mono.just(FetchResult.failure("갱신 실패: " + e.getMessage()));
                });
    }

    /**
     * 특정 시장의 종목 데이터 조회
     */
    private Flux<StockInfo> fetchMarketData(String marketType) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("mrkt_tp", marketType);

        // 글로벌 캐시 토큰 우선 사용
        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken != null && !cachedToken.isEmpty()) {
            log.debug("글로벌 캐시 토큰 사용 - 시장: {}", marketType);
            return kiwoomApiPort.postWithHeaders("ka10099", requestBody, cachedToken, "N", "")
                    .flatMapMany(responseBody -> parseStockListResponse(responseBody, marketType));
        }

        // fallback to AuthPort
        log.warn("글로벌 토큰 없음. AuthPort 사용 - 시장: {}", marketType);
        return authPort.getToken()
                .flatMapMany(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return Flux.error(new IllegalStateException("Token not found. Please connect to Kiwoom server from main page first."));
                    }
                    
                    String token = tokenOpt.get().getAccessToken();
                    log.debug("API 요청 - 시장: {}, Token: {}...", marketType,
                            token.substring(0, Math.min(20, token.length())));

                    return kiwoomApiPort.postWithHeaders("ka10099", requestBody, token, "N", "")
                            .flatMapMany(responseBody -> parseStockListResponse(responseBody, marketType));
                });
    }

    /**
     * StockList API 응답 파싱
     */
    private Flux<StockInfo> parseStockListResponse(String responseBody, String marketType) {
        try {
            StockListTR tr = new StockListTR();
            StockListTR.Response response = tr.parseResponse(responseBody);

            if (!response.isSuccess()) {
                log.error("API 응답 오류 - Code: {}, Message: {}",
                        response.getReturnCode(), response.getReturnMsg());
                return Flux.empty();
            }

            int totalCount = response.getStockList().size();

            // 거래소(market_code="0")만 필터링
            List<StockInfo> stocks = response.getStockList().stream()
                    .filter(data -> "0".equals(data.getMarketCode()))
                    .map(this::mapToStockInfo)
                    .toList();

            log.info("시장 {} 데이터 파싱 완료 - 전체: {}건, 거래소 필터링 후: {}건",
                    marketType, totalCount, stocks.size());
            return Flux.fromIterable(stocks);

        } catch (Exception e) {
            log.error("응답 파싱 실패 - 시장: {}, 오류: {}", marketType, e.getMessage(), e);
            return Flux.empty();
        }
    }

    /**
     * StockListTR.StockData -> StockInfo 변환
     */
    private StockInfo mapToStockInfo(StockListTR.StockData data) {
        return StockInfo.builder()
                .code(data.getCode())
                .name(data.getName())
                .listCount(data.getListCount())
                .auditInfo(data.getAuditInfo())
                .regDay(data.getRegDay())
                .lastPrice(data.getLastPrice())
                .state(data.getState())
                .marketCode(data.getMarketCode())
                .marketName(data.getMarketName())
                .upName(data.getUpName())
                .upSizeName(data.getUpSizeName())
                .companyClassName(data.getCompanyClassName())
                .orderWarning(data.getOrderWarning())
                .nxtEnable(data.getNxtEnable())
                .build();
    }
}
