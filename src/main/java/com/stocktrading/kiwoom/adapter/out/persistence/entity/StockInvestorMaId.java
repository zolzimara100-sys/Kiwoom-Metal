package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 투자자 이동평균 복합키
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockInvestorMaId implements Serializable {
    private String stkCd;
    private String dt;
}
