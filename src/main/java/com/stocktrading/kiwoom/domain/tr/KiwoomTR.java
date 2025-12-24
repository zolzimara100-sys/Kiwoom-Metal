package com.stocktrading.kiwoom.domain.tr;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 키움 TR(Transaction Request) 추상 클래스
 * 각 API 호출을 TR 클래스로 모델링
 *
 * @param <REQ> 요청 파라미터 타입
 * @param <RES> 응답 데이터 타입
 */
public abstract class KiwoomTR<REQ, RES> {

    /**
     * TR ID (예: FHKST01010400)
     */
    protected abstract String getTrId();

    /**
     * API ID (예: ka10059)
     */
    protected abstract String getApiId();

    /**
     * HTTP 메소드
     */
    protected abstract HttpMethod getHttpMethod();

    /**
     * 요청 파라미터를 쿼리 스트링으로 변환
     */
    protected abstract Map<String, String> buildQueryParams(REQ request);

    /**
     * 요청 파라미터를 Body로 변환 (POST인 경우)
     */
    protected Map<String, String> buildRequestBody(REQ request) {
        return Map.of();
    }

    /**
     * 응답 문자열을 응답 객체로 파싱
     */
    protected abstract RES parseResponse(String response);

    /**
     * TR 실행 (Template Method Pattern)
     */
    public Mono<RES> execute(REQ request, String token, TRExecutor executor) {
        Map<String, String> params = getHttpMethod() == HttpMethod.GET
                ? buildQueryParams(request)
                : buildRequestBody(request);

        Mono<String> responseMono = switch (getHttpMethod()) {
            case GET -> executor.get(getApiId(), getTrId(), params, token);
            case POST -> executor.post(getApiId(), getTrId(), params, token);
        };

        return responseMono.map(this::parseResponse);
    }

    /**
     * TR 설명 (로깅용)
     */
    public abstract String getDescription();

    /**
     * HTTP 메소드
     */
    public enum HttpMethod {
        GET, POST
    }

    /**
     * TR 실행자 인터페이스 (KiwoomApiPort의 메소드를 래핑)
     */
    public interface TRExecutor {
        Mono<String> get(String apiId, String trId, Map<String, String> queryParams, String token);
        Mono<String> post(String apiId, String trId, Map<String, String> body, String token);
    }
}
