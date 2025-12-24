
package com.stocktrading.kiwoom.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChart;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorChartRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorMaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorMaRepository;
import com.stocktrading.kiwoom.service.InvestorSupplyDemandService;

/**
 * 통계 분석 API Controller
 * - 투자자별 이동평균 계산 등 통계 배치 작업을 웹에서 실행
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class StatisticsController {

    private final StockInvestorMaRepository stockInvestorMaRepository;
    private final StockInvestorChartRepository stockInvestorChartRepository;
    private final com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorCorrDailyRepository stockInvestorCorrDailyRepository;
    private final InvestorSupplyDemandService investorSupplyDemandService;

    // Python 스크립트 경로 (상대 경로로 프로젝트 루트 기준)
    private static final String PYTHON_SCRIPT_MA_PATH = "python-analysis/calculate_incremental_ma.py";
    private static final String PYTHON_SCRIPT_CORR_PATH = "python-analysis/calculate_investor_correlation.py";
    private static final String PYTHON_COMMAND = "python3";
    private static final int TIMEOUT_MINUTES = 10;

    /**
     * 투자자 이동평균 계산 실행 (증분 업데이트 방식)
     * POST /api/statistics/moving-average/calculate
     */
    @PostMapping("/moving-average/calculate")
    public ResponseEntity<MovingAverageResponse> calculateMovingAverage() {
        log.info("투자자 이동평균 계산 API 호출 (증분 업데이트)");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Python 스크립트 실행
            ProcessBuilder processBuilder = new ProcessBuilder(
                    PYTHON_COMMAND,
                    PYTHON_SCRIPT_MA_PATH);

            // 작업 디렉토리 설정 (Docker 컨테이너 내부에서는 /app)
            processBuilder.directory(new java.io.File("."));
            processBuilder.redirectErrorStream(true);

            log.info("Python 스크립트 실행: {} {}", PYTHON_COMMAND, PYTHON_SCRIPT_MA_PATH);
            Process process = processBuilder.start();
            log.debug("프로세스 명령: {}", String.join(" ", processBuilder.command()));

            // 타임아웃 설정
            boolean finished = process.waitFor(TIMEOUT_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("타임아웃: " + TIMEOUT_MINUTES + "분 초과");
            }

            int exitCode = process.exitValue();

            // 출력 로그 읽기
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            if (exitCode != 0) {
                throw new RuntimeException("Python 스크립트 실행 실패 (exit code: " + exitCode + ")");
            }

            // 처리 결과 조회 (MA 테이블 총 건수)
            long totalRows = stockInvestorMaRepository.count();

            Duration elapsed = Duration.between(startTime, LocalDateTime.now());
            log.info("이동평균 계산 완료 - 소요시간: {}초, 총 {}건", elapsed.getSeconds(), totalRows);

            return ResponseEntity.ok(
                    MovingAverageResponse.builder()
                            .success(true)
                            .message("이동평균 계산 완료 (증분 업데이트)")
                            .rowCount((int) totalRows)
                            .elapsedSeconds(elapsed.getSeconds())
                            .build());

        } catch (Exception e) {
            log.error("이동평균 계산 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    MovingAverageResponse.builder()
                            .success(false)
                            .message("API 오류: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 특정 종목 이동평균 계산 실행
     * POST /api/statistics/moving-average/calculate/{stkCd}
     */
    @PostMapping("/moving-average/calculate/{stkCd}")
    public ResponseEntity<MovingAverageResponse> calculateMovingAverageByStock(@PathVariable String stkCd) {
        log.info("투자자 이동평균 계산 (단일 종목): {}", stkCd);
        ResponseEntity<MovingAverageResponse> execRes = executePythonScript(PYTHON_SCRIPT_MA_PATH, stkCd, "이동평균 계산 완료");

        // If execution failed, return immediately
        if (!execRes.getStatusCode().is2xxSuccessful() || execRes.getBody() == null || !execRes.getBody().success()) {
            String msg = execRes.getBody() != null ? execRes.getBody().message() : "이동평균 계산 실패";
            return ResponseEntity.status(execRes.getStatusCode()).body(MovingAverageResponse.builder()
                    .success(false)
                    .message(msg)
                    .elapsedSeconds(execRes.getBody() != null ? execRes.getBody().elapsedSeconds() : null)
                    .build());
        }

        // Reverting to original state
        return ResponseEntity.ok(MovingAverageResponse.builder()
                .success(true)
                .message("이동평균 계산 완료")
                .elapsedSeconds(execRes.getBody().elapsedSeconds())
                .build());
    }

    /**
     * 특정 종목 상관분석 계산 실행
     * POST /api/statistics/correlation/calculate/{stkCd}
     */
    @PostMapping("/correlation/calculate/{stkCd}")
    public ResponseEntity<MovingAverageResponse> calculateCorrelationByStock(@PathVariable String stkCd) {
        log.info("투자자 상관분석 계산 (단일 종목): {}", stkCd);
        ResponseEntity<MovingAverageResponse> execRes = executePythonScript(PYTHON_SCRIPT_CORR_PATH, stkCd,
                "상관분석 계산 완료");

        if (!execRes.getStatusCode().is2xxSuccessful() || execRes.getBody() == null || !execRes.getBody().success()) {
            String msg = execRes.getBody() != null ? execRes.getBody().message() : "상관분석 계산 실패";
            return ResponseEntity.status(execRes.getStatusCode()).body(MovingAverageResponse.builder()
                    .success(false)
                    .message(msg)
                    .elapsedSeconds(execRes.getBody() != null ? execRes.getBody().elapsedSeconds() : null)
                    .build());
        }

        // Reverting to original state
        return ResponseEntity.ok(MovingAverageResponse.builder()
                .success(true)
                .message("상관분석 계산 완료")
                .elapsedSeconds(execRes.getBody().elapsedSeconds())
                .build());
    }

    /**
     * 투자자 상관분석 계산 실행 (전체 종목 - 증분 업데이트)
     * POST /api/statistics/correlation/calculate
     */
    @PostMapping("/correlation/calculate")
    public ResponseEntity<MovingAverageResponse> calculateCorrelation() {
        log.info("투자자 상관분석 계산 API 호출 (전체 종목)");
        return executePythonScript(PYTHON_SCRIPT_CORR_PATH, "", "상관분석 계산 완료 (전체)");
    }

    /**
     * 투자자 수급분석 계산 실행 (전체 종목)
     * POST /api/statistics/supply-demand/calculate
     */
    @PostMapping("/supply-demand/calculate")
    public ResponseEntity<MovingAverageResponse> calculateSupplyDemand() {
        log.info("투자자 수급분석 계산 API 호출 (전체 종목)");
        long start = System.currentTimeMillis();
        int count = investorSupplyDemandService.calculateAll();
        long elapsed = (System.currentTimeMillis() - start) / 1000;

        return ResponseEntity.ok(MovingAverageResponse.builder()
                .success(true)
                .message("수급분석 계산 완료 (총 " + count + "개 종목)")
                .rowCount(count)
                .elapsedSeconds(elapsed)
                .build());
    }

    /**
     * 특정 종목 수급분석 계산 실행
     * POST /api/statistics/supply-demand/calculate/{stkCd}
     */
    @PostMapping("/supply-demand/calculate/{stkCd}")
    public ResponseEntity<MovingAverageResponse> calculateSupplyDemandByStock(@PathVariable String stkCd) {
        log.info("투자자 수급분석 계산 (단일 종목): {}", stkCd);
        long start = System.currentTimeMillis();
        try {
            investorSupplyDemandService.calculate(stkCd);
            long elapsed = (System.currentTimeMillis() - start) / 1000;
            return ResponseEntity.ok(MovingAverageResponse.builder()
                    .success(true)
                    .message("수급분석 계산 완료")
                    .elapsedSeconds(elapsed)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(MovingAverageResponse.builder()
                    .success(false)
                    .message("수급분석 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Python 스크립트 실행 공통 메서드
     */
    private ResponseEntity<MovingAverageResponse> executePythonScript(String scriptPath, String arg,
            String successMessage) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            List<String> command = new ArrayList<>();
            command.add(PYTHON_COMMAND);
            command.add(scriptPath);
            if (arg != null && !arg.isEmpty()) {
                command.add(arg);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new java.io.File("."));
            processBuilder.redirectErrorStream(true);

            log.info("Python 실행: {}", String.join(" ", command));
            Process process = processBuilder.start();

            boolean finished = process.waitFor(TIMEOUT_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("타임아웃");
            }

            int exitCode = process.exitValue();

            // 출력 로그 읽기 (stdout+stderr merged)
            StringBuilder outputBuilder = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                    outputBuilder.append(line).append("\n");
                }
            }

            Duration elapsed = Duration.between(startTime, LocalDateTime.now());

            if (exitCode != 0) {
                String out = outputBuilder.toString();
                log.error("Python script exited with code {}: {}", exitCode, out);
                return ResponseEntity.internalServerError().body(MovingAverageResponse.builder()
                        .success(false)
                        .message("스크립트 오류 (exit code: " + exitCode + ")\n" + (out.length() > 0 ? out : "No output"))
                        .elapsedSeconds(elapsed.getSeconds())
                        .build());
            }

            return ResponseEntity.ok(MovingAverageResponse.builder()
                    .success(true)
                    .message(successMessage)
                    .elapsedSeconds(elapsed.getSeconds())
                    .build());

        } catch (Exception e) {
            log.error("스크립트 실행 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(MovingAverageResponse.builder()
                    .success(false)
                    .build());
        }
    }

    /**
     * 종목별 이동평균 차트 데이터 조회
     * GET /api/statistics/moving-average/chart/{stkCd}
     * 
     * @param stkCd      종목코드
     * @param days       조회 일수 (기본값: 120일)
     * @param investors  조회할 투자자 유형 (쉼표 구분, 기본: frgnr,orgn)
     * @param period     이동평균 기간 (5, 10, 20, 60 중 선택, 기본: 5)
     * @param beforeDate 특정 날짜 이전 데이터 조회 (무한 스크롤용, 선택)
     */
    @GetMapping("/moving-average/chart/{stkCd}")
    public ResponseEntity<MaChartResponse> getChartData(
            @PathVariable String stkCd,
            @RequestParam(defaultValue = "120") int days,
            @RequestParam(defaultValue = "frgnr,orgn") String investors,
            @RequestParam(defaultValue = "5") int period,
            @RequestParam(required = false) String beforeDate) {
        log.info("이동평균 차트 데이터 조회 - 종목: {}, 일수: {}, 투자자: {}, 기간: {}일, beforeDate: {}",
                stkCd, days, investors, period, beforeDate);

        try {
            List<StockInvestorMaEntity> entities;

            // beforeDate가 있으면 해당 날짜 이전 데이터 조회
            if (beforeDate != null && !beforeDate.isEmpty()) {
                entities = stockInvestorMaRepository.findByStkCdBeforeDateOrderByDtDesc(stkCd, beforeDate, days);
            } else {
                entities = stockInvestorMaRepository.findRecentByStkCd(stkCd, days);
            }

            if (entities.isEmpty()) {
                return ResponseEntity.ok(MaChartResponse.builder()
                        .stkCd(stkCd)
                        .data(Collections.emptyList())
                        .message("데이터가 없습니다.")
                        .build());
            }

            // 날짜 오름차순으로 정렬
            Collections.reverse(entities);

            // 요청된 투자자 유형에 맞게 데이터 변환
            String[] investorTypes = investors.split(",");
            List<MaChartDataPoint> dataPoints = new ArrayList<>();

            for (StockInvestorMaEntity entity : entities) {
                MaChartDataPoint.MaChartDataPointBuilder pointBuilder = MaChartDataPoint.builder()
                        .dt(entity.getDt())
                        .curPrc(entity.getCurPrc());

                for (String investor : investorTypes) {
                    BigDecimal value = getMaValue(entity, investor.trim(), period);
                    if (value != null) {
                        switch (investor.trim()) {
                            case "frgnr" -> pointBuilder.frgnr(value);
                            case "orgn" -> pointBuilder.orgn(value);
                            case "ind_invsr" -> pointBuilder.indInvsr(value);
                            case "fnnc_invt" -> pointBuilder.fnncInvt(value);
                            case "insrnc" -> pointBuilder.insrnc(value);
                            case "invtrt" -> pointBuilder.invtrt(value);
                            case "etc_fnnc" -> pointBuilder.etcFnnc(value);
                            case "bank" -> pointBuilder.bank(value);
                            case "penfnd_etc" -> pointBuilder.penfndEtc(value);
                            case "samo_fund" -> pointBuilder.samoFund(value);
                            case "natn" -> pointBuilder.natn(value);
                            case "etc_corp" -> pointBuilder.etcCorp(value);
                            case "natfor" -> pointBuilder.natfor(value);
                        }
                    }
                }
                dataPoints.add(pointBuilder.build());
            }

            // DEBUG: 첫 데이터 포인트 샘플링 출력
            if (!dataPoints.isEmpty() && period >= 30) {
                MaChartDataPoint sample = dataPoints.get(0);
                log.info("DEBUG MA{}: dt={}, frgnr={}, orgn={}", period, sample.dt(), sample.frgnr(), sample.orgn());
            }

            return ResponseEntity.ok(MaChartResponse.builder()
                    .stkCd(stkCd)
                    .sector(entities.get(0).getSector())
                    .period(period)
                    .data(dataPoints)
                    .message("조회 성공")
                    .build());

        } catch (Exception e) {
            log.error("차트 데이터 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(MaChartResponse.builder()
                    .stkCd(stkCd)
                    .message("조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 엔티티에서 특정 투자자/기간의 MA 값 추출
     */
    private BigDecimal getMaValue(StockInvestorMaEntity entity, String investor, int period) {
        return switch (investor) {
            case "frgnr" -> switch (period) {
                case 5 -> entity.getFrgnrInvsrMa5();
                case 10 -> entity.getFrgnrInvsrMa10();
                case 20 -> entity.getFrgnrInvsrMa20();
                case 30 -> entity.getFrgnrInvsrMa30();
                case 40 -> entity.getFrgnrInvsrMa40();
                case 50 -> entity.getFrgnrInvsrMa50();
                case 60 -> entity.getFrgnrInvsrMa60();
                case 90 -> entity.getFrgnrInvsrMa90();
                case 120 -> entity.getFrgnrInvsrMa120();
                case 140 -> entity.getFrgnrInvsrMa140();
                default -> null;
            };
            case "orgn" -> switch (period) {
                case 5 -> entity.getOrgnMa5();
                case 10 -> entity.getOrgnMa10();
                case 20 -> entity.getOrgnMa20();
                case 30 -> entity.getOrgnMa30();
                case 40 -> entity.getOrgnMa40();
                case 50 -> entity.getOrgnMa50();
                case 60 -> entity.getOrgnMa60();
                case 90 -> entity.getOrgnMa90();
                case 120 -> entity.getOrgnMa120();
                case 140 -> entity.getOrgnMa140();
                default -> null;
            };
            case "fnnc_invt" -> switch (period) {
                case 5 -> entity.getFnncInvtMa5();
                case 10 -> entity.getFnncInvtMa10();
                case 20 -> entity.getFnncInvtMa20();
                case 30 -> entity.getFnncInvtMa30();
                case 40 -> entity.getFnncInvtMa40();
                case 50 -> entity.getFnncInvtMa50();
                case 60 -> entity.getFnncInvtMa60();
                case 90 -> entity.getFnncInvtMa90();
                case 120 -> entity.getFnncInvtMa120();
                case 140 -> entity.getFnncInvtMa140();
                default -> null;
            };
            case "insrnc" -> switch (period) {
                case 5 -> entity.getInsrncMa5();
                case 10 -> entity.getInsrncMa10();
                case 20 -> entity.getInsrncMa20();
                case 30 -> entity.getInsrncMa30();
                case 40 -> entity.getInsrncMa40();
                case 50 -> entity.getInsrncMa50();
                case 60 -> entity.getInsrncMa60();
                case 90 -> entity.getInsrncMa90();
                case 120 -> entity.getInsrncMa120();
                case 140 -> entity.getInsrncMa140();
                default -> null;
            };
            case "invtrt" -> switch (period) {
                case 5 -> entity.getInvtrtMa5();
                case 10 -> entity.getInvtrtMa10();
                case 20 -> entity.getInvtrtMa20();
                case 30 -> entity.getInvtrtMa30();
                case 40 -> entity.getInvtrtMa40();
                case 50 -> entity.getInvtrtMa50();
                case 60 -> entity.getInvtrtMa60();
                case 90 -> entity.getInvtrtMa90();
                case 120 -> entity.getInvtrtMa120();
                case 140 -> entity.getInvtrtMa140();
                default -> null;
            };
            case "etc_fnnc" -> switch (period) {
                case 5 -> entity.getEtcFnncMa5();
                case 10 -> entity.getEtcFnncMa10();
                case 20 -> entity.getEtcFnncMa20();
                case 30 -> entity.getEtcFnncMa30();
                case 40 -> entity.getEtcFnncMa40();
                case 50 -> entity.getEtcFnncMa50();
                case 60 -> entity.getEtcFnncMa60();
                case 90 -> entity.getEtcFnncMa90();
                case 120 -> entity.getEtcFnncMa120();
                case 140 -> entity.getEtcFnncMa140();
                default -> null;
            };
            case "bank" -> switch (period) {
                case 5 -> entity.getBankMa5();
                case 10 -> entity.getBankMa10();
                case 20 -> entity.getBankMa20();
                case 30 -> entity.getBankMa30();
                case 40 -> entity.getBankMa40();
                case 50 -> entity.getBankMa50();
                case 60 -> entity.getBankMa60();
                case 90 -> entity.getBankMa90();
                case 120 -> entity.getBankMa120();
                case 140 -> entity.getBankMa140();
                default -> null;
            };
            case "penfnd_etc" -> switch (period) {
                case 5 -> entity.getPenfndEtcMa5();
                case 10 -> entity.getPenfndEtcMa10();
                case 20 -> entity.getPenfndEtcMa20();
                case 30 -> entity.getPenfndEtcMa30();
                case 40 -> entity.getPenfndEtcMa40();
                case 50 -> entity.getPenfndEtcMa50();
                case 60 -> entity.getPenfndEtcMa60();
                case 90 -> entity.getPenfndEtcMa90();
                case 120 -> entity.getPenfndEtcMa120();
                case 140 -> entity.getPenfndEtcMa140();
                default -> null;
            };
            case "samo_fund" -> switch (period) {
                case 5 -> entity.getSamoFundMa5();
                case 10 -> entity.getSamoFundMa10();
                case 20 -> entity.getSamoFundMa20();
                case 30 -> entity.getSamoFundMa30();
                case 40 -> entity.getSamoFundMa40();
                case 50 -> entity.getSamoFundMa50();
                case 60 -> entity.getSamoFundMa60();
                case 90 -> entity.getSamoFundMa90();
                case 120 -> entity.getSamoFundMa120();
                case 140 -> entity.getSamoFundMa140();
                default -> null;
            };
            case "natn" -> switch (period) {
                case 5 -> entity.getNatnMa5();
                case 10 -> entity.getNatnMa10();
                case 20 -> entity.getNatnMa20();
                case 30 -> entity.getNatnMa30();
                case 40 -> entity.getNatnMa40();
                case 50 -> entity.getNatnMa50();
                case 60 -> entity.getNatnMa60();
                case 90 -> entity.getNatnMa90();
                case 120 -> entity.getNatnMa120();
                case 140 -> entity.getNatnMa140();
                default -> null;
            };
            case "etc_corp" -> switch (period) {
                case 5 -> entity.getEtcCorpMa5();
                case 10 -> entity.getEtcCorpMa10();
                case 20 -> entity.getEtcCorpMa20();
                case 30 -> entity.getEtcCorpMa30();
                case 40 -> entity.getEtcCorpMa40();
                case 50 -> entity.getEtcCorpMa50();
                case 60 -> entity.getEtcCorpMa60();
                case 90 -> entity.getEtcCorpMa90();
                case 120 -> entity.getEtcCorpMa120();
                case 140 -> entity.getEtcCorpMa140();
                default -> null;
            };
            case "natfor" -> switch (period) {
                case 5 -> entity.getNatforMa5();
                case 10 -> entity.getNatforMa10();
                case 20 -> entity.getNatforMa20();
                case 30 -> entity.getNatforMa30();
                case 40 -> entity.getNatforMa40();
                case 50 -> entity.getNatforMa50();
                case 60 -> entity.getNatforMa60();
                case 90 -> entity.getNatforMa90();
                case 120 -> entity.getNatforMa120();
                case 140 -> entity.getNatforMa140();
                default -> null;
            };
            case "ind_invsr" -> switch (period) {
                case 5 -> entity.getIndInvsrMa5();
                case 10 -> entity.getIndInvsrMa10();
                case 20 -> entity.getIndInvsrMa20();
                case 30 -> entity.getIndInvsrMa30();
                case 40 -> entity.getIndInvsrMa40();
                case 50 -> entity.getIndInvsrMa50();
                case 60 -> entity.getIndInvsrMa60();
                case 90 -> entity.getIndInvsrMa90();
                case 120 -> entity.getIndInvsrMa120();
                case 140 -> entity.getIndInvsrMa140();
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * 이동평균 계산 응답 DTO
     */
    @Builder
    public record MovingAverageResponse(
            boolean success,
            String message,
            Integer rowCount,
            Long elapsedSeconds,
            String updatedMaxDate) {
    }

    /**
     * 차트 데이터 응답 DTO
     */
    @Builder
    public record MaChartResponse(
            String stkCd,
            String sector,
            Integer period,
            List<MaChartDataPoint> data,
            String message) {
    }

    /**
     * 차트 데이터 포인트 DTO
     */
    @Builder
    public record MaChartDataPoint(
            String dt,
            Long curPrc,
            BigDecimal frgnr,
            BigDecimal orgn,
            BigDecimal fnncInvt,
            BigDecimal insrnc,
            BigDecimal invtrt,
            BigDecimal etcFnnc,
            BigDecimal bank,
            BigDecimal penfndEtc,
            BigDecimal samoFund,
            BigDecimal natn,
            BigDecimal etcCorp,
            BigDecimal natfor,
            BigDecimal indInvsr) {
    }

    /**
     * 투자자별 거래 비중 계산 API
     * POST /api/statistics/investor-ratio
     */
    /**
     * 투자자별 거래 비중 계산 API (REQ-006)
     * GET /api/statistics/investor-ratio/{stkCd}
     * - 최근 1년간의 데이터를 실시간으로 조회하여 비중 계산
     */
    @GetMapping("/investor-ratio/{stkCd}")
    public ResponseEntity<InvestorRatioResponse> getInvestorRatio(@PathVariable String stkCd) {
        log.info("투자자별 거래 비중 조회 API 호출: 종목코드={}", stkCd);

        try {
            // 1. 1년 범위 계산
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(1);

            // 2. 데이터 조회 (tb_stock_investor_chart)
            List<StockInvestorChart> data = stockInvestorChartRepository
                    .findByStkCdAndDtBetweenOrderByDtAsc(stkCd, startDate, endDate);

            if (data.isEmpty()) {
                return ResponseEntity.ok(InvestorRatioResponse.builder()
                        .stkCd(stkCd)
                        .dataCount(0)
                        .message("데이터가 없습니다.")
                        .build());
            }

            // 3. 투자자별 거래규모 계산: SUM(|curPrc * 순매수량|)
            Map<String, BigDecimal> volumeMap = new HashMap<>();
            String[] investors = { "frgnr", "orgn", "ind", "fnncInvt", "insrnc", "invtrt", "bank",
                    "etcFnnc", "penfndEtc", "samoFund", "etcCorp", "natn", "natfor" };

            for (String inv : investors) {
                volumeMap.put(inv, BigDecimal.ZERO);
            }

            for (StockInvestorChart row : data) {
                BigDecimal curPrc = new BigDecimal(row.getCurPrc());

                volumeMap.put("frgnr",
                        volumeMap.get("frgnr").add(curPrc.multiply(BigDecimal.valueOf(row.getFrgnrInvsr())).abs()));
                volumeMap.put("orgn",
                        volumeMap.get("orgn").add(curPrc.multiply(BigDecimal.valueOf(row.getOrgn())).abs()));
                volumeMap.put("ind",
                        volumeMap.get("ind").add(curPrc.multiply(BigDecimal.valueOf(row.getIndInvsr())).abs()));
                volumeMap.put("fnncInvt",
                        volumeMap.get("fnncInvt").add(curPrc.multiply(BigDecimal.valueOf(row.getFnncInvt())).abs()));
                volumeMap.put("insrnc",
                        volumeMap.get("insrnc").add(curPrc.multiply(BigDecimal.valueOf(row.getInsrnc())).abs()));
                volumeMap.put("invtrt",
                        volumeMap.get("invtrt").add(curPrc.multiply(BigDecimal.valueOf(row.getInvtrt())).abs()));
                volumeMap.put("bank",
                        volumeMap.get("bank").add(curPrc.multiply(BigDecimal.valueOf(row.getBank())).abs()));
                volumeMap.put("etcFnnc",
                        volumeMap.get("etcFnnc").add(curPrc.multiply(BigDecimal.valueOf(row.getEtcFnnc())).abs()));
                volumeMap.put("penfndEtc",
                        volumeMap.get("penfndEtc").add(curPrc.multiply(BigDecimal.valueOf(row.getPenfndEtc())).abs()));
                volumeMap.put("samoFund",
                        volumeMap.get("samoFund").add(curPrc.multiply(BigDecimal.valueOf(row.getSamoFund())).abs()));
                volumeMap.put("etcCorp",
                        volumeMap.get("etcCorp").add(curPrc.multiply(BigDecimal.valueOf(row.getEtcCorp())).abs()));
                volumeMap.put("natn",
                        volumeMap.get("natn").add(curPrc.multiply(BigDecimal.valueOf(row.getNatn())).abs()));
                volumeMap.put("natfor",
                        volumeMap.get("natfor").add(curPrc.multiply(BigDecimal.valueOf(row.getNatfor())).abs()));
            }

            // 4. 전체 거래규모 합계
            BigDecimal totalVolume = volumeMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5. 비중 계산 (소수점 첫째자리)
            Map<String, BigDecimal> ratioMap = new HashMap<>();
            if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<String, BigDecimal> entry : volumeMap.entrySet()) {
                    BigDecimal ratio = entry.getValue()
                            .divide(totalVolume, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                    ratioMap.put(entry.getKey(), ratio);
                }
            } else {
                for (String inv : investors) {
                    ratioMap.put(inv, BigDecimal.ZERO);
                }
            }

            // 6. 응답 생성
            return ResponseEntity.ok(InvestorRatioResponse.builder()
                    .stkCd(stkCd)
                    .dataCount(data.size())
                    .frgnr(ratioMap.get("frgnr"))
                    .orgn(ratioMap.get("orgn"))
                    .ind(ratioMap.get("ind"))
                    .fnncInvt(ratioMap.get("fnncInvt"))
                    .insrnc(ratioMap.get("insrnc"))
                    .invtrt(ratioMap.get("invtrt"))
                    .bank(ratioMap.get("bank"))
                    .etcFnnc(ratioMap.get("etcFnnc"))
                    .penfndEtc(ratioMap.get("penfndEtc"))
                    .samoFund(ratioMap.get("samoFund"))
                    .etcCorp(ratioMap.get("etcCorp"))
                    .natn(ratioMap.get("natn"))
                    .natfor(ratioMap.get("natfor"))
                    .message("success")
                    .build());

        } catch (Exception e) {
            log.error("투자자별 거래 비중 계산 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(InvestorRatioResponse.builder()
                    .stkCd(stkCd)
                    .message("서버 오류: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 투자자 거래 비중 응답 DTO
     */
    @Builder
    public record InvestorRatioResponse(
            String stkCd,
            Integer dataCount,
            BigDecimal frgnr,
            BigDecimal orgn,
            BigDecimal ind,
            BigDecimal fnncInvt,
            BigDecimal insrnc,
            BigDecimal invtrt,
            BigDecimal bank,
            BigDecimal etcFnnc,
            BigDecimal penfndEtc,
            BigDecimal samoFund,
            BigDecimal natn,
            BigDecimal etcCorp,
            BigDecimal natfor,
            String message) {
    }

    /**
     * 순매수 이동평균 페이지용 투자자 비중 계산 API (최초 로직)
     * GET /api/statistics/investor-ratio-ma/{stkCd}
     * - tb_stock_investor_ma의 이동평균값 기반으로 비중 계산
     * - 선택된 기간(period)에 해당하는 MA 컬럼 사용
     *
     * @param stkCd    종목코드
     * @param period   이동평균 기간 (5, 10, 20, 30, 40, 50, 60, 90, 120, 140)
     * @param fromDate 시작 날짜 (YYYYMMDD, 필수)
     * @param toDate   종료 날짜 (YYYYMMDD, 필수)
     */
    @GetMapping("/investor-ratio-ma/{stkCd}")
    public ResponseEntity<InvestorRatioMaResponse> getInvestorRatioMa(
            @PathVariable String stkCd,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        log.info("투자자별 이동평균 비중 조회: 종목코드={}, 기간=MA{}, from={}, to={}",
                stkCd, period, fromDate, toDate);

        try {
            // 1. 날짜 범위 필수 체크
            if (fromDate == null || toDate == null) {
                return ResponseEntity.badRequest().body(InvestorRatioMaResponse.builder()
                        .stkCd(stkCd)
                        .period(period)
                        .message("fromDate와 toDate는 필수입니다.")
                        .build());
            }

            // 날짜 형식 정규화
            String actualFromDate = fromDate.replace("-", "");
            String actualToDate = toDate.replace("-", "");

            // 2. 데이터 조회 (tb_stock_investor_ma - 이동평균 테이블)
            List<StockInvestorMaEntity> data = stockInvestorMaRepository
                    .findByStkCdAndDtBetween(stkCd, actualFromDate, actualToDate);

            if (data.isEmpty()) {
                return ResponseEntity.ok(InvestorRatioMaResponse.builder()
                        .stkCd(stkCd)
                        .period(period)
                        .fromDate(actualFromDate)
                        .toDate(actualToDate)
                        .dataCount(0)
                        .message("데이터가 없습니다.")
                        .build());
            }

            // 3. 투자자별 이동평균값 abs 합계 계산
            Map<String, BigDecimal> volumeMap = new HashMap<>();
            String[] investors = { "frgnr", "orgn", "fnncInvt", "insrnc", "invtrt", "bank",
                    "etcFnnc", "penfndEtc", "samoFund", "etcCorp", "natn", "natfor" };

            for (String inv : investors) {
                volumeMap.put(inv, BigDecimal.ZERO);
            }

            // 각 row에서 선택된 기간의 MA값 추출하여 누적
            for (StockInvestorMaEntity row : data) {
                BigDecimal frgnrMa = getMaValue(row, "frgnr", period);
                if (frgnrMa != null) {
                    volumeMap.put("frgnr", volumeMap.get("frgnr").add(frgnrMa.abs()));
                }

                BigDecimal orgnMa = getMaValue(row, "orgn", period);
                if (orgnMa != null) {
                    volumeMap.put("orgn", volumeMap.get("orgn").add(orgnMa.abs()));
                }

                BigDecimal fnncInvtMa = getMaValue(row, "fnnc_invt", period);
                if (fnncInvtMa != null) {
                    volumeMap.put("fnncInvt", volumeMap.get("fnncInvt").add(fnncInvtMa.abs()));
                }

                BigDecimal insrncMa = getMaValue(row, "insrnc", period);
                if (insrncMa != null) {
                    volumeMap.put("insrnc", volumeMap.get("insrnc").add(insrncMa.abs()));
                }

                BigDecimal invtrtMa = getMaValue(row, "invtrt", period);
                if (invtrtMa != null) {
                    volumeMap.put("invtrt", volumeMap.get("invtrt").add(invtrtMa.abs()));
                }

                BigDecimal bankMa = getMaValue(row, "bank", period);
                if (bankMa != null) {
                    volumeMap.put("bank", volumeMap.get("bank").add(bankMa.abs()));
                }

                BigDecimal etcFnncMa = getMaValue(row, "etc_fnnc", period);
                if (etcFnncMa != null) {
                    volumeMap.put("etcFnnc", volumeMap.get("etcFnnc").add(etcFnncMa.abs()));
                }

                BigDecimal penfndEtcMa = getMaValue(row, "penfnd_etc", period);
                if (penfndEtcMa != null) {
                    volumeMap.put("penfndEtc", volumeMap.get("penfndEtc").add(penfndEtcMa.abs()));
                }

                BigDecimal samoFundMa = getMaValue(row, "samo_fund", period);
                if (samoFundMa != null) {
                    volumeMap.put("samoFund", volumeMap.get("samoFund").add(samoFundMa.abs()));
                }

                BigDecimal etcCorpMa = getMaValue(row, "etc_corp", period);
                if (etcCorpMa != null) {
                    volumeMap.put("etcCorp", volumeMap.get("etcCorp").add(etcCorpMa.abs()));
                }

                BigDecimal natnMa = getMaValue(row, "natn", period);
                if (natnMa != null) {
                    volumeMap.put("natn", volumeMap.get("natn").add(natnMa.abs()));
                }

                BigDecimal natforMa = getMaValue(row, "natfor", period);
                if (natforMa != null) {
                    volumeMap.put("natfor", volumeMap.get("natfor").add(natforMa.abs()));
                }
            }

            // 4. 전체 합계
            BigDecimal totalVolume = volumeMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5. 비중 계산 (소수점 첫째자리)
            Map<String, BigDecimal> ratioMap = new HashMap<>();
            if (totalVolume.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<String, BigDecimal> entry : volumeMap.entrySet()) {
                    BigDecimal ratio = entry.getValue()
                            .divide(totalVolume, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                    ratioMap.put(entry.getKey(), ratio);
                }
            } else {
                for (String inv : investors) {
                    ratioMap.put(inv, BigDecimal.ZERO);
                }
            }

            log.info("비중 계산 완료: 종목={}, MA{}, {}~{}, 데이터={}건, 외국인={}%",
                    stkCd, period, actualFromDate, actualToDate, data.size(), ratioMap.get("frgnr"));

            // 6. 응답 생성
            return ResponseEntity.ok(InvestorRatioMaResponse.builder()
                    .stkCd(stkCd)
                    .period(period)
                    .fromDate(actualFromDate)
                    .toDate(actualToDate)
                    .dataCount(data.size())
                    .frgnr(ratioMap.get("frgnr"))
                    .orgn(ratioMap.get("orgn"))
                    .fnncInvt(ratioMap.get("fnncInvt"))
                    .insrnc(ratioMap.get("insrnc"))
                    .invtrt(ratioMap.get("invtrt"))
                    .bank(ratioMap.get("bank"))
                    .etcFnnc(ratioMap.get("etcFnnc"))
                    .penfndEtc(ratioMap.get("penfndEtc"))
                    .samoFund(ratioMap.get("samoFund"))
                    .etcCorp(ratioMap.get("etcCorp"))
                    .natn(ratioMap.get("natn"))
                    .natfor(ratioMap.get("natfor"))
                    .message("success")
                    .build());

        } catch (Exception e) {
            log.error("이동평균 비중 계산 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(InvestorRatioMaResponse.builder()
                    .stkCd(stkCd)
                    .period(period)
                    .message("서버 오류: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 날짜에서 N일 전 날짜 계산
     */
    private String calculateDateBefore(String dateStr, int days) {
        try {
            LocalDate date = LocalDate.parse(dateStr,
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate beforeDate = date.minusDays(days);
            return beforeDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("날짜 계산 실패, 기본값 사용: {}", e.getMessage());
            return "19900101";
        }
    }

    /**
     * 이동평균 기반 투자자 비중 응답 DTO
     */
    @Builder
    public record InvestorRatioMaResponse(
            String stkCd,
            Integer period,
            String fromDate,
            String toDate,
            Integer dataCount,
            BigDecimal frgnr,
            BigDecimal orgn,
            BigDecimal fnncInvt,
            BigDecimal insrnc,
            BigDecimal invtrt,
            BigDecimal bank,
            BigDecimal etcFnnc,
            BigDecimal penfndEtc,
            BigDecimal samoFund,
            BigDecimal natn,
            BigDecimal etcCorp,
            BigDecimal natfor,
            String message) {
    }
}
