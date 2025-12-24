package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.InvestorTrading;

import java.time.LocalDate;
import java.util.List;

/**
 * 투자자별 거래 데이터 조회 Use Case
 */
public interface QueryInvestorTradingUseCase {

    /**
     * 종목코드와 날짜로 조회
     */
    List<InvestorTrading> queryByStockAndDate(String stockCode, LocalDate date);

    /**
     * 종목코드와 기간으로 조회
     */
    List<InvestorTrading> queryByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 날짜의 모든 데이터 조회
     */
    List<InvestorTrading> queryByDate(LocalDate date);

    /**
     * 최근 데이터 조회 (종목별)
     */
    InvestorTrading queryLatestByStock(String stockCode);
}
