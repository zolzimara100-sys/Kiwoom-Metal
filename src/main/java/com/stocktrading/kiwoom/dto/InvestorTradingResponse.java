package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투자자별 거래내역(ka10060) API 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorTradingResponse {

    @JsonProperty("return_code")
    private Integer returnCode;        // 응답코드 (0: 정상)

    @JsonProperty("return_msg")
    private String returnMsg;          // 응답 메시지

    @JsonProperty("stk_invsr_orgn_chart")
    private List<InvestorTradingData> output;

    /**
     * 응답 헤더 (연속조회용)
     */
    private ResponseHeader header;

    /**
     * 응답 헤더 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseHeader {
        @JsonProperty("cont-yn")
        private String contYn;      // 연속조회여부 (Y/N)

        @JsonProperty("next-key")
        private String nextKey;     // 연속조회키
    }

    /**
     * 투자자별 거래내역 데이터 (ka10060 API 스펙)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestorTradingData {

        // 시세 정보
        @JsonProperty("dt")
        private String dt;              // 일자

        @JsonProperty("cur_prc")
        private String curPrc;          // 현재가

        @JsonProperty("pre_sig")
        private String preSig;          // 대비기호

        @JsonProperty("pred_pre")
        private String predPre;         // 전일대비

        @JsonProperty("flu_rt")
        private String fluRt;           // 등락율

        @JsonProperty("acc_trde_qty")
        private String accTrdeQty;      // 누적거래량

        @JsonProperty("acc_trde_prica")
        private String accTrdePrica;    // 누적거래대금

        // 투자자별 수치
        @JsonProperty("ind_invsr")
        private String indInvsr;        // 개인투자자

        @JsonProperty("frgnr_invsr")
        private String frgnrInvsr;      // 외국인투자자

        @JsonProperty("orgn")
        private String orgn;            // 기관계

        @JsonProperty("fnnc_invt")
        private String fnncInvt;        // 금융투자

        @JsonProperty("insrnc")
        private String insrnc;          // 보험

        @JsonProperty("invtrt")
        private String invtrt;          // 투신

        @JsonProperty("etc_fnnc")
        private String etcFnnc;         // 기타금융

        @JsonProperty("bank")
        private String bank;            // 은행

        @JsonProperty("penfnd_etc")
        private String penfndEtc;       // 연기금등

        @JsonProperty("samo_fund")
        private String samoFund;        // 사모펀드

        @JsonProperty("natn")
        private String natn;            // 국가

        @JsonProperty("etc_corp")
        private String etcCorp;         // 기타법인

        @JsonProperty("natfor")
        private String natfor;          // 내외국인
    }
}
