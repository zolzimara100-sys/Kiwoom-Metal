package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorCorrDailyId implements Serializable {
    private String stkCd;
    private String dt;
    private int corrDays;
}
