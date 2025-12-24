package com.stocktrading.kiwoom.domain.tr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 종목별투자자기관별차트요청 (ka10060) TR 클래스
 * POST /api/dostk/chart
 * 운영서버: https://api.kiwoom.com
 */
@Slf4j
public class InvestorChartTR extends KiwoomTR<InvestorChartTR.Request, InvestorChartTR.Response> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getTrId() {
        return "ka10060";
    }

    @Override
    protected String getApiId() {
        return "ka10060";
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
        return Map.of(
                "dt", request.getDt(),
                "stk_cd", request.getStkCd(),
                "amt_qty_tp", request.getAmtQtyTp(),
                "trde_tp", request.getTrdeTp(),
                "unit_tp", request.getUnitTp()
        );
    }

    @Override
    public Response parseResponse(String response) {
        try {
            log.debug("KA10060 JSON 파싱 시작 - 응답 길이: {} bytes", response != null ? response.length() : 0);
            
            JsonNode root = objectMapper.readTree(response);

            int returnCode = root.path("return_code").asInt();
            String returnMsg = root.path("return_msg").asText();

            log.debug("KA10060 return_code: {}, return_msg: {}", returnCode, returnMsg);

            List<ChartData> chartDataList = new ArrayList<>();
            JsonNode chartArray = root.path("stk_invsr_orgn_chart");

            if (chartArray.isArray()) {
                log.debug("KA10060 차트 데이터 배열 크기: {}", chartArray.size());
                
                for (JsonNode node : chartArray) {
                    ChartData data = ChartData.builder()
                            .dt(node.path("dt").asText())
                            .curPrc(parsePrice(node.path("cur_prc").asText()))
                            .predPre(parsePriceDiff(node.path("pred_pre").asText()))
                            .accTrdePrica(parseLong(node.path("acc_trde_prica").asText()))
                            // 투자자별 데이터
                            .indInvsr(parseLong(node.path("ind_invsr").asText()))
                            .frgnrInvsr(parseLong(node.path("frgnr_invsr").asText()))
                            .orgn(parseLong(node.path("orgn").asText()))
                            // 기관 세부 내역
                            .fnncInvt(parseLong(node.path("fnnc_invt").asText()))
                            .insrnc(parseLong(node.path("insrnc").asText()))
                            .invtrt(parseLong(node.path("invtrt").asText()))
                            .etcFnnc(parseLong(node.path("etc_fnnc").asText()))
                            .bank(parseLong(node.path("bank").asText()))
                            .penfndEtc(parseLong(node.path("penfnd_etc").asText()))
                            .samoFund(parseLong(node.path("samo_fund").asText()))
                            .natn(parseLong(node.path("natn").asText()))
                            .etcCorp(parseLong(node.path("etc_corp").asText()))
                            .natfor(parseLong(node.path("natfor").asText()))
                            .build();
                    chartDataList.add(data);
                }
            } else {
                log.warn("KA10060 stk_invsr_orgn_chart가 배열이 아닙니다");
            }

            log.debug("KA10060 JSON 파싱 완료 - 파싱된 데이터 수: {}", chartDataList.size());

            return Response.builder()
                    .returnCode(returnCode)
                    .returnMsg(returnMsg)
                    .chartDataList(chartDataList)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("KA10060 응답 파싱 실패: {}", e.getMessage(), e);
            log.error("파싱 실패한 응답 내용: {}", response != null && response.length() > 500 
                    ? response.substring(0, 500) + "..." : response);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return "종목별투자자기관별차트요청";
    }

    /**
     * 부호가 포함된 숫자 파싱 - cur_prc용
     * - "+" 기호 제거: "+61300" → 61300
     * - "-" 단독인 경우 0 반환: "-" → 0 (데이터 없음)
     * - "-" 뒤에 숫자가 있으면 절대값: "-61300" → 61300
     */
    private Long parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        
        // "-" 단독인 경우 (데이터 없음)
        if ("-".equals(value.trim())) {
            return 0L;
        }
        
        try {
            // +, - 기호 모두 제거
            String cleaned = value.replace("+", "").replace("-", "").trim();
            if (cleaned.isEmpty()) {
                return 0L;
            }
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("cur_prc 파싱 실패: value={}", value);
            return 0L;
        }
    }

    /**
     * 부호가 포함된 숫자 파싱 - pred_pre용
     * - "+" 기호만 제거: "+4000" → 4000
     * - "-" 기호는 유지: "-1584" → -1584
     * - "-" 단독인 경우 0 반환: "-" → 0 (데이터 없음)
     */
    private Long parsePriceDiff(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        
        // "-" 단독인 경우 (데이터 없음)
        if ("-".equals(value.trim())) {
            return 0L;
        }
        
        try {
            // + 기호만 제거, - 기호는 유지
            String cleaned = value.replace("+", "").trim();
            if (cleaned.isEmpty()) {
                return 0L;
            }
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("pred_pre 파싱 실패: value={}", value);
            return 0L;
        }
    }

    /**
     * 부호가 포함된 숫자 파싱 (예: "+61300", "-1584") - 기존 메서드 (하위 호환용)
     */
    private Long parseSignedLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace("+", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 요청 데이터
     */
    @Value
    @Builder
    public static class Request {
        String dt;          // 일자 (YYYYMMDD)
        String stkCd;       // 종목코드
        String amtQtyTp;    // 금액수량구분 (1:금액, 2:수량)
        String trdeTp;      // 매매구분 (0:순매수, 1:매수, 2:매도)
        String unitTp;      // 단위구분 (1000:천주, 1:단주)

        public static Request of(String dt, String stkCd) {
            return Request.builder()
                    .dt(dt)
                    .stkCd(stkCd)
                    .amtQtyTp("2")   // 기본값: 수량
                    .trdeTp("0")    // 기본값: 순매수
                    .unitTp("1") // 기본값: 단주
                    .build();
        }
    }

    /**
     * 응답 데이터
     */
    @Value
    @Builder
    public static class Response {
        int returnCode;
        String returnMsg;
        List<ChartData> chartDataList;

        public boolean isSuccess() {
            return returnCode == 0;
        }
    }

    /**
     * 차트 데이터 (일별)
     */
    @Value
    @Builder
    public static class ChartData {
        String dt;              // 일자
        Long curPrc;            // 현재가
        Long predPre;           // 전일대비
        Long accTrdePrica;      // 누적거래대금

        // 투자자별 데이터
        Long indInvsr;          // 개인투자자
        Long frgnrInvsr;        // 외국인투자자
        Long orgn;              // 기관계

        // 기관 세부 내역
        Long fnncInvt;          // 금융투자
        Long insrnc;            // 보험
        Long invtrt;            // 투신
        Long etcFnnc;           // 기타금융
        Long bank;              // 은행
        Long penfndEtc;         // 연기금등
        Long samoFund;          // 사모펀드
        Long natn;              // 국가
        Long etcCorp;           // 기타법인
        Long natfor;            // 내외국인
    }
}
