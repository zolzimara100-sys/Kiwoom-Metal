package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.InvestorTrading;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 투자자별 거래 데이터 수집 Use Case
 */
public interface FetchInvestorTradingUseCase {

    /**
     * 특정 종목의 투자자 거래 데이터 수집
     */
    Flux<InvestorTrading> fetchByStock(FetchInvestorTradingCommand command);

    /**
     * 최근 30일 데이터 수집
     */
    Flux<InvestorTrading> fetchRecent(String stockCode);

    /**
     * 커맨드 객체
     */
    record FetchInvestorTradingCommand(
            String stockCode,
            LocalDate startDate,
            LocalDate endDate,
            InvestorTrading.TradeType tradeType,
            InvestorTrading.AmountQuantityType amountQuantityType,
            InvestorTrading.UnitType unitType
    ) {
        public FetchInvestorTradingCommand {
            if (stockCode == null || stockCode.isBlank()) {
                throw new IllegalArgumentException("Stock code is required");
            }
            if (startDate == null) {
                throw new IllegalArgumentException("Start date is required");
            }
            if (endDate == null) {
                throw new IllegalArgumentException("End date is required");
            }
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before end date");
            }
        }
    }
}
