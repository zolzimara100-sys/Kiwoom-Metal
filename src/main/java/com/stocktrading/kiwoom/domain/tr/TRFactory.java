package com.stocktrading.kiwoom.domain.tr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TR Factory
 * TR 인스턴스 생성을 담당
 */
@Component
@RequiredArgsConstructor
public class TRFactory {

    private final ObjectMapper objectMapper;

    /**
     * 투자자별 거래내역 TR 생성
     */
    public InvestorTradingTR createInvestorTradingTR() {
        return new InvestorTradingTR(objectMapper);
    }

    /**
     * 주식 현재가 TR 생성
     */
    public StockPriceTR createStockPriceTR() {
        return new StockPriceTR(objectMapper);
    }

    /**
     * TR 타입에 따라 생성
     */
    public <REQ, RES> KiwoomTR<REQ, RES> create(TRType type) {
        return switch (type) {
            case INVESTOR_TRADING -> (KiwoomTR<REQ, RES>) createInvestorTradingTR();
            case STOCK_PRICE -> (KiwoomTR<REQ, RES>) createStockPriceTR();
        };
    }

    /**
     * TR 타입 열거형
     */
    public enum TRType {
        INVESTOR_TRADING("투자자별 거래내역"),
        STOCK_PRICE("주식 현재가");

        private final String description;

        TRType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
