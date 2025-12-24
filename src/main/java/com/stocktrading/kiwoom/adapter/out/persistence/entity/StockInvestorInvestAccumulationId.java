package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StockInvestorInvestAccumulationEntity 복합키
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInvestorInvestAccumulationId implements Serializable {

    private String stkCd; // 종목코드
    private LocalDate dt; // 일자
}
