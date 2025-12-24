package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 투자자별 거래내역(ka10060) API 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorTradingRequest {

    @JsonProperty("dt")
    private String dt;          // 일자 (YYYYMMDD)

    @JsonProperty("stk_cd")
    private String stkCd;       // 종목코드 (예: 005930)

    @JsonProperty("amt_qty_tp")
    private String amtQtyTp;    // 금액수량구분 (1:금액, 2:수량)

    @JsonProperty("trde_tp")
    private String trdeTp;      // 매매구분 (0:순매수, 1:매수, 2:매도)

    @JsonProperty("unit_tp")
    private String unitTp;      // 단위구분 (1:단주, 1000:천주)

    // 내부적으로 사용 (기간 조회용)
    private transient String inqrStrtDt;  // 조회시작일자 (내부 사용)
    private transient String inqrEndDt;   // 조회종료일자 (내부 사용)

    /**
     * 기본값 설정 생성자
     * 순매수(0), 금액(1), 천주(1000) 기본값
     */
    public static InvestorTradingRequest of(String stkCd, String dt) {
        return InvestorTradingRequest.builder()
                .dt(dt)
                .stkCd(stkCd)
                .trdeTp("0")        // 기본값: 순매수
                .amtQtyTp("2")      // 기본값: 수량
                .unitTp("1")     // 기본값: 단주
                .build();
    }

    /**
     * 기간 조회용 생성자
     */
    public static InvestorTradingRequest ofPeriod(String stkCd, String startDt, String endDt) {
        return InvestorTradingRequest.builder()
                .dt(startDt)        // 시작일로 초기화
                .stkCd(stkCd)
                .trdeTp("0")
                .amtQtyTp("1")
                .unitTp("1000")
                .inqrStrtDt(startDt)
                .inqrEndDt(endDt)
                .build();
    }
}
