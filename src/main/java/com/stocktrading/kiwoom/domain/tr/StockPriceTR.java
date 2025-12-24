package com.stocktrading.kiwoom.domain.tr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 주식 현재가 조회 TR
 * API ID: stockprice
 * TR ID: FHKST01010100
 */
@RequiredArgsConstructor
public class StockPriceTR extends KiwoomTR<StockPriceTR.Request, StockPriceTR.Response> {

    private final ObjectMapper objectMapper;

    @Override
    protected String getTrId() {
        return "FHKST01010100";
    }

    @Override
    protected String getApiId() {
        return "stockprice";
    }

    @Override
    protected HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    @Override
    protected Map<String, String> buildQueryParams(Request request) {
        return Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",  // 시장구분 (J:주식)
                "FID_INPUT_ISCD", request.stockCode()
        );
    }

    @Override
    protected Response parseResponse(String response) {
        try {
            return objectMapper.readValue(response, Response.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse stock price response", e);
        }
    }

    @Override
    public String getDescription() {
        return "주식 현재가 조회";
    }

    /**
     * 요청 파라미터
     */
    public record Request(String stockCode) {}

    /**
     * 응답 데이터
     */
    public record Response(
            String rtCd,
            String msgCd,
            String msg1,
            Output output
    ) {
        public record Output(
                String stckPrpr,      // 현재가
                String prdyVrss,      // 전일대비
                String prdyCtrt,      // 등락율
                String acmlVol,       // 누적거래량
                String acmlTrPbmn     // 누적거래대금
        ) {}
    }
}
