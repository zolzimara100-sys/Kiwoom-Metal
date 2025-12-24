package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 투자자별 거래내역 복합키 클래스
 * Primary Key: stk_cd + dt + trde_tp + amt_qty_tp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInvestorDailyId implements Serializable {

    private String stkCd;       // 종목코드
    private LocalDate dt;       // 일자
    private String trdeTp;      // 매매구분 (0:순매수, 1:매수, 2:매도)
    private String amtQtyTp;    // 금액수량구분 (1:금액, 2:수량)
}
