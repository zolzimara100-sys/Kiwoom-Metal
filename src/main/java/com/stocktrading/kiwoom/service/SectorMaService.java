package com.stocktrading.kiwoom.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorSectorMaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListMetaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorSectorMaRepository;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockListMetaRepository;
import com.stocktrading.kiwoom.dto.SectorMaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 섹터별 투자자 이동평균 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectorMaService {

    private final StockInvestorSectorMaRepository sectorMaRepository;
    private final StockListMetaRepository stockListMetaRepository;

    /**
     * 섹터별 이동평균 조회 (날짜 범위)
     */
    @Transactional(readOnly = true)
    public SectorMaResponse getSectorMaByPeriod(String sectorCd, String startDate, String endDate) {
        log.info("섹터 이동평균 조회 (기간): sectorCd={}, startDate={}, endDate={}", sectorCd, startDate, endDate);

        List<StockInvestorSectorMaEntity> entities = sectorMaRepository.findBySectorCdAndDtBetween(
            sectorCd, startDate, endDate);

        return SectorMaResponse.from(sectorCd, entities);
    }

    /**
     * 섹터별 이동평균 조회 (최근 N일)
     */
    @Transactional(readOnly = true)
    public SectorMaResponse getRecentSectorMa(String sectorCd, int limit) {
        log.info("섹터 이동평균 조회 (최근): sectorCd={}, limit={}", sectorCd, limit);

        List<StockInvestorSectorMaEntity> entities = sectorMaRepository.findRecentBySectorCd(sectorCd, limit);

        // 날짜 오름차순으로 정렬
        Collections.reverse(entities);

        return SectorMaResponse.from(sectorCd, entities);
    }

    /**
     * 전체 섹터 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SectorMaResponse.SectorInfo> getAllSectors() {
        log.info("전체 섹터 목록 조회");

        Map<String, String> sectorMap = getSectorNameMap();

        return sectorMap.entrySet().stream()
            .map(entry -> new SectorMaResponse.SectorInfo(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(SectorMaResponse.SectorInfo::getSectorCd))
            .collect(Collectors.toList());
    }

    /**
     * 섹터에 속한 종목 목록 조회 (REQ-004-1)
     */
    @Transactional(readOnly = true)
    public List<SectorMaResponse.StockInfo> getStocksBySector(String sectorCd) {
        log.info("섹터별 종목 목록 조회: sectorCd={}", sectorCd);

        // 섹터 코드를 한글 섹터명으로 변환
        Map<String, String> sectorMap = getSectorNameMap();
        String sectorName = sectorMap.getOrDefault(sectorCd, sectorCd);
        log.info("섹터 코드 {} -> 섹터명: {}", sectorCd, sectorName);

        List<StockListMetaEntity> stocks = stockListMetaRepository.findBySector(sectorName);

        return stocks.stream()
            .map(stock -> new SectorMaResponse.StockInfo(stock.getCode(), stock.getName()))
            .collect(Collectors.toList());
    }

    /**
     * 섹터별 차트 데이터 조회 (REQ-004)
     */
    @Transactional(readOnly = true)
    public SectorMaResponse.SectorMaChartResponse getChartData(
            String sectorCd,
            int days,
            String investors,
            int period,
            String beforeDate
    ) {
        log.info("섹터 차트 데이터 조회: sectorCd={}, days={}, investors={}, period={}, beforeDate={}",
            sectorCd, days, investors, period, beforeDate);

        try {
            // 1. 데이터 조회
            List<StockInvestorSectorMaEntity> entities;
            if (beforeDate != null && !beforeDate.isEmpty()) {
                // 무한 스크롤: beforeDate 이전 데이터 조회
                entities = sectorMaRepository.findBySectorCdBeforeDateOrderByDtDesc(sectorCd, beforeDate, days);
            } else {
                // 초기 로드: 최근 N일 데이터 조회
                entities = sectorMaRepository.findRecentBySectorCd(sectorCd, days);
            }

            if (entities.isEmpty()) {
                return SectorMaResponse.SectorMaChartResponse.builder()
                    .sectorCd(sectorCd)
                    .data(Collections.emptyList())
                    .message("데이터가 없습니다.")
                    .build();
            }

            // 2. 날짜 오름차순으로 정렬
            Collections.reverse(entities);

            // 3. 투자자 유형 파싱
            String[] investorArray = investors.split(",");

            // 4. 차트 데이터 포인트 생성
            List<SectorMaResponse.SectorMaChartDataPoint> dataPoints = entities.stream()
                .map(entity -> {
                    SectorMaResponse.SectorMaChartDataPoint.SectorMaChartDataPointBuilder builder =
                        SectorMaResponse.SectorMaChartDataPoint.builder().dt(entity.getDt());

                    // 각 투자자별로 요청된 period의 MA 값 추출
                    for (String investor : investorArray) {
                        java.math.BigDecimal value = getMaValue(entity, investor.trim(), period);
                        setInvestorValue(builder, investor.trim(), value);
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());

            // 5. 섹터명 조회
            String sectorNm = entities.get(0).getSectorNm();

            return SectorMaResponse.SectorMaChartResponse.builder()
                .sectorCd(sectorCd)
                .sectorNm(sectorNm)
                .period(period)
                .data(dataPoints)
                .build();

        } catch (Exception e) {
            log.error("섹터 차트 데이터 조회 실패: sectorCd={}, 오류: {}", sectorCd, e.getMessage(), e);
            return SectorMaResponse.SectorMaChartResponse.builder()
                .sectorCd(sectorCd)
                .message("조회 실패: " + e.getMessage())
                .build();
        }
    }

    /**
     * Entity에서 투자자별, 기간별 MA 값 추출
     */
    private java.math.BigDecimal getMaValue(StockInvestorSectorMaEntity entity, String investor, int period) {
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
            default -> null;
        };
    }

    /**
     * Builder에 투자자별 값 설정
     */
    private void setInvestorValue(
            SectorMaResponse.SectorMaChartDataPoint.SectorMaChartDataPointBuilder builder,
            String investor,
            java.math.BigDecimal value
    ) {
        switch (investor) {
            case "frgnr" -> builder.frgnr(value);
            case "orgn" -> builder.orgn(value);
            case "ind_invsr" -> builder.indInvsr(value);
            case "fnnc_invt" -> builder.fnncInvt(value);
            case "insrnc" -> builder.insrnc(value);
            case "invtrt" -> builder.invtrt(value);
            case "etc_fnnc" -> builder.etcFnnc(value);
            case "bank" -> builder.bank(value);
            case "penfnd_etc" -> builder.penfndEtc(value);
            case "samo_fund" -> builder.samoFund(value);
            case "natn" -> builder.natn(value);
            case "etc_corp" -> builder.etcCorp(value);
            case "natfor" -> builder.natfor(value);
        }
    }

    /**
     * 섹터 코드-이름 매핑
     */
    private Map<String, String> getSectorNameMap() {
        Map<String, String> map = new HashMap<>();
        map.put("semicon", "반도체");
        map.put("heavyind", "철강/조선");
        map.put("auto", "자동차");
        map.put("battery", "이차전지");
        map.put("ai_infra", "AI (전력/SMR/에너지)");
        map.put("petro", "석유화학/정유");
        map.put("defense", "방위산업");
        map.put("culture", "뷰티/엔터/게임");
        map.put("robot", "첨단로봇");
        map.put("bio", "바이오/제약");
        return map;
    }

    /**
     * 모든 섹터 차트 데이터 조회 (REQ-005)
     */
    @Transactional(readOnly = true)
    public SectorMaResponse.AllSectorMaChartResponse getAllSectorsChartData(
            int days, String investors, int period, String beforeDate) {

        log.info("모든 섹터 차트 데이터 조회: days={}, investors={}, period={}, beforeDate={}",
            days, investors, period, beforeDate);

        try {
            // 모든 섹터 목록 조회
            List<SectorMaResponse.SectorInfo> sectors = getAllSectors();
            Map<String, SectorMaResponse.SectorMaChartResponse> sectorDataMap = new HashMap<>();

            // 각 섹터별로 데이터 조회
            for (SectorMaResponse.SectorInfo sectorInfo : sectors) {
                try {
                    SectorMaResponse.SectorMaChartResponse sectorData =
                        getChartData(sectorInfo.getSectorCd(), days, investors, period, beforeDate);
                    sectorDataMap.put(sectorInfo.getSectorCd(), sectorData);
                } catch (Exception e) {
                    log.warn("섹터 {} 데이터 조회 실패: {}", sectorInfo.getSectorCd(), e.getMessage());
                    // 실패한 섹터는 건너뛰고 계속 진행
                }
            }

            if (sectorDataMap.isEmpty()) {
                return SectorMaResponse.AllSectorMaChartResponse.error("모든 섹터 데이터 조회 실패");
            }

            return SectorMaResponse.AllSectorMaChartResponse.success(sectorDataMap);

        } catch (Exception e) {
            log.error("모든 섹터 차트 데이터 조회 실패", e);
            return SectorMaResponse.AllSectorMaChartResponse.error("조회 실패: " + e.getMessage());
        }
    }

    /**
     * 섹터 투자자별 이동평균 비중 계산
     */
    @Transactional(readOnly = true)
    public com.stocktrading.kiwoom.controller.StatisticsController.InvestorRatioMaResponse getSectorInvestorRatioMa(
            String sectorCd, int period, String fromDate, String toDate) {

        log.info("섹터 투자자 비중 계산: sectorCd={}, period={}, from={}, to={}", sectorCd, period, fromDate, toDate);

        // 섹터 이동평균 데이터 조회
        List<StockInvestorSectorMaEntity> data = sectorMaRepository.findBySectorCdAndDtBetween(
            sectorCd, fromDate, toDate);

        if (data.isEmpty()) {
            return com.stocktrading.kiwoom.controller.StatisticsController.InvestorRatioMaResponse.builder()
                .stkCd(sectorCd)
                .period(period)
                .fromDate(fromDate)
                .toDate(toDate)
                .dataCount(0)
                .message("데이터가 없습니다.")
                .build();
        }

        // 투자자별 이동평균값 abs 합계 계산
        Map<String, java.math.BigDecimal> volumeMap = new HashMap<>();
        String[] investors = { "frgnr", "orgn", "fnncInvt", "insrnc", "invtrt", "bank",
                "etcFnnc", "penfndEtc", "samoFund", "etcCorp", "natn", "natfor" };

        for (String inv : investors) {
            volumeMap.put(inv, java.math.BigDecimal.ZERO);
        }

        // 각 row에서 선택된 기간의 MA값 추출하여 누적
        for (StockInvestorSectorMaEntity row : data) {
            java.math.BigDecimal frgnrMa = getSectorMaValue(row, "frgnr", period);
            if (frgnrMa != null) volumeMap.put("frgnr", volumeMap.get("frgnr").add(frgnrMa.abs()));

            java.math.BigDecimal orgnMa = getSectorMaValue(row, "orgn", period);
            if (orgnMa != null) volumeMap.put("orgn", volumeMap.get("orgn").add(orgnMa.abs()));

            java.math.BigDecimal fnncInvtMa = getSectorMaValue(row, "fnncInvt", period);
            if (fnncInvtMa != null) volumeMap.put("fnncInvt", volumeMap.get("fnncInvt").add(fnncInvtMa.abs()));

            java.math.BigDecimal insrncMa = getSectorMaValue(row, "insrnc", period);
            if (insrncMa != null) volumeMap.put("insrnc", volumeMap.get("insrnc").add(insrncMa.abs()));

            java.math.BigDecimal invtrtMa = getSectorMaValue(row, "invtrt", period);
            if (invtrtMa != null) volumeMap.put("invtrt", volumeMap.get("invtrt").add(invtrtMa.abs()));

            java.math.BigDecimal bankMa = getSectorMaValue(row, "bank", period);
            if (bankMa != null) volumeMap.put("bank", volumeMap.get("bank").add(bankMa.abs()));

            java.math.BigDecimal etcFnncMa = getSectorMaValue(row, "etcFnnc", period);
            if (etcFnncMa != null) volumeMap.put("etcFnnc", volumeMap.get("etcFnnc").add(etcFnncMa.abs()));

            java.math.BigDecimal penfndEtcMa = getSectorMaValue(row, "penfndEtc", period);
            if (penfndEtcMa != null) volumeMap.put("penfndEtc", volumeMap.get("penfndEtc").add(penfndEtcMa.abs()));

            java.math.BigDecimal samoFundMa = getSectorMaValue(row, "samoFund", period);
            if (samoFundMa != null) volumeMap.put("samoFund", volumeMap.get("samoFund").add(samoFundMa.abs()));

            java.math.BigDecimal etcCorpMa = getSectorMaValue(row, "etcCorp", period);
            if (etcCorpMa != null) volumeMap.put("etcCorp", volumeMap.get("etcCorp").add(etcCorpMa.abs()));

            java.math.BigDecimal natnMa = getSectorMaValue(row, "natn", period);
            if (natnMa != null) volumeMap.put("natn", volumeMap.get("natn").add(natnMa.abs()));

            java.math.BigDecimal natforMa = getSectorMaValue(row, "natfor", period);
            if (natforMa != null) volumeMap.put("natfor", volumeMap.get("natfor").add(natforMa.abs()));
        }

        // 전체 합계 계산
        java.math.BigDecimal totalVolume = volumeMap.values().stream()
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (totalVolume.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return com.stocktrading.kiwoom.controller.StatisticsController.InvestorRatioMaResponse.builder()
                .stkCd(sectorCd)
                .period(period)
                .fromDate(fromDate)
                .toDate(toDate)
                .dataCount(data.size())
                .message("거래량 합계가 0입니다.")
                .build();
        }

        // 비율 계산 (%)
        Map<String, java.math.BigDecimal> ratioMap = new HashMap<>();
        for (String inv : investors) {
            java.math.BigDecimal ratio = volumeMap.get(inv)
                .divide(totalVolume, 4, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100));
            ratioMap.put(inv, ratio);
        }

        return com.stocktrading.kiwoom.controller.StatisticsController.InvestorRatioMaResponse.builder()
            .stkCd(sectorCd)
            .period(period)
            .fromDate(fromDate)
            .toDate(toDate)
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
            .message("조회 성공")
            .build();
    }

    /**
     * 섹터 Entity에서 투자자별 기간별 MA값 추출
     */
    private java.math.BigDecimal getSectorMaValue(StockInvestorSectorMaEntity entity, String investor, int period) {
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
            case "fnncInvt" -> switch (period) {
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
            case "etcFnnc" -> switch (period) {
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
            case "penfndEtc" -> switch (period) {
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
            case "samoFund" -> switch (period) {
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
            case "etcCorp" -> switch (period) {
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
            default -> null;
        };
    }
}
