package com.stocktrading.kiwoom.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 일별 잔고 수익률 조회 응답 DTO
 * API ID: ka01690
 */
@Data
public class DailyBalanceResponse {
    
    /**
     * 조회일자
     */
    @JsonProperty("dt")
    private String date;
    
    /**
     * 총매수금액
     */
    @JsonProperty("tot_buy_amt")
    private String totalBuyAmount;
    
    /**
     * 총평가금액
     */
    @JsonProperty("tot_evlt_amt")
    private String totalEvaluationAmount;
    
    /**
     * 총평가손익
     */
    @JsonProperty("tot_evltv_prft")
    private String totalEvaluationProfit;
    
    /**
     * 총수익률
     */
    @JsonProperty("tot_prft_rt")
    private String totalProfitRate;
    
    /**
     * 예수금잔액
     */
    @JsonProperty("dbst_bal")
    private String depositBalance;
    
    /**
     * 당일주식자산
     */
    @JsonProperty("day_stk_asst")
    private String dayStockAsset;
    
    /**
     * 매수비중
     */
    @JsonProperty("buy_wght")
    private String buyWeight;
    
    /**
     * 일별잔고수익률 목록
     */
    @JsonProperty("day_bal_rt")
    private List<DailyBalanceItem> dailyBalanceList;
    
    /**
     * 응답코드
     */
    @JsonProperty("return_code")
    private Integer returnCode;
    
    /**
     * 응답메시지
     */
    @JsonProperty("return_msg")
    private String returnMessage;
    
    /**
     * 일별잔고수익률 항목
     */
    @Data
    public static class DailyBalanceItem {
        
        /**
         * 현재가
         */
        @JsonProperty("cur_prc")
        private String currentPrice;
        
        /**
         * 종목코드
         */
        @JsonProperty("stk_cd")
        private String stockCode;
        
        /**
         * 종목명
         */
        @JsonProperty("stk_nm")
        private String stockName;
        
        /**
         * 잔고수량
         */
        @JsonProperty("rmnd_qty")
        private String remainQuantity;
        
        /**
         * 매수단가
         */
        @JsonProperty("buy_uv")
        private String buyUnitValue;
        
        /**
         * 매수비중
         */
        @JsonProperty("buy_wght")
        private String buyWeight;
        
        /**
         * 평가손익
         */
        @JsonProperty("evltv_prft")
        private String evaluationProfit;
        
        /**
         * 수익률
         */
        @JsonProperty("prft_rt")
        private String profitRate;
        
        /**
         * 평가금액
         */
        @JsonProperty("evlt_amt")
        private String evaluationAmount;
        
        /**
         * 평가비중
         */
        @JsonProperty("evlt_wght")
        private String evaluationWeight;
    }
}
