package com.stocktrading.kiwoom.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChart;
import com.stocktrading.kiwoom.domain.model.InvestorChart;

/**
 * InvestorChart 도메인 모델 ↔ Entity 변환 Mapper
 */
@Component
public class InvestorChartMapper {

        /**
         * 도메인 모델 → Entity
         */
        public StockInvestorChart toEntity(InvestorChart domain) {
                return StockInvestorChart.builder()
                                .stkCd(domain.getStockCode())
                                .dt(domain.getDate())

                                .unitTp(domain.getUnitType().getCode())
                                // 시세 정보
                                .curPrc(domain.getCurrentPrice())
                                .predPre(domain.getPreviousDayDifference())
                                .accTrdePrica(domain.getAccumulatedTradeAmount())
                                // 투자자별 데이터
                                .indInvsr(domain.getInvestorData().getIndividual())
                                .frgnrInvsr(domain.getInvestorData().getForeigner())
                                .orgn(domain.getInvestorData().getInstitution())
                                .natfor(domain.getInvestorData().getNationalForeign())
                                // 기관 세부 내역
                                .fnncInvt(domain.getInstitutionBreakdown().getFinancialInvest())
                                .insrnc(domain.getInstitutionBreakdown().getInsurance())
                                .invtrt(domain.getInstitutionBreakdown().getInvestment())
                                .etcFnnc(domain.getInstitutionBreakdown().getEtcFinancial())
                                .bank(domain.getInstitutionBreakdown().getBank())
                                .penfndEtc(domain.getInstitutionBreakdown().getPensionFund())
                                .samoFund(domain.getInstitutionBreakdown().getPrivateFund())
                                .natn(domain.getInstitutionBreakdown().getNation())
                                .etcCorp(domain.getInstitutionBreakdown().getEtcCorporation())
                                .build();
        }

        /**
         * Entity → 도메인 모델
         */
        public InvestorChart toDomain(StockInvestorChart entity) {
                return InvestorChart.builder()
                                .stockCode(entity.getStkCd())
                                .date(entity.getDt())

                                .unitType(InvestorChart.UnitType.fromCode(entity.getUnitTp()))
                                // 시세 정보
                                .currentPrice(entity.getCurPrc())
                                .previousDayDifference(entity.getPredPre())
                                .accumulatedTradeAmount(entity.getAccTrdePrica())
                                // 투자자별 데이터
                                .investorData(InvestorChart.InvestorData.builder()
                                                .individual(entity.getIndInvsr())
                                                .foreigner(entity.getFrgnrInvsr())
                                                .institution(entity.getOrgn())
                                                .nationalForeign(entity.getNatfor())
                                                .build())
                                // 기관 세부 내역
                                .institutionBreakdown(InvestorChart.InstitutionBreakdown.builder()
                                                .financialInvest(entity.getFnncInvt())
                                                .insurance(entity.getInsrnc())
                                                .investment(entity.getInvtrt())
                                                .etcFinancial(entity.getEtcFnnc())
                                                .bank(entity.getBank())
                                                .pensionFund(entity.getPenfndEtc())
                                                .privateFund(entity.getSamoFund())
                                                .nation(entity.getNatn())
                                                .etcCorporation(entity.getEtcCorp())
                                                .build())
                                .build();
        }
}
