package com.stocktrading.kiwoom.adapter.in.websocket;

import com.stocktrading.kiwoom.domain.port.out.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * 실시간 주가 WebSocket 핸들러
 * 키움 WebSocket으로부터 실시간 데이터를 수신하고 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealTimeStockPriceHandler {

    private final CachePort cachePort;
    private final ReactiveStockPriceProcessor processor;

    private Flux<Map<String, Object>> latestDataFlux;

    /**
     * WebSocket 연결 및 실시간 데이터 스트림 시작
     */
    public Flux<Map<String, Object>> connectAndStream(String websocketUrl) {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        return Flux.create(sink -> {
            client.execute(
                    URI.create(websocketUrl),
                    session -> handleWebSocketSession(session, sink::next)
            ).subscribe();
        });
    }

    /**
     * WebSocket 세션 처리
     */
    private Mono<Void> handleWebSocketSession(WebSocketSession session, java.util.function.Consumer<Map<String, Object>> dataSink) {
        log.info("WebSocket 연결 성공");

        // 수신 메시지 처리
        Flux<Map<String, Object>> messageFlux = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> log.debug("실시간 데이터 수신: {}", message))
                .flatMap(this::parseStockPriceMessage)
                .flatMap(priceData -> processor.process(priceData).thenReturn(priceData))
                .doOnNext(dataSink)
                .doOnError(error -> log.error("WebSocket 메시지 처리 오류: {}", error.getMessage()));

        // 송신 메시지 (구독 요청 등)
        Mono<Void> sendSubscription = session.send(
                Flux.just(session.textMessage("{\"action\":\"subscribe\",\"stocks\":[\"005930\"]}"))
        );

        return sendSubscription.thenMany(messageFlux).then();
    }

    /**
     * 메시지 파싱
     */
    private Mono<Map<String, Object>> parseStockPriceMessage(String message) {
        try {
            // TODO: 실제 메시지 파싱 로직
            return Mono.just(Map.of(
                    "stock_code", "005930",
                    "price", 50000,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("메시지 파싱 실패: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
