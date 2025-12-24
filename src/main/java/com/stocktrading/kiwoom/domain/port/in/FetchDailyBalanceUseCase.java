package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.DailyBalance;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 일별 잔고 조회 Use Case
 */
public interface FetchDailyBalanceUseCase {

    /**
     * 일별 잔고 조회
     */
    Mono<DailyBalance> fetchDailyBalance(FetchDailyBalanceCommand command);

    /**
     * 일별 잔고 조회 (페이지네이션)
     */
    Mono<DailyBalance> fetchDailyBalanceWithContinuation(
            FetchDailyBalanceCommand command,
            String continuationKey
    );

    /**
     * 커맨드 객체
     */
    record FetchDailyBalanceCommand(
            LocalDate date,
            String accountNumber,
            String accountProductCode
    ) {
        public FetchDailyBalanceCommand {
            if (date == null) {
                throw new IllegalArgumentException("Date is required");
            }
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("Account number is required");
            }
        }
    }
}
