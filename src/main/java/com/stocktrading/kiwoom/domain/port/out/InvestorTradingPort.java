package com.stocktrading.kiwoom.domain.port.out;

import com.stocktrading.kiwoom.domain.model.InvestorTrading;

import java.time.LocalDate;
import java.util.List;

/**
 * 투자자 거래 데이터 영속성 Port
 */
public interface InvestorTradingPort {

    /**
     * 데이터 저장
     */
    InvestorTrading save(InvestorTrading trading);

    /**
     * 여러 데이터 일괄 저장
     */
    List<InvestorTrading> saveAll(List<InvestorTrading> tradings);

    /**
     * 종목코드와 날짜로 조회
     */
    List<InvestorTrading> findByStockAndDate(String stockCode, LocalDate date);

    /**
     * 종목코드와 기간으로 조회
     */
    List<InvestorTrading> findByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 날짜의 모든 데이터 조회
     */
    List<InvestorTrading> findByDate(LocalDate date);

    /**
     * 최근 데이터 조회 (종목별)
     */
    InvestorTrading findLatestByStock(String stockCode);

    /**
     * 중복 확인
     */
    boolean exists(String stockCode, LocalDate date, String tradeType, String amountQuantityType);
}
