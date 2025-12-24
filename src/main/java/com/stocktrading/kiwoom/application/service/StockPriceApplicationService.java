package com.stocktrading.kiwoom.application.service;

import com.stocktrading.kiwoom.domain.model.StockPrice;
import com.stocktrading.kiwoom.domain.port.in.QueryStockPriceUseCase;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.StockPricePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 주가 조회 Application Service (Use Case 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceApplicationService implements QueryStockPriceUseCase {

    private final StockPricePort stockPricePort;
    private final AuthPort authPort;

    @Override
    public Mono<StockPrice> queryRealTimePrice(String stockCode) {
        log.debug("실시간 주가 조회 - 종목: {}", stockCode);

        return stockPricePort.findCachedPrice(stockCode)
                .flatMap(cachedOpt -> {
                    if (cachedOpt.isPresent()) {
                        log.debug("캐시에서 주가 조회 성공 - 종목: {}", stockCode);
                        return Mono.just(cachedOpt.get());
                    }

                    log.debug("캐시 미스. API에서 주가 조회 - 종목: {}", stockCode);
                    return queryPriceFromApi(stockCode);
                });
    }

    @Override
    public Mono<StockPrice> queryPriceFromApi(String stockCode) {
        return authPort.getToken()
                .flatMap(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return Mono.error(new IllegalStateException("Token not found"));
                    }

                    String token = tokenOpt.get().getAccessToken();
                    return stockPricePort.fetchPriceFromApi(stockCode, token)
                            .flatMap(price -> stockPricePort.cachePrice(price).thenReturn(price));
                })
                .doOnSuccess(price -> log.info("주가 조회 및 캐싱 완료 - 종목: {}, 가격: {}",
                        stockCode, price.getCurrentPrice()));
    }
}
