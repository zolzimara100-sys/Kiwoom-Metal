package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * StockInvestorChart 복합키
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInvestorChartId implements Serializable {

    private String stkCd; // 종목코드
    private LocalDate dt; // 일자
}
