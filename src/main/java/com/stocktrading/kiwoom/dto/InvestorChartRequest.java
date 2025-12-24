package com.stocktrading.kiwoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 종목별 투자자 기관별 차트 API 요청 DTO (ka10060)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorChartRequest {

    private String stkCd; // 종목코드 (예: 005930)

    private String dt; // 일자 (YYYYMMDD)

    @Builder.Default
    private String amtQtyTp = "1"; // 금액수량구분 (1:금액, 2:수량)

    @Builder.Default
    private String trdeTp = "0"; // 매매구분 (0:순매수, 1:매수, 2:매도)

    @Builder.Default
    private String unitTp = "1000"; // 단위구분 (1000:천주, 1:단주)

    /**
     * 기본값 설정 생성자
     */
    public static InvestorChartRequest of(String stkCd, String dt) {
        return InvestorChartRequest.builder()
                .stkCd(stkCd)
                .dt(dt)
                .amtQtyTp("2")  // 기본값: 수량
                .trdeTp("0")    // 기본값: 순매수
                .unitTp("1") // 기본값: 단주
                .build();
    }
}
