package com.stocktrading.kiwoom.adapter.out.persistence;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.domain.model.InvestorTrading;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * JPA Entity와 Domain Model 간 변환 Mapper
 */
@Component
public class InvestorTradingMapper {

    /**
     * Domain Model -> JPA Entity
     */
    public StockInvestorDaily toEntity(InvestorTrading domain) {
        return StockInvestorDaily.builder()
                .stkCd(domain.getStockCode())
                .dt(domain.getDate())
                .trdeTp(domain.getTradeType().getCode())
                .amtQtyTp(domain.getAmountQuantityType().getCode())
                .unitTp(domain.getUnitType().getCode())
                .curPrc(domain.getCurrentPrice())
                .preSig(domain.getPriceSign() != null ? domain.getPriceSign().getCode() : null)
                .predPre(domain.getPreviousDayDifference())
                .fluRt(domain.getFluctuationRate())
                .accTrdeQty(domain.getAccumulatedTradeQuantity())
                .accTrdePrica(domain.getAccumulatedTradeAmount())
                .indInvsr(domain.getInvestorBreakdown().getIndividual())
                .frgnrInvsr(domain.getInvestorBreakdown().getForeigner())
                .orgn(domain.getInvestorBreakdown().getInstitution())
                .fnncInvt(domain.getInvestorBreakdown().getFinancialInvest())
                .insrnc(domain.getInvestorBreakdown().getInsurance())
                .invtrt(domain.getInvestorBreakdown().getInvestment())
                .etcFnnc(domain.getInvestorBreakdown().getEtcFinancial())
                .bank(domain.getInvestorBreakdown().getBank())
                .penfndEtc(domain.getInvestorBreakdown().getPensionFund())
                .samoFund(domain.getInvestorBreakdown().getPrivateFund())
                .natn(domain.getInvestorBreakdown().getNation())
                .etcCorp(domain.getInvestorBreakdown().getEtcCorporation())
                .natfor(domain.getInvestorBreakdown().getNationalForeign())
                .regDt(LocalDateTime.now())
                .build();
    }

    /**
     * JPA Entity -> Domain Model
     */
    public InvestorTrading toDomain(StockInvestorDaily entity) {
        InvestorTrading.InvestorBreakdown breakdown = InvestorTrading.InvestorBreakdown.builder()
                .individual(entity.getIndInvsr())
                .foreigner(entity.getFrgnrInvsr())
                .institution(entity.getOrgn())
                .financialInvest(entity.getFnncInvt())
                .insurance(entity.getInsrnc())
                .investment(entity.getInvtrt())
                .etcFinancial(entity.getEtcFnnc())
                .bank(entity.getBank())
                .pensionFund(entity.getPenfndEtc())
                .privateFund(entity.getSamoFund())
                .nation(entity.getNatn())
                .etcCorporation(entity.getEtcCorp())
                .nationalForeign(entity.getNatfor())
                .build();

        return InvestorTrading.builder()
                .stockCode(entity.getStkCd())
                .date(entity.getDt())
                .tradeType(InvestorTrading.TradeType.fromCode(entity.getTrdeTp()))
                .amountQuantityType(InvestorTrading.AmountQuantityType.fromCode(entity.getAmtQtyTp()))
                .unitType(InvestorTrading.UnitType.fromCode(entity.getUnitTp()))
                .currentPrice(entity.getCurPrc())
                .priceSign(entity.getPreSig() != null ? InvestorTrading.PriceSign.fromCode(entity.getPreSig()) : null)
                .previousDayDifference(entity.getPredPre())
                .fluctuationRate(entity.getFluRt())
                .accumulatedTradeQuantity(entity.getAccTrdeQty())
                .accumulatedTradeAmount(entity.getAccTrdePrica())
                .investorBreakdown(breakdown)
                .build();
    }
}
