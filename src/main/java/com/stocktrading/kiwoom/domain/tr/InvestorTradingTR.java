package com.stocktrading.kiwoom.domain.tr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 투자자별 거래내역 TR
 * API ID: ka10059
 * TR ID: FHKST01010400
 */
@RequiredArgsConstructor
public class InvestorTradingTR extends KiwoomTR<InvestorTradingTR.Request, InvestorTradingTR.Response> {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    protected String getTrId() {
        return "FHKST01010400";
    }

    @Override
    protected String getApiId() {
        return "ka10059";
    }

    @Override
    protected HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    @Override
    protected Map<String, String> buildQueryParams(Request request) {
        return Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",  // 시장구분 (J:주식)
                "FID_INPUT_ISCD", request.stockCode(),
                "FID_INPUT_DATE_1", request.startDate().format(DATE_FORMATTER),
                "FID_INPUT_DATE_2", request.endDate().format(DATE_FORMATTER),
                "FID_DIV_CLS_CODE", request.tradeType(),
                "FID_INPUT_PRICE_1", request.amountQuantityType(),
                "FID_RANK_SORT_CLS_CODE", request.unitType()
        );
    }

    @Override
    protected Response parseResponse(String response) {
        try {
            return objectMapper.readValue(response, Response.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse investor trading response", e);
        }
    }

    @Override
    public String getDescription() {
        return "투자자별 거래내역 조회";
    }

    /**
     * 요청 파라미터
     */
    public record Request(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate,
            String tradeType,
            String amountQuantityType,
            String unitType
    ) {}

    /**
     * 응답 데이터 (간소화된 버전)
     */
    public record Response(
            String rtCd,
            String msgCd,
            String msg1,
            Object output
    ) {}
}
