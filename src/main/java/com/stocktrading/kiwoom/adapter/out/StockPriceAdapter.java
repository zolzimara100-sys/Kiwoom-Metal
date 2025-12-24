package com.stocktrading.kiwoom.adapter.out;

import com.stocktrading.kiwoom.domain.model.StockPrice;
import com.stocktrading.kiwoom.domain.port.out.StockPricePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * StockPrice Port 구현체 (임시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceAdapter implements StockPricePort {

    @Override
    public Mono<Optional<StockPrice>> findCachedPrice(String stockCode) {
        return Mono.just(Optional.empty());
    }

    @Override
    public Mono<Void> cachePrice(StockPrice stockPrice) {
        return Mono.empty();
    }

    @Override
    public Mono<StockPrice> fetchPriceFromApi(String stockCode, String token) {
        return Mono.empty();
    }
}
