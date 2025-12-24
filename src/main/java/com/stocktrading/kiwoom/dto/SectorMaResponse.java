package com.stocktrading.kiwoom.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorSectorMaEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 섹터별 투자자 이동평균 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorMaResponse {

    private String sectorCd;
    private String sectorNm;
    private List<SectorMaData> data;
    private String error;

    /**
     * Entity 리스트로부터 Response 생성
     */
    public static SectorMaResponse from(String sectorCd, List<StockInvestorSectorMaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return SectorMaResponse.builder()
                .sectorCd(sectorCd)
                .data(List.of())
                .build();
        }

        String sectorNm = entities.get(0).getSectorNm();
        List<SectorMaData> dataList = entities.stream()
            .map(SectorMaData::from)
            .collect(Collectors.toList());

        return SectorMaResponse.builder()
            .sectorCd(sectorCd)
            .sectorNm(sectorNm)
            .data(dataList)
            .build();
    }

    /**
     * 에러 응답 생성
     */
    public static SectorMaResponse error(String message) {
        return SectorMaResponse.builder()
            .error(message)
            .build();
    }

    /**
     * 섹터 이동평균 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMaData {
        private String dt;

        // 외국인
        private BigDecimal frgnrInvsrMa5;
        private BigDecimal frgnrInvsrMa10;
        private BigDecimal frgnrInvsrMa20;
        private BigDecimal frgnrInvsrMa30;
        private BigDecimal frgnrInvsrMa40;
        private BigDecimal frgnrInvsrMa50;
        private BigDecimal frgnrInvsrMa60;
        private BigDecimal frgnrInvsrMa90;
        private BigDecimal frgnrInvsrMa120;
        private BigDecimal frgnrInvsrMa140;

        // 기관계
        private BigDecimal orgnMa5;
        private BigDecimal orgnMa10;
        private BigDecimal orgnMa20;
        private BigDecimal orgnMa30;
        private BigDecimal orgnMa40;
        private BigDecimal orgnMa50;
        private BigDecimal orgnMa60;
        private BigDecimal orgnMa90;
        private BigDecimal orgnMa120;
        private BigDecimal orgnMa140;

        // 외인+기관
        private BigDecimal frgnrOrgnMa5;
        private BigDecimal frgnrOrgnMa10;
        private BigDecimal frgnrOrgnMa20;
        private BigDecimal frgnrOrgnMa30;
        private BigDecimal frgnrOrgnMa40;
        private BigDecimal frgnrOrgnMa50;
        private BigDecimal frgnrOrgnMa60;
        private BigDecimal frgnrOrgnMa90;
        private BigDecimal frgnrOrgnMa120;
        private BigDecimal frgnrOrgnMa140;

        // 개인투자자
        private BigDecimal indInvsrMa5;
        private BigDecimal indInvsrMa10;
        private BigDecimal indInvsrMa20;
        private BigDecimal indInvsrMa30;
        private BigDecimal indInvsrMa40;
        private BigDecimal indInvsrMa50;
        private BigDecimal indInvsrMa60;
        private BigDecimal indInvsrMa90;
        private BigDecimal indInvsrMa120;
        private BigDecimal indInvsrMa140;

        // 금융투자
        private BigDecimal fnncInvtMa5;
        private BigDecimal fnncInvtMa10;
        private BigDecimal fnncInvtMa20;
        private BigDecimal fnncInvtMa30;
        private BigDecimal fnncInvtMa40;
        private BigDecimal fnncInvtMa50;
        private BigDecimal fnncInvtMa60;
        private BigDecimal fnncInvtMa90;
        private BigDecimal fnncInvtMa120;
        private BigDecimal fnncInvtMa140;

        // 보험
        private BigDecimal insrncMa5;
        private BigDecimal insrncMa10;
        private BigDecimal insrncMa20;
        private BigDecimal insrncMa30;
        private BigDecimal insrncMa40;
        private BigDecimal insrncMa50;
        private BigDecimal insrncMa60;
        private BigDecimal insrncMa90;
        private BigDecimal insrncMa120;
        private BigDecimal insrncMa140;

        // 투신
        private BigDecimal invtrtMa5;
        private BigDecimal invtrtMa10;
        private BigDecimal invtrtMa20;
        private BigDecimal invtrtMa30;
        private BigDecimal invtrtMa40;
        private BigDecimal invtrtMa50;
        private BigDecimal invtrtMa60;
        private BigDecimal invtrtMa90;
        private BigDecimal invtrtMa120;
        private BigDecimal invtrtMa140;

        // 기타금융
        private BigDecimal etcFnncMa5;
        private BigDecimal etcFnncMa10;
        private BigDecimal etcFnncMa20;
        private BigDecimal etcFnncMa30;
        private BigDecimal etcFnncMa40;
        private BigDecimal etcFnncMa50;
        private BigDecimal etcFnncMa60;
        private BigDecimal etcFnncMa90;
        private BigDecimal etcFnncMa120;
        private BigDecimal etcFnncMa140;

        // 은행
        private BigDecimal bankMa5;
        private BigDecimal bankMa10;
        private BigDecimal bankMa20;
        private BigDecimal bankMa30;
        private BigDecimal bankMa40;
        private BigDecimal bankMa50;
        private BigDecimal bankMa60;
        private BigDecimal bankMa90;
        private BigDecimal bankMa120;
        private BigDecimal bankMa140;

        // 연기금등
        private BigDecimal penfndEtcMa5;
        private BigDecimal penfndEtcMa10;
        private BigDecimal penfndEtcMa20;
        private BigDecimal penfndEtcMa30;
        private BigDecimal penfndEtcMa40;
        private BigDecimal penfndEtcMa50;
        private BigDecimal penfndEtcMa60;
        private BigDecimal penfndEtcMa90;
        private BigDecimal penfndEtcMa120;
        private BigDecimal penfndEtcMa140;

        // 사모펀드
        private BigDecimal samoFundMa5;
        private BigDecimal samoFundMa10;
        private BigDecimal samoFundMa20;
        private BigDecimal samoFundMa30;
        private BigDecimal samoFundMa40;
        private BigDecimal samoFundMa50;
        private BigDecimal samoFundMa60;
        private BigDecimal samoFundMa90;
        private BigDecimal samoFundMa120;
        private BigDecimal samoFundMa140;

        // 국가
        private BigDecimal natnMa5;
        private BigDecimal natnMa10;
        private BigDecimal natnMa20;
        private BigDecimal natnMa30;
        private BigDecimal natnMa40;
        private BigDecimal natnMa50;
        private BigDecimal natnMa60;
        private BigDecimal natnMa90;
        private BigDecimal natnMa120;
        private BigDecimal natnMa140;

        // 기타법인
        private BigDecimal etcCorpMa5;
        private BigDecimal etcCorpMa10;
        private BigDecimal etcCorpMa20;
        private BigDecimal etcCorpMa30;
        private BigDecimal etcCorpMa40;
        private BigDecimal etcCorpMa50;
        private BigDecimal etcCorpMa60;
        private BigDecimal etcCorpMa90;
        private BigDecimal etcCorpMa120;
        private BigDecimal etcCorpMa140;

        // 내외국인
        private BigDecimal natforMa5;
        private BigDecimal natforMa10;
        private BigDecimal natforMa20;
        private BigDecimal natforMa30;
        private BigDecimal natforMa40;
        private BigDecimal natforMa50;
        private BigDecimal natforMa60;
        private BigDecimal natforMa90;
        private BigDecimal natforMa120;
        private BigDecimal natforMa140;

        /**
         * Entity로부터 DTO 생성
         */
        public static SectorMaData from(StockInvestorSectorMaEntity entity) {
            return SectorMaData.builder()
                .dt(entity.getDt())
                // 외국인
                .frgnrInvsrMa5(entity.getFrgnrInvsrMa5()).frgnrInvsrMa10(entity.getFrgnrInvsrMa10())
                .frgnrInvsrMa20(entity.getFrgnrInvsrMa20()).frgnrInvsrMa30(entity.getFrgnrInvsrMa30())
                .frgnrInvsrMa40(entity.getFrgnrInvsrMa40()).frgnrInvsrMa50(entity.getFrgnrInvsrMa50())
                .frgnrInvsrMa60(entity.getFrgnrInvsrMa60()).frgnrInvsrMa90(entity.getFrgnrInvsrMa90())
                .frgnrInvsrMa120(entity.getFrgnrInvsrMa120()).frgnrInvsrMa140(entity.getFrgnrInvsrMa140())
                // 기관계
                .orgnMa5(entity.getOrgnMa5()).orgnMa10(entity.getOrgnMa10())
                .orgnMa20(entity.getOrgnMa20()).orgnMa30(entity.getOrgnMa30())
                .orgnMa40(entity.getOrgnMa40()).orgnMa50(entity.getOrgnMa50())
                .orgnMa60(entity.getOrgnMa60()).orgnMa90(entity.getOrgnMa90())
                .orgnMa120(entity.getOrgnMa120()).orgnMa140(entity.getOrgnMa140())
                // 외인+기관
                .frgnrOrgnMa5(entity.getFrgnrOrgnMa5()).frgnrOrgnMa10(entity.getFrgnrOrgnMa10())
                .frgnrOrgnMa20(entity.getFrgnrOrgnMa20()).frgnrOrgnMa30(entity.getFrgnrOrgnMa30())
                .frgnrOrgnMa40(entity.getFrgnrOrgnMa40()).frgnrOrgnMa50(entity.getFrgnrOrgnMa50())
                .frgnrOrgnMa60(entity.getFrgnrOrgnMa60()).frgnrOrgnMa90(entity.getFrgnrOrgnMa90())
                .frgnrOrgnMa120(entity.getFrgnrOrgnMa120()).frgnrOrgnMa140(entity.getFrgnrOrgnMa140())
                // 개인투자자
                .indInvsrMa5(entity.getIndInvsrMa5()).indInvsrMa10(entity.getIndInvsrMa10())
                .indInvsrMa20(entity.getIndInvsrMa20()).indInvsrMa30(entity.getIndInvsrMa30())
                .indInvsrMa40(entity.getIndInvsrMa40()).indInvsrMa50(entity.getIndInvsrMa50())
                .indInvsrMa60(entity.getIndInvsrMa60()).indInvsrMa90(entity.getIndInvsrMa90())
                .indInvsrMa120(entity.getIndInvsrMa120()).indInvsrMa140(entity.getIndInvsrMa140())
                // 금융투자
                .fnncInvtMa5(entity.getFnncInvtMa5()).fnncInvtMa10(entity.getFnncInvtMa10())
                .fnncInvtMa20(entity.getFnncInvtMa20()).fnncInvtMa30(entity.getFnncInvtMa30())
                .fnncInvtMa40(entity.getFnncInvtMa40()).fnncInvtMa50(entity.getFnncInvtMa50())
                .fnncInvtMa60(entity.getFnncInvtMa60()).fnncInvtMa90(entity.getFnncInvtMa90())
                .fnncInvtMa120(entity.getFnncInvtMa120()).fnncInvtMa140(entity.getFnncInvtMa140())
                // 보험
                .insrncMa5(entity.getInsrncMa5()).insrncMa10(entity.getInsrncMa10())
                .insrncMa20(entity.getInsrncMa20()).insrncMa30(entity.getInsrncMa30())
                .insrncMa40(entity.getInsrncMa40()).insrncMa50(entity.getInsrncMa50())
                .insrncMa60(entity.getInsrncMa60()).insrncMa90(entity.getInsrncMa90())
                .insrncMa120(entity.getInsrncMa120()).insrncMa140(entity.getInsrncMa140())
                // 투신
                .invtrtMa5(entity.getInvtrtMa5()).invtrtMa10(entity.getInvtrtMa10())
                .invtrtMa20(entity.getInvtrtMa20()).invtrtMa30(entity.getInvtrtMa30())
                .invtrtMa40(entity.getInvtrtMa40()).invtrtMa50(entity.getInvtrtMa50())
                .invtrtMa60(entity.getInvtrtMa60()).invtrtMa90(entity.getInvtrtMa90())
                .invtrtMa120(entity.getInvtrtMa120()).invtrtMa140(entity.getInvtrtMa140())
                // 기타금융
                .etcFnncMa5(entity.getEtcFnncMa5()).etcFnncMa10(entity.getEtcFnncMa10())
                .etcFnncMa20(entity.getEtcFnncMa20()).etcFnncMa30(entity.getEtcFnncMa30())
                .etcFnncMa40(entity.getEtcFnncMa40()).etcFnncMa50(entity.getEtcFnncMa50())
                .etcFnncMa60(entity.getEtcFnncMa60()).etcFnncMa90(entity.getEtcFnncMa90())
                .etcFnncMa120(entity.getEtcFnncMa120()).etcFnncMa140(entity.getEtcFnncMa140())
                // 은행
                .bankMa5(entity.getBankMa5()).bankMa10(entity.getBankMa10())
                .bankMa20(entity.getBankMa20()).bankMa30(entity.getBankMa30())
                .bankMa40(entity.getBankMa40()).bankMa50(entity.getBankMa50())
                .bankMa60(entity.getBankMa60()).bankMa90(entity.getBankMa90())
                .bankMa120(entity.getBankMa120()).bankMa140(entity.getBankMa140())
                // 연기금등
                .penfndEtcMa5(entity.getPenfndEtcMa5()).penfndEtcMa10(entity.getPenfndEtcMa10())
                .penfndEtcMa20(entity.getPenfndEtcMa20()).penfndEtcMa30(entity.getPenfndEtcMa30())
                .penfndEtcMa40(entity.getPenfndEtcMa40()).penfndEtcMa50(entity.getPenfndEtcMa50())
                .penfndEtcMa60(entity.getPenfndEtcMa60()).penfndEtcMa90(entity.getPenfndEtcMa90())
                .penfndEtcMa120(entity.getPenfndEtcMa120()).penfndEtcMa140(entity.getPenfndEtcMa140())
                // 사모펀드
                .samoFundMa5(entity.getSamoFundMa5()).samoFundMa10(entity.getSamoFundMa10())
                .samoFundMa20(entity.getSamoFundMa20()).samoFundMa30(entity.getSamoFundMa30())
                .samoFundMa40(entity.getSamoFundMa40()).samoFundMa50(entity.getSamoFundMa50())
                .samoFundMa60(entity.getSamoFundMa60()).samoFundMa90(entity.getSamoFundMa90())
                .samoFundMa120(entity.getSamoFundMa120()).samoFundMa140(entity.getSamoFundMa140())
                // 국가
                .natnMa5(entity.getNatnMa5()).natnMa10(entity.getNatnMa10())
                .natnMa20(entity.getNatnMa20()).natnMa30(entity.getNatnMa30())
                .natnMa40(entity.getNatnMa40()).natnMa50(entity.getNatnMa50())
                .natnMa60(entity.getNatnMa60()).natnMa90(entity.getNatnMa90())
                .natnMa120(entity.getNatnMa120()).natnMa140(entity.getNatnMa140())
                // 기타법인
                .etcCorpMa5(entity.getEtcCorpMa5()).etcCorpMa10(entity.getEtcCorpMa10())
                .etcCorpMa20(entity.getEtcCorpMa20()).etcCorpMa30(entity.getEtcCorpMa30())
                .etcCorpMa40(entity.getEtcCorpMa40()).etcCorpMa50(entity.getEtcCorpMa50())
                .etcCorpMa60(entity.getEtcCorpMa60()).etcCorpMa90(entity.getEtcCorpMa90())
                .etcCorpMa120(entity.getEtcCorpMa120()).etcCorpMa140(entity.getEtcCorpMa140())
                // 내외국인
                .natforMa5(entity.getNatforMa5()).natforMa10(entity.getNatforMa10())
                .natforMa20(entity.getNatforMa20()).natforMa30(entity.getNatforMa30())
                .natforMa40(entity.getNatforMa40()).natforMa50(entity.getNatforMa50())
                .natforMa60(entity.getNatforMa60()).natforMa90(entity.getNatforMa90())
                .natforMa120(entity.getNatforMa120()).natforMa140(entity.getNatforMa140())
                .build();
        }
    }

    /**
     * 섹터 정보
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SectorInfo {
        private String sectorCd;
        private String sectorNm;
    }

    /**
     * 종목 정보 (REQ-004-1)
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockInfo {
        private String code;
        private String name;
    }

    /**
     * 섹터별 차트 데이터 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMaChartResponse {
        private String sectorCd;
        private String sectorNm;
        private Integer period;
        private List<SectorMaChartDataPoint> data;
        private String message;
    }

    /**
     * 섹터별 차트 데이터 포인트 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMaChartDataPoint {
        private String dt;
        private BigDecimal frgnr;
        private BigDecimal orgn;
        private BigDecimal fnncInvt;
        private BigDecimal insrnc;
        private BigDecimal invtrt;
        private BigDecimal etcFnnc;
        private BigDecimal bank;
        private BigDecimal penfndEtc;
        private BigDecimal samoFund;
        private BigDecimal natn;
        private BigDecimal etcCorp;
        private BigDecimal natfor;
        private BigDecimal indInvsr;
    }

    /**
     * 모든 섹터 차트 응답 (REQ-005)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllSectorMaChartResponse {
        private Map<String, SectorMaChartResponse> sectors;  // 섹터별 차트 데이터
        private String message;

        public static AllSectorMaChartResponse success(Map<String, SectorMaChartResponse> sectors) {
            return AllSectorMaChartResponse.builder()
                .sectors(sectors)
                .build();
        }

        public static AllSectorMaChartResponse error(String message) {
            return AllSectorMaChartResponse.builder()
                .message(message)
                .build();
        }
    }
}
