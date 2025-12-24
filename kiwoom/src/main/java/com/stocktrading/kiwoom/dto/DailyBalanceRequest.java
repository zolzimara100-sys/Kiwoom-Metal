package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일별 잔고 수익률 조회 요청 DTO
 * API ID: ka01690
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBalanceRequest {
    
    /**
     * 조회일자 (YYYYMMDD)
     */
    @JsonProperty("qry_dt")
    private String queryDate;
}
