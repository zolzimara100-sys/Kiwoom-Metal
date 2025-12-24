package com.stocktrading.kiwoom.domain.tr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KA10099 - 종목정보리스트 TR
 */
@Slf4j
public class StockListTR extends KiwoomTR<StockListTR.Request, StockListTR.Response> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getTrId() {
        return "ka10099";
    }

    @Override
    protected String getApiId() {
        return "ka10099";
    }

    @Override
    protected HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    protected Map<String, String> buildQueryParams(Request request) {
        return Map.of();
    }

    @Override
    protected Map<String, String> buildRequestBody(Request request) {
        Map<String, String> body = new HashMap<>();
        body.put("mrkt_tp", request.marketType());
        return body;
    }

    @Override
    public Response parseResponse(String response) {
        try {
            log.debug("KA10099 JSON 파싱 시작 - 응답 길이: {} bytes", response != null ? response.length() : 0);

            JsonNode root = objectMapper.readTree(response);

            int returnCode = root.path("return_code").asInt();
            String returnMsg = root.path("return_msg").asText();

            log.debug("KA10099 return_code: {}, return_msg: {}", returnCode, returnMsg);

            List<StockData> stockList = new ArrayList<>();
            JsonNode listArray = root.path("list");

            if (listArray.isArray()) {
                log.debug("KA10099 종목 리스트 크기: {}", listArray.size());

                for (JsonNode node : listArray) {
                    StockData data = StockData.builder()
                            .code(node.path("code").asText())
                            .name(node.path("name").asText())
                            .listCount(parseLong(node.path("listCount").asText()))
                            .auditInfo(node.path("auditInfo").asText())
                            .regDay(node.path("regDay").asText())
                            .lastPrice(parseLong(node.path("lastPrice").asText()))
                            .state(node.path("state").asText())
                            .marketCode(node.path("marketCode").asText())
                            .marketName(node.path("marketName").asText())
                            .upName(node.path("upName").asText())
                            .upSizeName(node.path("upSizeName").asText())
                            .companyClassName(node.path("companyClassName").asText())
                            .orderWarning(node.path("orderWarning").asText())
                            .nxtEnable(node.path("nxtEnable").asText())
                            .build();
                    stockList.add(data);
                }
            } else {
                log.warn("KA10099 list가 배열이 아닙니다");
            }

            log.debug("KA10099 JSON 파싱 완료 - 파싱된 종목 수: {}", stockList.size());

            return Response.builder()
                    .returnCode(returnCode)
                    .returnMsg(returnMsg)
                    .stockList(stockList)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("KA10099 응답 파싱 실패: {}", e.getMessage(), e);
            log.error("파싱 실패한 응답 내용: {}", response != null && response.length() > 500
                    ? response.substring(0, 500) + "..." : response);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "종목정보리스트";
    }

    /**
     * 문자열을 Long으로 파싱
     */
    private Long parseLong(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: value={}", value);
            return 0L;
        }
    }

    /**
     * 요청 DTO
     */
    public record Request(String marketType) {
        public static Request of(String marketType) {
            return new Request(marketType);
        }
    }

    /**
     * 응답 DTO
     */
    @Getter
    @Builder
    public static class Response {
        private final int returnCode;
        private final String returnMsg;
        private final List<StockData> stockList;

        public boolean isSuccess() {
            return returnCode == 0;
        }
    }

    /**
     * 종목 데이터 DTO
     */
    @Getter
    @Builder
    public static class StockData {
        private final String code;
        private final String name;
        private final Long listCount;
        private final String auditInfo;
        private final String regDay;
        private final Long lastPrice;
        private final String state;
        private final String marketCode;
        private final String marketName;
        private final String upName;
        private final String upSizeName;
        private final String companyClassName;
        private final String orderWarning;
        private final String nxtEnable;
    }
}
