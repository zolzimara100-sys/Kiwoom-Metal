package com.stocktrading.kiwoom.application.service;

import com.stocktrading.kiwoom.domain.model.InvestorTrading;
import com.stocktrading.kiwoom.domain.port.in.FetchInvestorTradingUseCase;
import com.stocktrading.kiwoom.domain.port.in.QueryInvestorTradingUseCase;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.InvestorTradingPort;
import com.stocktrading.kiwoom.domain.port.out.KiwoomApiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 투자자 거래 Application Service (Use Case 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorTradingApplicationService implements FetchInvestorTradingUseCase, QueryInvestorTradingUseCase {

    private final InvestorTradingPort tradingPort;
    private final KiwoomApiPort apiPort;
    private final AuthPort authPort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String API_ID = "ka10059";
    private static final String TRADE_ID = "FHKST01010400";

    @Override
    @Transactional
    public Flux<InvestorTrading> fetchByStock(FetchInvestorTradingCommand command) {
        log.info("투자자 거래 데이터 수집 시작 - 종목: {}, 기간: {} ~ {}",
                command.stockCode(), command.startDate(), command.endDate());

        return authPort.getToken()
                .flatMap(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return Mono.error(new IllegalStateException("Token not found"));
                    }
                    String token = tokenOpt.get().getAccessToken();
                    return callInvestorTradingApi(command, token);
                })
                .flatMapMany(response -> parseAndSaveResponse(response, command))
                .doOnComplete(() -> log.info("투자자 거래 데이터 수집 완료"))
                .doOnError(error -> log.error("투자자 거래 데이터 수집 실패: {}", error.getMessage()));
    }

    @Override
    public Flux<InvestorTrading> fetchRecent(String stockCode) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        FetchInvestorTradingCommand command = new FetchInvestorTradingCommand(
                stockCode,
                startDate,
                endDate,
                InvestorTrading.TradeType.NET_BUY,
                InvestorTrading.AmountQuantityType.AMOUNT,
                InvestorTrading.UnitType.SINGLE
        );

        return fetchByStock(command);
    }

    @Override
    public List<InvestorTrading> queryByStockAndDate(String stockCode, LocalDate date) {
        return tradingPort.findByStockAndDate(stockCode, date);
    }

    @Override
    public List<InvestorTrading> queryByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate) {
        return tradingPort.findByStockAndPeriod(stockCode, startDate, endDate);
    }

    @Override
    public List<InvestorTrading> queryByDate(LocalDate date) {
        return tradingPort.findByDate(date);
    }

    @Override
    public InvestorTrading queryLatestByStock(String stockCode) {
        return tradingPort.findLatestByStock(stockCode);
    }

    /**
     * 키움 API 호출
     */
    private Mono<String> callInvestorTradingApi(FetchInvestorTradingCommand command, String token) {
        Map<String, String> queryParams = Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",
                "FID_INPUT_ISCD", command.stockCode(),
                "FID_INPUT_DATE_1", command.startDate().format(DATE_FORMATTER),
                "FID_INPUT_DATE_2", command.endDate().format(DATE_FORMATTER),
                "FID_DIV_CLS_CODE", command.tradeType().getCode(),
                "FID_INPUT_PRICE_1", command.amountQuantityType().getCode(),
                "FID_RANK_SORT_CLS_CODE", command.unitType().getCode()
        );

        return apiPort.get(API_ID, TRADE_ID, queryParams, token);
    }

    /**
     * API 응답 파싱 및 저장
     */
    private Flux<InvestorTrading> parseAndSaveResponse(String response, FetchInvestorTradingCommand command) {
        // TODO: JSON 파싱 및 도메인 모델로 변환
        // TODO: 중복 체크 후 저장
        // 현재는 간단한 구현만
        return Flux.empty();
    }
}
