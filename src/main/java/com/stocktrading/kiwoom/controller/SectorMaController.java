package com.stocktrading.kiwoom.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stocktrading.kiwoom.batch.SectorMaBatchService;
import com.stocktrading.kiwoom.service.SectorMaService;
import com.stocktrading.kiwoom.dto.SectorMaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 섹터별 투자자 이동평균 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sector-ma")
@RequiredArgsConstructor
public class SectorMaController {

    private final SectorMaService sectorMaService;
    private final SectorMaBatchService batchService;

    /**
     * 섹터별 이동평균 조회
     * GET /api/v1/sector-ma/{sectorCd}
     *
     * @param sectorCd 섹터 코드
     * @param startDate 시작일 (yyyyMMdd, 옵션)
     * @param endDate 종료일 (yyyyMMdd, 옵션)
     * @param limit 조회 건수 (기본: 120)
     * @return 섹터 이동평균 데이터
     */
    @GetMapping("/{sectorCd}")
    public ResponseEntity<SectorMaResponse> getSectorMovingAverage(
            @PathVariable String sectorCd,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "120") int limit) {

        log.info("섹터 이동평균 조회 요청 - 섹터: {}, 시작일: {}, 종료일: {}, 제한: {}",
            sectorCd, startDate, endDate, limit);

        try {
            SectorMaResponse response;

            if (startDate != null && endDate != null) {
                // 날짜 범위 조회
                response = sectorMaService.getSectorMaByPeriod(sectorCd, startDate, endDate);
            } else {
                // 최근 N일 조회
                response = sectorMaService.getRecentSectorMa(sectorCd, limit);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("섹터 이동평균 조회 실패 - 섹터: {}, 오류: {}", sectorCd, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(SectorMaResponse.error(e.getMessage()));
        }
    }

    /**
     * 전체 섹터 목록 조회
     * GET /api/v1/sector-ma/sectors
     *
     * @return 섹터 코드 및 이름 목록
     */
    @GetMapping("/sectors")
    public ResponseEntity<List<SectorMaResponse.SectorInfo>> getAllSectors() {
        log.info("섹터 목록 조회 요청");

        try {
            List<SectorMaResponse.SectorInfo> sectors = sectorMaService.getAllSectors();
            return ResponseEntity.ok(sectors);

        } catch (Exception e) {
            log.error("섹터 목록 조회 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 섹터에 속한 종목 목록 조회 (REQ-004-1)
     * GET /api/v1/sector-ma/stocks/{sectorCd}
     *
     * @param sectorCd 섹터 코드 (예: ai_infra, semicon 등)
     * @return 종목 코드 및 이름 목록
     */
    @GetMapping("/stocks/{sectorCd}")
    public ResponseEntity<List<SectorMaResponse.StockInfo>> getStocksBySector(@PathVariable String sectorCd) {
        log.info("섹터별 종목 목록 조회 요청 - 섹터: {}", sectorCd);

        try {
            List<SectorMaResponse.StockInfo> stocks = sectorMaService.getStocksBySector(sectorCd);
            return ResponseEntity.ok(stocks);

        } catch (Exception e) {
            log.error("섹터별 종목 목록 조회 실패 - 섹터: {}, 오류: {}", sectorCd, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 섹터별 차트 데이터 조회 (REQ-004)
     * GET /api/v1/sector-ma/chart/{sectorCd}
     *
     * @param sectorCd 섹터 코드 (예: ai_infra, semicon 등)
     * @param days 조회 일수 (기본: 120)
     * @param investors 투자자 유형 (쉼표 구분, 기본: "frgnr,orgn")
     * @param period 이동평균 기간 (5, 10, 20, 30, 40, 50, 60, 90, 120, 140)
     * @param beforeDate 무한 스크롤용 날짜 (YYYYMMDD, 옵션)
     * @return 섹터별 차트 데이터
     */
    @GetMapping("/chart/{sectorCd}")
    public ResponseEntity<SectorMaResponse.SectorMaChartResponse> getChartData(
            @PathVariable String sectorCd,
            @RequestParam(defaultValue = "120") int days,
            @RequestParam(defaultValue = "frgnr,orgn") String investors,
            @RequestParam(defaultValue = "5") int period,
            @RequestParam(required = false) String beforeDate) {

        log.info("섹터 차트 데이터 조회 요청 - 섹터: {}, 일수: {}, 투자자: {}, 기간: {}, 이전날짜: {}",
            sectorCd, days, investors, period, beforeDate);

        try {
            SectorMaResponse.SectorMaChartResponse response = sectorMaService.getChartData(
                sectorCd, days, investors, period, beforeDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("섹터 차트 데이터 조회 실패 - 섹터: {}, 오류: {}", sectorCd, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(SectorMaResponse.SectorMaChartResponse.builder()
                    .sectorCd(sectorCd)
                    .message("조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 모든 섹터 차트 데이터 조회 (REQ-005)
     * GET /api/v1/sector-ma/chart/all
     *
     * @param days 조회 일수 (기본: 120)
     * @param investors 투자자 유형 (쉼표 구분, 기본: "frgnr,orgn")
     * @param period 이동평균 기간 (5, 10, 20, 30, 40, 50, 60, 90, 120, 140)
     * @param beforeDate 무한 스크롤용 날짜 (YYYYMMDD, 옵션)
     * @return 모든 섹터의 차트 데이터
     */
    @GetMapping("/chart/all")
    public ResponseEntity<SectorMaResponse.AllSectorMaChartResponse> getAllSectorsChartData(
            @RequestParam(defaultValue = "120") int days,
            @RequestParam(defaultValue = "frgnr,orgn") String investors,
            @RequestParam(defaultValue = "5") int period,
            @RequestParam(required = false) String beforeDate) {

        log.info("모든 섹터 차트 데이터 조회 요청 - 일수: {}, 투자자: {}, 기간: {}, 이전날짜: {}",
            days, investors, period, beforeDate);

        try {
            SectorMaResponse.AllSectorMaChartResponse response = sectorMaService.getAllSectorsChartData(
                days, investors, period, beforeDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("모든 섹터 차트 데이터 조회 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(SectorMaResponse.AllSectorMaChartResponse.builder()
                    .message("조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 배치 수동 실행 (테스트용)
     * POST /api/v1/sector-ma/batch/run
     *
     * @param date 날짜 (yyyyMMdd, 옵션, 기본: 어제)
     * @return 실행 결과
     */
    @PostMapping("/batch/run")
    public ResponseEntity<String> runBatch(@RequestParam(required = false) String date) {
        log.info("섹터 이동평균 배치 수동 실행 요청 - 날짜: {}", date);

        try {
            LocalDate targetDate;
            if (date != null && !date.isEmpty()) {
                targetDate = LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                targetDate = LocalDate.now().minusDays(1);
            }

            batchService.calculateAndSaveAllSectors(targetDate);

            return ResponseEntity.ok("배치 실행 완료: " + targetDate);

        } catch (Exception e) {
            log.error("배치 실행 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("배치 실행 실패: " + e.getMessage());
        }
    }

    /**
     * 기간 배치 수동 실행
     * POST /api/v1/sector-ma/batch/run-period
     *
     * @param startDate 시작일 (yyyyMMdd, 옵션, 기본: 2000-01-02)
     * @param endDate 종료일 (yyyyMMdd, 옵션, 기본: 어제)
     * @return 실행 결과
     */
    @PostMapping("/batch/run-period")
    public ResponseEntity<String> runBatchForPeriod(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("섹터 이동평균 기간 배치 수동 실행 요청 - 시작일: {}, 종료일: {}", startDate, endDate);

        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

            LocalDate start;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate, formatter);
            } else {
                start = LocalDate.of(2000, 1, 2);  // 기본값: 2000-01-02
            }

            LocalDate end;
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate, formatter);
            } else {
                end = LocalDate.now().minusDays(1);  // 기본값: 어제
            }

            log.info("기간 배치 시작 - 시작일: {}, 종료일: {}", start, end);

            batchService.calculateAndSaveAllSectorsForPeriod(start, end);

            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            String result = String.format("기간 배치 실행 완료: %s ~ %s (총 %d일)", start, end, totalDays);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("기간 배치 실행 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("기간 배치 실행 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 섹터의 기간 배치 수동 실행
     * 섹터에 새로운 종목이 추가되었을 때 해당 섹터만 재계산
     * POST /api/v1/sector-ma/batch/run-sector-period
     *
     * @param sectorCd 섹터 코드 (필수)
     * @param startDate 시작일 (yyyyMMdd, 옵션, 기본: 2000-01-02)
     * @param endDate 종료일 (yyyyMMdd, 옵션, 기본: 어제)
     * @return 실행 결과
     */
    @PostMapping("/batch/run-sector-period")
    public ResponseEntity<String> runBatchForSectorPeriod(
            @RequestParam String sectorCd,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("특정 섹터 이동평균 기간 배치 수동 실행 요청 - 섹터: {}, 시작일: {}, 종료일: {}",
            sectorCd, startDate, endDate);

        try {
            // 섹터 코드 검증
            if (sectorCd == null || sectorCd.isEmpty()) {
                return ResponseEntity.badRequest().body("섹터 코드(sectorCd)는 필수입니다");
            }

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

            LocalDate start;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate, formatter);
            } else {
                start = LocalDate.of(2000, 1, 2);  // 기본값: 2000-01-02
            }

            LocalDate end;
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate, formatter);
            } else {
                end = LocalDate.now().minusDays(1);  // 기본값: 어제
            }

            log.info("특정 섹터 기간 배치 시작 - 섹터: {}, 시작일: {}, 종료일: {}", sectorCd, start, end);

            batchService.calculateAndSaveSectorForPeriod(sectorCd, start, end);

            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            String result = String.format("섹터 '%s' 기간 배치 실행 완료: %s ~ %s (총 %d일)",
                sectorCd, start, end, totalDays);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("특정 섹터 기간 배치 실행 실패 - 섹터: {}, 오류: {}", sectorCd, e.getMessage(), e);
            return ResponseEntity.status(500).body("섹터 기간 배치 실행 실패: " + e.getMessage());
        }
    }
}
