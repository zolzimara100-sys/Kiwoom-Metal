package com.stocktrading.kiwoom.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.dto.InvestorTradingRequest;
import com.stocktrading.kiwoom.service.InvestorTradingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 투자자별 거래내역(ka10059) 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/kiwoom/investor-trading")
@RequiredArgsConstructor
public class InvestorTradingController {

    private final InvestorTradingService investorTradingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 투자자별 거래내역 조회 및 저장
     *
     * @param stkCd     종목코드 (예: 005930)
     * @param startDate 시작일자 (YYYYMMDD)
     * @param endDate   종료일자 (YYYYMMDD)
     * @param trdeTp    매매구분 (0:순매수, 1:매수, 2:매도) - 기본값: 0
     * @param amtQtyTp  금액수량구분 (1:금액, 2:수량) - 기본값: 1
     * @param unitTp    단위구분 (1:단주, 1000:천주) - 기본값: 1
     */
    @PostMapping("/fetch")
    public Mono<ResponseEntity<List<StockInvestorDaily>>> fetchInvestorTrading(
            @RequestParam String stkCd,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") String trdeTp,
            @RequestParam(defaultValue = "1") String amtQtyTp,
            @RequestParam(defaultValue = "1") String unitTp) {

        log.info("투자자별 거래내역 조회 요청 - 종목: {}, 기간: {} ~ {}, 매매구분: {}, 금액수량구분: {}",
                stkCd, startDate, endDate, trdeTp, amtQtyTp);

        InvestorTradingRequest request = InvestorTradingRequest.builder()
                .stkCd(stkCd)
                .inqrStrtDt(startDate)
                .inqrEndDt(endDate)
                .trdeTp(trdeTp)
                .amtQtyTp(amtQtyTp)
                .unitTp(unitTp)
                .build();

        return investorTradingService.fetchAndSaveInvestorTrading(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("투자자별 거래내역 조회 실패: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * 간편 조회: 최근 30일 데이터 조회 및 저장 (순매수, 금액 기준)
     */
    @PostMapping("/fetch-recent")
    public Mono<ResponseEntity<List<StockInvestorDaily>>> fetchRecentInvestorTrading(
            @RequestParam String stkCd) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        log.info("최근 30일 투자자별 거래내역 조회 - 종목: {}", stkCd);

        InvestorTradingRequest request = InvestorTradingRequest.ofPeriod(
                stkCd,
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER));

        return investorTradingService.fetchAndSaveInvestorTrading(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("최근 투자자별 거래내역 조회 실패: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * DB에서 특정 종목의 특정 일자 데이터 조회
     */
    @GetMapping("/query")
    public ResponseEntity<List<StockInvestorDaily>> getInvestorTrading(
            @RequestParam String stkCd,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("투자자별 거래내역 DB 조회 - 종목: {}, 일자: {}", stkCd, date);

        List<StockInvestorDaily> result = investorTradingService.getInvestorTrading(stkCd, date);
        return ResponseEntity.ok(result);
    }

    /**
     * DB에서 특정 기간의 데이터 조회
     */
    @GetMapping("/query-period")
    public ResponseEntity<List<StockInvestorDaily>> getInvestorTradingByPeriod(
            @RequestParam String stkCd,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("투자자별 거래내역 기간 조회 - 종목: {}, 기간: {} ~ {}", stkCd, startDate, endDate);

        List<StockInvestorDaily> result = investorTradingService.getInvestorTradingByPeriod(stkCd, startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * DB에서 특정 일자의 모든 종목 데이터 조회
     */
    @GetMapping("/query-date")
    public ResponseEntity<List<StockInvestorDaily>> getInvestorTradingByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("투자자별 거래내역 일자별 조회 - 일자: {}", date);

        List<StockInvestorDaily> result = investorTradingService.getInvestorTradingByDate(date);
        return ResponseEntity.ok(result);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("InvestorTrading API is running");
    }
}
