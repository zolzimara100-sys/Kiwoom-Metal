package com.stocktrading.kiwoom.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorSectorMaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorSectorMaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 섹터별 투자자 이동평균 배치 서비스
 *
 * 섹터별 거래대금을 계산하고 이동평균을 산출하여 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectorMaBatchService {

    private final JdbcTemplate jdbcTemplate;
    private final StockInvestorSectorMaRepository sectorMaRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 이동평균 기간
    private static final int[] MA_PERIODS = {5, 10, 20, 30, 40, 50, 60, 90, 120, 140};

    /**
     * 특정 날짜의 모든 섹터에 대해 이동평균 계산 및 저장
     */
    @Transactional
    public void calculateAndSaveAllSectors(LocalDate targetDate) {
        log.info("=== 섹터별 이동평균 계산 시작: date={} ===", targetDate);

        try {
            // 1. 모든 섹터 코드 조회
            List<String> sectorCodes = getAllSectorCodes();
            log.info("처리 대상 섹터: {}", sectorCodes);

            int successCount = 0;
            int failCount = 0;

            // 2. 각 섹터별로 처리 (개선된 이동평균 계산 사용)
            for (String sectorCd : sectorCodes) {
                try {
                    calculateAndSaveSectorMaWithTradingDays(sectorCd, targetDate);
                    successCount++;
                } catch (Exception e) {
                    log.error("섹터 처리 실패: sectorCd={}", sectorCd, e);
                    failCount++;
                }
            }

            log.info("=== 섹터별 이동평균 계산 완료: 성공={}, 실패={} ===", successCount, failCount);

        } catch (Exception e) {
            log.error("섹터별 이동평균 계산 실패", e);
            throw e;
        }
    }

    /**
     * 특정 섹터의 기간 범위에 대해 이동평균 계산 및 저장
     * 섹터에 새로운 종목이 추가되었을 때 해당 섹터만 재계산
     * (각 날짜별로 개별 트랜잭션으로 처리하여 타임아웃 방지)
     *
     * @param sectorCd 섹터 코드
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     */
    public void calculateAndSaveSectorForPeriod(String sectorCd, LocalDate startDate, LocalDate endDate) {
        log.info("=== 특정 섹터 이동평균 기간 배치 시작: sectorCd={}, startDate={}, endDate={} ===",
            sectorCd, startDate, endDate);

        try {
            // 1. 날짜 범위 검증
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일이 종료일보다 이후일 수 없습니다");
            }

            // 2. 섹터 코드 검증
            if (sectorCd == null || sectorCd.isEmpty()) {
                throw new IllegalArgumentException("섹터 코드는 필수입니다");
            }

            // 3. 처리할 총 일수 계산
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            log.info("섹터: {}, 총 처리 대상 일수: {} 일", sectorCd, totalDays);

            int successCount = 0;
            int failCount = 0;
            long processedDays = 0;

            // 4. 날짜별로 순회하며 처리
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                processedDays++;

                if (processedDays % 100 == 0 || processedDays == 1) {
                    log.info(">>> [{}] 처리 중: {} ({}/{}) <<<", sectorCd, currentDate, processedDays, totalDays);
                }

                try {
                    // 개별 트랜잭션으로 처리 (타임아웃 방지)
                    calculateAndSaveSectorMaWithTradingDays(sectorCd, currentDate);
                    successCount++;
                } catch (Exception e) {
                    log.error("[{}] 날짜 처리 실패: date={}, 오류={}", sectorCd, currentDate, e.getMessage(), e);
                    failCount++;
                }

                // 다음 날짜로 이동
                currentDate = currentDate.plusDays(1);

                // 진행률 로그 (10% 단위)
                if (processedDays % Math.max(1, totalDays / 10) == 0) {
                    double progress = (processedDays * 100.0) / totalDays;
                    log.info("=== [{}] 진행률: {}/{} ({:.1f}%) ===", sectorCd, processedDays, totalDays, progress);
                }
            }

            log.info("=== 특정 섹터 이동평균 기간 배치 완료 ===");
            log.info("섹터: {}", sectorCd);
            log.info("처리 기간: {} ~ {}", startDate, endDate);
            log.info("처리 일수: {} 일", totalDays);
            log.info("성공: {} 건", successCount);
            log.info("실패: {} 건", failCount);

        } catch (Exception e) {
            log.error("특정 섹터 기간 배치 실행 중 오류 발생: sectorCd={}", sectorCd, e);
            throw e;
        }
    }

    /**
     * 기간 범위의 모든 섹터에 대해 이동평균 계산 및 저장
     * (각 섹터/날짜별로 개별 트랜잭션으로 처리하여 타임아웃 방지)
     */
    public void calculateAndSaveAllSectorsForPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("=== 섹터별 이동평균 기간 배치 시작: startDate={}, endDate={} ===", startDate, endDate);

        try {
            // 1. 날짜 범위 검증
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일이 종료일보다 이후일 수 없습니다");
            }

            // 2. 처리할 총 일수 계산
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            log.info("총 처리 대상 일수: {} 일", totalDays);

            // 3. 모든 섹터 코드 조회
            List<String> sectorCodes = getAllSectorCodes();
            log.info("처리 대상 섹터: {} 개 - {}", sectorCodes.size(), sectorCodes);

            int totalSuccess = 0;
            int totalFail = 0;
            long processedDays = 0;

            // 4. 날짜별로 순회하며 처리
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                processedDays++;
                log.info(">>> 처리 중: {} ({}/{}) <<<", currentDate, processedDays, totalDays);

                int daySuccess = 0;
                int dayFail = 0;

                // 각 섹터별로 처리 (개선된 이동평균 계산 사용)
                for (String sectorCd : sectorCodes) {
                    try {
                        calculateAndSaveSectorMaWithTradingDays(sectorCd, currentDate);
                        daySuccess++;
                        totalSuccess++;
                    } catch (Exception e) {
                        log.error("섹터 처리 실패: date={}, sectorCd={}", currentDate, sectorCd, e);
                        dayFail++;
                        totalFail++;
                    }
                }

                log.info("날짜 처리 완료: date={}, 성공={}, 실패={}", currentDate, daySuccess, dayFail);

                // 다음 날짜로 이동
                currentDate = currentDate.plusDays(1);

                // 진행률 로그 (10% 단위)
                if (processedDays % Math.max(1, totalDays / 10) == 0) {
                    double progress = (processedDays * 100.0) / totalDays;
                    log.info("=== 전체 진행률: {}/{} ({:.1f}%) ===", processedDays, totalDays, progress);
                }
            }

            log.info("=== 섹터별 이동평균 기간 배치 완료 ===");
            log.info("처리 기간: {} ~ {}", startDate, endDate);
            log.info("처리 일수: {} 일", totalDays);
            log.info("총 성공: {} 건", totalSuccess);
            log.info("총 실패: {} 건", totalFail);

        } catch (Exception e) {
            log.error("섹터별 이동평균 기간 배치 실패", e);
            throw e;
        }
    }

    /**
     * 특정 섹터의 이동평균 계산 및 저장 (거래일 기준 정확한 계산)
     */
    @Transactional
    public void calculateAndSaveSectorMaWithTradingDays(String sectorCd, LocalDate targetDate) {
        log.info("섹터 이동평균 계산 (거래일 기준): sectorCd={}, date={}", sectorCd, targetDate);

        try {
            // 1. 섹터별 일별 거래대금 데이터 조회 (targetDate 이하의 모든 거래일 데이터)
            List<SectorDailyData> allDailyDataList = getSectorDailyTradingAmountUntilDate(sectorCd, targetDate);

            if (allDailyDataList.isEmpty()) {
                log.warn("섹터 데이터 없음: sectorCd={}", sectorCd);
                return;
            }

            // 날짜 내림차순으로 정렬 (최신 날짜가 앞에 오도록)
            allDailyDataList.sort((a, b) -> b.getDt().compareTo(a.getDt()));

            // 2. targetDate 데이터가 있는지 확인
            boolean targetDateExists = allDailyDataList.stream()
                .anyMatch(d -> d.getDt().equals(targetDate.format(DATE_FORMATTER)));

            if (!targetDateExists) {
                log.warn("대상 날짜 데이터 없음: sectorCd={}, date={}", sectorCd, targetDate);
                return;
            }

            // 3. 이동평균 계산 (거래일 기준으로 정확한 기간 사용)
            StockInvestorSectorMaEntity entity = calculateMovingAveragesWithTradingDays(sectorCd, targetDate, allDailyDataList);

            // 4. 저장
            sectorMaRepository.save(entity);

            log.info("섹터 이동평균 저장 완료: sectorCd={}, date={}", sectorCd, targetDate);

        } catch (Exception e) {
            log.error("섹터 이동평균 계산 실패: sectorCd={}, date={}", sectorCd, targetDate, e);
            throw e;
        }
    }

    /**
     * 특정 섹터의 이동평균 계산 및 저장 (calculateAndSaveAllSectorsForPeriod에서 사용)
     */
    @Transactional
    public void calculateAndSaveSectorMa(String sectorCd, LocalDate targetDate) {
        log.info("섹터 이동평균 계산: sectorCd={}, date={}", sectorCd, targetDate);

        try {
            // 1. 이동평균 계산을 위한 과거 데이터 조회 (최대 140일 필요)
            LocalDate startDate = targetDate.minusDays(150);  // 여유있게 150일

            // 2. 섹터별 일별 거래대금 데이터 조회
            List<SectorDailyData> dailyDataList = getSectorDailyTradingAmount(sectorCd, startDate, targetDate);

            if (dailyDataList.isEmpty()) {
                log.warn("섹터 데이터 없음: sectorCd={}", sectorCd);
                return;
            }

            // 날짜 순으로 정렬
            dailyDataList.sort(Comparator.comparing(SectorDailyData::getDt));

            // 3. targetDate 데이터가 있는지 확인
            boolean targetDateExists = dailyDataList.stream()
                .anyMatch(d -> d.getDt().equals(targetDate.format(DATE_FORMATTER)));

            if (!targetDateExists) {
                log.warn("대상 날짜 데이터 없음: sectorCd={}, date={}", sectorCd, targetDate);
                return;
            }

            // 4. 이동평균 계산
            StockInvestorSectorMaEntity entity = calculateMovingAverages(sectorCd, targetDate, dailyDataList);

            // 5. 저장
            sectorMaRepository.save(entity);

            log.info("섹터 이동평균 저장 완료: sectorCd={}, date={}", sectorCd, targetDate);

        } catch (Exception e) {
            log.error("섹터 이동평균 계산 실패: sectorCd={}, date={}", sectorCd, targetDate, e);
            throw e;
        }
    }

    /**
     * 모든 섹터 코드 조회
     */
    private List<String> getAllSectorCodes() {
        String sql = "SELECT DISTINCT detail FROM tb_stock_list_meta WHERE main = 'SECTOR' AND detail IS NOT NULL";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 특정 날짜까지의 섹터별 일별 거래대금 데이터 조회 (거래일 기준 정확한 계산용)
     */
    private List<SectorDailyData> getSectorDailyTradingAmountUntilDate(String sectorCd, LocalDate endDate) {
        String sql =
            "WITH sector_stocks AS ( " +
            "  SELECT code FROM tb_stock_list_meta " +
            "  WHERE main = 'SECTOR' AND detail = ? " +
            ") " +
            "SELECT " +
            "  TO_CHAR(c.dt, 'YYYYMMDD') as dt, " +
            "  COALESCE(SUM(c.cur_prc * c.frgnr_invsr), 0) as frgnr_invsr, " +
            "  COALESCE(SUM(c.cur_prc * c.orgn), 0) as orgn, " +
            "  COALESCE(SUM(c.cur_prc * (c.frgnr_invsr + c.orgn)), 0) as frgnr_orgn, " +
            "  COALESCE(SUM(c.cur_prc * c.ind_invsr), 0) as ind_invsr, " +
            "  COALESCE(SUM(c.cur_prc * c.fnnc_invt), 0) as fnnc_invt, " +
            "  COALESCE(SUM(c.cur_prc * c.insrnc), 0) as insrnc, " +
            "  COALESCE(SUM(c.cur_prc * c.invtrt), 0) as invtrt, " +
            "  COALESCE(SUM(c.cur_prc * c.etc_fnnc), 0) as etc_fnnc, " +
            "  COALESCE(SUM(c.cur_prc * c.bank), 0) as bank, " +
            "  COALESCE(SUM(c.cur_prc * c.penfnd_etc), 0) as penfnd_etc, " +
            "  COALESCE(SUM(c.cur_prc * c.samo_fund), 0) as samo_fund, " +
            "  COALESCE(SUM(c.cur_prc * c.natn), 0) as natn, " +
            "  COALESCE(SUM(c.cur_prc * c.etc_corp), 0) as etc_corp, " +
            "  COALESCE(SUM(c.cur_prc * c.natfor), 0) as natfor " +
            "FROM tb_stock_investor_chart c " +
            "INNER JOIN sector_stocks s ON c.stk_cd = s.code " +
            "WHERE c.dt <= ? " +
            "GROUP BY c.dt " +
            "ORDER BY c.dt DESC " +  // 내림차순으로 정렬 (최신 날짜가 먼저)
            "LIMIT 200";  // 충분한 거래일 데이터 확보 (140일 + 여유분)

        return jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                SectorDailyData data = new SectorDailyData();
                data.setDt(rs.getString("dt"));
                data.setFrgnrInvsr(rs.getLong("frgnr_invsr"));
                data.setOrgn(rs.getLong("orgn"));
                data.setFrgnrOrgn(rs.getLong("frgnr_orgn"));
                data.setIndInvsr(rs.getLong("ind_invsr"));
                data.setFnncInvt(rs.getLong("fnnc_invt"));
                data.setInsrnc(rs.getLong("insrnc"));
                data.setInvtrt(rs.getLong("invtrt"));
                data.setEtcFnnc(rs.getLong("etc_fnnc"));
                data.setBank(rs.getLong("bank"));
                data.setPenfndEtc(rs.getLong("penfnd_etc"));
                data.setSamoFund(rs.getLong("samo_fund"));
                data.setNatn(rs.getLong("natn"));
                data.setEtcCorp(rs.getLong("etc_corp"));
                data.setNatfor(rs.getLong("natfor"));
                return data;
            },
            sectorCd,
            java.sql.Date.valueOf(endDate)
        );
    }

    /**
     * 섹터별 일별 거래대금 데이터 조회
     *
     * 거래대금 = cur_prc × 투자자별_순매수량
     * 섹터별로 GROUP BY하여 SUM
     */
    private List<SectorDailyData> getSectorDailyTradingAmount(String sectorCd, LocalDate startDate, LocalDate endDate) {
        String sql =
            "WITH sector_stocks AS ( " +
            "  SELECT code FROM tb_stock_list_meta " +
            "  WHERE main = 'SECTOR' AND detail = ? " +
            ") " +
            "SELECT " +
            "  TO_CHAR(c.dt, 'YYYYMMDD') as dt, " +
            "  COALESCE(SUM(c.cur_prc * c.frgnr_invsr), 0) as frgnr_invsr, " +
            "  COALESCE(SUM(c.cur_prc * c.orgn), 0) as orgn, " +
            "  COALESCE(SUM(c.cur_prc * (c.frgnr_invsr + c.orgn)), 0) as frgnr_orgn, " +
            "  COALESCE(SUM(c.cur_prc * c.ind_invsr), 0) as ind_invsr, " +
            "  COALESCE(SUM(c.cur_prc * c.fnnc_invt), 0) as fnnc_invt, " +
            "  COALESCE(SUM(c.cur_prc * c.insrnc), 0) as insrnc, " +
            "  COALESCE(SUM(c.cur_prc * c.invtrt), 0) as invtrt, " +
            "  COALESCE(SUM(c.cur_prc * c.etc_fnnc), 0) as etc_fnnc, " +
            "  COALESCE(SUM(c.cur_prc * c.bank), 0) as bank, " +
            "  COALESCE(SUM(c.cur_prc * c.penfnd_etc), 0) as penfnd_etc, " +
            "  COALESCE(SUM(c.cur_prc * c.samo_fund), 0) as samo_fund, " +
            "  COALESCE(SUM(c.cur_prc * c.natn), 0) as natn, " +
            "  COALESCE(SUM(c.cur_prc * c.etc_corp), 0) as etc_corp, " +
            "  COALESCE(SUM(c.cur_prc * c.natfor), 0) as natfor " +
            "FROM tb_stock_investor_chart c " +
            "INNER JOIN sector_stocks s ON c.stk_cd = s.code " +
            "WHERE c.dt BETWEEN ? AND ? " +
            "GROUP BY c.dt " +
            "ORDER BY c.dt";

        return jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                SectorDailyData data = new SectorDailyData();
                data.setDt(rs.getString("dt"));
                data.setFrgnrInvsr(rs.getLong("frgnr_invsr"));
                data.setOrgn(rs.getLong("orgn"));
                data.setFrgnrOrgn(rs.getLong("frgnr_orgn"));
                data.setIndInvsr(rs.getLong("ind_invsr"));
                data.setFnncInvt(rs.getLong("fnnc_invt"));
                data.setInsrnc(rs.getLong("insrnc"));
                data.setInvtrt(rs.getLong("invtrt"));
                data.setEtcFnnc(rs.getLong("etc_fnnc"));
                data.setBank(rs.getLong("bank"));
                data.setPenfndEtc(rs.getLong("penfnd_etc"));
                data.setSamoFund(rs.getLong("samo_fund"));
                data.setNatn(rs.getLong("natn"));
                data.setEtcCorp(rs.getLong("etc_corp"));
                data.setNatfor(rs.getLong("natfor"));
                return data;
            },
            sectorCd,
            java.sql.Date.valueOf(startDate),
            java.sql.Date.valueOf(endDate)
        );
    }

    /**
     * 이동평균 계산 (거래일 기준 정확한 계산)
     */
    private StockInvestorSectorMaEntity calculateMovingAveragesWithTradingDays(
        String sectorCd,
        LocalDate targetDate,
        List<SectorDailyData> dailyDataList
    ) {
        String targetDateStr = targetDate.format(DATE_FORMATTER);

        // 섹터명 조회
        String sectorNm = getSectorName(sectorCd);

        // Entity 생성
        StockInvestorSectorMaEntity entity = StockInvestorSectorMaEntity.builder()
            .sectorCd(sectorCd)
            .dt(targetDateStr)
            .sectorNm(sectorNm)
            .build();

        // targetDate가 리스트의 첫 번째 (최신) 데이터인지 확인
        if (!dailyDataList.get(0).getDt().equals(targetDateStr)) {
            // targetDate까지의 데이터만 필터링 (내림차순 정렬되어 있음)
            List<SectorDailyData> filteredList = new ArrayList<>();
            for (SectorDailyData data : dailyDataList) {
                if (data.getDt().compareTo(targetDateStr) <= 0) {
                    filteredList.add(data);
                }
            }
            dailyDataList = filteredList;
        }

        // 각 이동평균 기간에 대해 계산 (거래일 기준)
        for (int period : MA_PERIODS) {
            calculateMovingAverageForTradingDays(entity, dailyDataList, period);
        }

        return entity;
    }

    /**
     * 이동평균 계산 (기존 메서드 - calculateAndSaveAllSectors 에서 사용)
     */
    private StockInvestorSectorMaEntity calculateMovingAverages(
        String sectorCd,
        LocalDate targetDate,
        List<SectorDailyData> dailyDataList
    ) {
        String targetDateStr = targetDate.format(DATE_FORMATTER);

        // 섹터명 조회
        String sectorNm = getSectorName(sectorCd);

        // Entity 생성
        StockInvestorSectorMaEntity entity = StockInvestorSectorMaEntity.builder()
            .sectorCd(sectorCd)
            .dt(targetDateStr)
            .sectorNm(sectorNm)
            .build();

        // targetDate의 인덱스 찾기
        int targetIndex = -1;
        for (int i = 0; i < dailyDataList.size(); i++) {
            if (dailyDataList.get(i).getDt().equals(targetDateStr)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            throw new IllegalStateException("Target date not found in daily data");
        }

        // 각 이동평균 기간에 대해 계산
        for (int period : MA_PERIODS) {
            calculateMovingAverageForPeriod(entity, dailyDataList, targetIndex, period);
        }

        return entity;
    }

    /**
     * 특정 기간의 이동평균 계산 (거래일 기준)
     */
    private void calculateMovingAverageForTradingDays(
        StockInvestorSectorMaEntity entity,
        List<SectorDailyData> dailyDataList,
        int period
    ) {
        // 거래일 기준으로 정확히 period 개수만큼 가져오기
        if (dailyDataList.size() < period) {
            log.debug("이동평균 계산 스킵 (데이터 부족): period={}, available={}",
                period, dailyDataList.size());
            return;
        }

        // 정확히 period 개수만큼 데이터 가져오기 (이미 내림차순 정렬되어 있음)
        List<SectorDailyData> periodData = dailyDataList.subList(0, period);

        // 각 투자자별 이동평균 계산
        setMovingAverageValue(entity, "frgnr_invsr", period, calculateAverage(periodData, SectorDailyData::getFrgnrInvsr));
        setMovingAverageValue(entity, "orgn", period, calculateAverage(periodData, SectorDailyData::getOrgn));
        setMovingAverageValue(entity, "frgnr_orgn", period, calculateAverage(periodData, SectorDailyData::getFrgnrOrgn));
        setMovingAverageValue(entity, "ind_invsr", period, calculateAverage(periodData, SectorDailyData::getIndInvsr));
        setMovingAverageValue(entity, "fnnc_invt", period, calculateAverage(periodData, SectorDailyData::getFnncInvt));
        setMovingAverageValue(entity, "insrnc", period, calculateAverage(periodData, SectorDailyData::getInsrnc));
        setMovingAverageValue(entity, "invtrt", period, calculateAverage(periodData, SectorDailyData::getInvtrt));
        setMovingAverageValue(entity, "etc_fnnc", period, calculateAverage(periodData, SectorDailyData::getEtcFnnc));
        setMovingAverageValue(entity, "bank", period, calculateAverage(periodData, SectorDailyData::getBank));
        setMovingAverageValue(entity, "penfnd_etc", period, calculateAverage(periodData, SectorDailyData::getPenfndEtc));
        setMovingAverageValue(entity, "samo_fund", period, calculateAverage(periodData, SectorDailyData::getSamoFund));
        setMovingAverageValue(entity, "natn", period, calculateAverage(periodData, SectorDailyData::getNatn));
        setMovingAverageValue(entity, "etc_corp", period, calculateAverage(periodData, SectorDailyData::getEtcCorp));
        setMovingAverageValue(entity, "natfor", period, calculateAverage(periodData, SectorDailyData::getNatfor));
    }

    /**
     * 특정 기간의 이동평균 계산 (기존 메서드)
     */
    private void calculateMovingAverageForPeriod(
        StockInvestorSectorMaEntity entity,
        List<SectorDailyData> dailyDataList,
        int targetIndex,
        int period
    ) {
        int startIndex = Math.max(0, targetIndex - period + 1);
        List<SectorDailyData> periodData = dailyDataList.subList(startIndex, targetIndex + 1);

        if (periodData.size() < period * 0.7) {  // 최소 70% 데이터 필요
            log.warn("이동평균 계산 스킵 (데이터 부족): period={}, required={}, actual={}",
                period, period, periodData.size());
            return;
        }

        // 각 투자자별 이동평균 계산
        setMovingAverageValue(entity, "frgnr_invsr", period, calculateAverage(periodData, SectorDailyData::getFrgnrInvsr));
        setMovingAverageValue(entity, "orgn", period, calculateAverage(periodData, SectorDailyData::getOrgn));
        setMovingAverageValue(entity, "frgnr_orgn", period, calculateAverage(periodData, SectorDailyData::getFrgnrOrgn));
        setMovingAverageValue(entity, "ind_invsr", period, calculateAverage(periodData, SectorDailyData::getIndInvsr));
        setMovingAverageValue(entity, "fnnc_invt", period, calculateAverage(periodData, SectorDailyData::getFnncInvt));
        setMovingAverageValue(entity, "insrnc", period, calculateAverage(periodData, SectorDailyData::getInsrnc));
        setMovingAverageValue(entity, "invtrt", period, calculateAverage(periodData, SectorDailyData::getInvtrt));
        setMovingAverageValue(entity, "etc_fnnc", period, calculateAverage(periodData, SectorDailyData::getEtcFnnc));
        setMovingAverageValue(entity, "bank", period, calculateAverage(periodData, SectorDailyData::getBank));
        setMovingAverageValue(entity, "penfnd_etc", period, calculateAverage(periodData, SectorDailyData::getPenfndEtc));
        setMovingAverageValue(entity, "samo_fund", period, calculateAverage(periodData, SectorDailyData::getSamoFund));
        setMovingAverageValue(entity, "natn", period, calculateAverage(periodData, SectorDailyData::getNatn));
        setMovingAverageValue(entity, "etc_corp", period, calculateAverage(periodData, SectorDailyData::getEtcCorp));
        setMovingAverageValue(entity, "natfor", period, calculateAverage(periodData, SectorDailyData::getNatfor));
    }

    /**
     * 평균값 계산
     */
    private BigDecimal calculateAverage(List<SectorDailyData> data, java.util.function.ToLongFunction<SectorDailyData> getter) {
        double avg = data.stream()
            .mapToLong(getter)
            .average()
            .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Entity에 이동평균 값 설정
     */
    private void setMovingAverageValue(StockInvestorSectorMaEntity entity, String investorType, int period, BigDecimal value) {
        String methodName = "set" + toCamelCase(investorType) + "Ma" + period;
        try {
            entity.getClass().getMethod(methodName, BigDecimal.class).invoke(entity, value);
        } catch (Exception e) {
            log.error("Failed to set moving average: {}={}", methodName, value, e);
        }
    }

    /**
     * snake_case를 CamelCase로 변환
     */
    private String toCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(result.length() == 0 ? Character.toUpperCase(c) : c);
                }
            }
        }

        return result.toString();
    }

    /**
     * 섹터명 조회
     */
    private String getSectorName(String sectorCd) {
        Map<String, String> sectorNameMap = new HashMap<>();
        sectorNameMap.put("semicon", "반도체");
        sectorNameMap.put("heavyind", "철강/조선");
        sectorNameMap.put("auto", "자동차");
        sectorNameMap.put("battery", "이차전지");
        sectorNameMap.put("ai_infra", "AI(전력/SMR/에너지)");
        sectorNameMap.put("petro", "석유화학/정유");
        sectorNameMap.put("defense", "방위산업");
        sectorNameMap.put("culture", "뷰티/엔터/게임");
        sectorNameMap.put("robot", "첨단로봇");
        sectorNameMap.put("bio", "바이오/제약");

        return sectorNameMap.getOrDefault(sectorCd, sectorCd);
    }

    /**
     * 섹터별 일별 데이터 DTO
     */
    @lombok.Data
    private static class SectorDailyData {
        private String dt;
        private Long frgnrInvsr;
        private Long orgn;
        private Long frgnrOrgn;
        private Long indInvsr;
        private Long fnncInvt;
        private Long insrnc;
        private Long invtrt;
        private Long etcFnnc;
        private Long bank;
        private Long penfndEtc;
        private Long samoFund;
        private Long natn;
        private Long etcCorp;
        private Long natfor;
    }
}
