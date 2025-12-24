package com.stocktrading.kiwoom.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChart;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorChartRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorInvestAccumulationEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorInvestAccumulationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestorSupplyDemandService {

    private final StockInvestorChartRepository chartRepository;
    private final StockInvestorInvestAccumulationRepository accumulationRepository;

    /**
     * 기간별 투자자 분석 데이터 조회 (On-Demand Update 포함)
     */
    @Transactional
    public List<StockInvestorInvestAccumulationEntity> getAnalysisData(String stkCd, LocalDate startDt,
            LocalDate endDt) {
        // 1. Sync Data
        syncAccumulationData(stkCd);

        // 2. Fetch Data
        if (startDt == null)
            startDt = LocalDate.of(2000, 1, 1);
        if (endDt == null)
            endDt = LocalDate.now();

        return accumulationRepository.findByStkCdAndDtBetweenOrderByDtAsc(stkCd, startDt, endDt);
    }

    /**
     * 특정 종목 수급분석 계산 (API Trigger)
     */
    @Transactional
    public void calculate(String stkCd) {
        syncAccumulationData(stkCd);
    }

    /**
     * 전체 종목 수급분석 계산 (Batch Trigger)
     */
    public int calculateAll() {
        List<String> stockCodes = chartRepository.findDistinctStockCodes();
        int count = 0;
        for (String stkCd : stockCodes) {
            try {
                syncAccumulationData(stkCd);
                count++;
            } catch (Exception e) {
                log.error("Failed to sync supply/demand for {}", stkCd, e);
            }
        }
        return count;
    }

    /**
     * 데이터 동기화 (Initial Load or Delta Update)
     */
    private void syncAccumulationData(String stkCd) {
        LocalDate lastAccumDt = accumulationRepository.findMaxDateByStkCd(stkCd).orElse(null);
        LocalDate lastChartDt = chartRepository.findMaxDateByStock(stkCd).orElse(null);

        if (lastChartDt == null) {
            return; // No source data
        }

        if (lastAccumDt == null) {
            // Initial Load
            log.info("Initial load for stock: {}", stkCd);
            processAccumulation(stkCd, LocalDate.of(2000, 1, 1), lastChartDt, null);
        } else if (lastAccumDt.isBefore(lastChartDt)) {
            // Delta Update
            log.info("Delta update for stock: {} from {} to {}", stkCd, lastAccumDt, lastChartDt);
            // Fetch last accumulated state
            StockInvestorInvestAccumulationEntity lastEntity = accumulationRepository.findTopByStkCdOrderByDtDesc(stkCd)
                    .orElseThrow(() -> new IllegalStateException("Max date exists but entity not found"));

            processAccumulation(stkCd, lastAccumDt.plusDays(1), lastChartDt, lastEntity);
        }
    }

    private void processAccumulation(String stkCd, LocalDate startDt, LocalDate endDt,
            StockInvestorInvestAccumulationEntity previousState) {
        List<StockInvestorChart> sourceData = chartRepository.findByStkCdAndDtBetweenOrderByDtAsc(stkCd, startDt,
                endDt);

        List<StockInvestorInvestAccumulationEntity> newEntities = new ArrayList<>();

        Accumulator acc = new Accumulator(previousState);

        for (StockInvestorChart chart : sourceData) {
            acc.accumulate(chart);
            newEntities.add(acc.toEntity(chart));
        }

        accumulationRepository.saveAll(newEntities);
    }

    // Accumulator helper class
    private static class Accumulator {
        // 1. Qty
        long indInvsrNetBuyQty = 0;
        long frgnrInvsrNetBuyQty = 0;
        long orgnNetBuyQty = 0;
        long fnncInvtNetBuyQty = 0;
        long insrncNetBuyQty = 0;
        long invtrtNetBuyQty = 0;
        long etcFnncNetBuyQty = 0;
        long bankNetBuyQty = 0;
        long penfndEtcNetBuyQty = 0;
        long samoFundNetBuyQty = 0;
        long natnNetBuyQty = 0;
        long etcCorpNetBuyQty = 0;
        long natforNetBuyQty = 0;
        long frgnrInvsrOrgnNetBuyQty = 0;

        // 2. Amount
        long indInvsrNetBuyAmount = 0;
        long frgnrInvsrNetBuyAmount = 0;
        long orgnNetBuyAmount = 0;
        long fnncInvtNetBuyAmount = 0;
        long insrncNetBuyAmount = 0;
        long invtrtNetBuyAmount = 0;
        long etcFnncNetBuyAmount = 0;
        long bankNetBuyAmount = 0;
        long penfndEtcNetBuyAmount = 0;
        long samoFundNetBuyAmount = 0;
        long natnNetBuyAmount = 0;
        long etcCorpNetBuyAmount = 0;
        long natforNetBuyAmount = 0;
        long frgnrInvsrOrgnNetBuyAmount = 0;

        public Accumulator(StockInvestorInvestAccumulationEntity prev) {
            if (prev != null) {
                this.indInvsrNetBuyQty = prev.getIndInvsrNetBuyQty();
                this.frgnrInvsrNetBuyQty = prev.getFrgnrInvsrNetBuyQty();
                this.orgnNetBuyQty = prev.getOrgnNetBuyQty();
                this.fnncInvtNetBuyQty = prev.getFnncInvtNetBuyQty();
                this.insrncNetBuyQty = prev.getInsrncNetBuyQty();
                this.invtrtNetBuyQty = prev.getInvtrtNetBuyQty();
                this.etcFnncNetBuyQty = prev.getEtcFnncNetBuyQty();
                this.bankNetBuyQty = prev.getBankNetBuyQty();
                this.penfndEtcNetBuyQty = prev.getPenfndEtcNetBuyQty();
                this.samoFundNetBuyQty = prev.getSamoFundNetBuyQty();
                this.natnNetBuyQty = prev.getNatnNetBuyQty();
                this.etcCorpNetBuyQty = prev.getEtcCorpNetBuyQty();
                this.natforNetBuyQty = prev.getNatforNetBuyQty();
                this.frgnrInvsrOrgnNetBuyQty = prev.getFrgnrInvsrOrgnNetBuyQty();

                this.indInvsrNetBuyAmount = prev.getIndInvsrNetBuyAmount();
                this.frgnrInvsrNetBuyAmount = prev.getFrgnrInvsrNetBuyAmount();
                this.orgnNetBuyAmount = prev.getOrgnNetBuyAmount();
                this.fnncInvtNetBuyAmount = prev.getFnncInvtNetBuyAmount();
                this.insrncNetBuyAmount = prev.getInsrncNetBuyAmount();
                this.invtrtNetBuyAmount = prev.getInvtrtNetBuyAmount();
                this.etcFnncNetBuyAmount = prev.getEtcFnncNetBuyAmount();
                this.bankNetBuyAmount = prev.getBankNetBuyAmount();
                this.penfndEtcNetBuyAmount = prev.getPenfndEtcNetBuyAmount();
                this.samoFundNetBuyAmount = prev.getSamoFundNetBuyAmount();
                this.natnNetBuyAmount = prev.getNatnNetBuyAmount();
                this.etcCorpNetBuyAmount = prev.getEtcCorpNetBuyAmount();
                this.natforNetBuyAmount = prev.getNatforNetBuyAmount();
                this.frgnrInvsrOrgnNetBuyAmount = prev.getFrgnrInvsrOrgnNetBuyAmount();
            }
        }

        public void accumulate(StockInvestorChart c) {
            long price = c.getCurPrc() != null ? c.getCurPrc() : 0L;

            // Qty
            accumulate(c.getIndInvsr(), price, (q, p) -> {
                indInvsrNetBuyQty += q;
                indInvsrNetBuyAmount += (q * p);
            });
            accumulate(c.getFrgnrInvsr(), price, (q, p) -> {
                frgnrInvsrNetBuyQty += q;
                frgnrInvsrNetBuyAmount += (q * p);
            });
            accumulate(c.getOrgn(), price, (q, p) -> {
                orgnNetBuyQty += q;
                orgnNetBuyAmount += (q * p);
            });
            accumulate(c.getFnncInvt(), price, (q, p) -> {
                fnncInvtNetBuyQty += q;
                fnncInvtNetBuyAmount += (q * p);
            });
            accumulate(c.getInsrnc(), price, (q, p) -> {
                insrncNetBuyQty += q;
                insrncNetBuyAmount += (q * p);
            });
            accumulate(c.getInvtrt(), price, (q, p) -> {
                invtrtNetBuyQty += q;
                invtrtNetBuyAmount += (q * p);
            });
            accumulate(c.getEtcFnnc(), price, (q, p) -> {
                etcFnncNetBuyQty += q;
                etcFnncNetBuyAmount += (q * p);
            });
            accumulate(c.getBank(), price, (q, p) -> {
                bankNetBuyQty += q;
                bankNetBuyAmount += (q * p);
            });
            accumulate(c.getPenfndEtc(), price, (q, p) -> {
                penfndEtcNetBuyQty += q;
                penfndEtcNetBuyAmount += (q * p);
            });
            accumulate(c.getSamoFund(), price, (q, p) -> {
                samoFundNetBuyQty += q;
                samoFundNetBuyAmount += (q * p);
            });
            accumulate(c.getNatn(), price, (q, p) -> {
                natnNetBuyQty += q;
                natnNetBuyAmount += (q * p);
            });
            accumulate(c.getEtcCorp(), price, (q, p) -> {
                etcCorpNetBuyQty += q;
                etcCorpNetBuyAmount += (q * p);
            });
            accumulate(c.getNatfor(), price, (q, p) -> {
                natforNetBuyQty += q;
                natforNetBuyAmount += (q * p);
            });

            // Foreign + Organ
            long combinedQty = (c.getFrgnrInvsr() != null ? c.getFrgnrInvsr() : 0)
                    + (c.getOrgn() != null ? c.getOrgn() : 0);
            frgnrInvsrOrgnNetBuyQty += combinedQty;
            frgnrInvsrOrgnNetBuyAmount += (combinedQty * price);
        }

        private interface UpdateOp {
            void accept(long q, long p);
        }

        private void accumulate(Long dailyQty, long price, UpdateOp op) {
            long q = dailyQty != null ? dailyQty : 0L;
            op.accept(q, price);
        }

        public StockInvestorInvestAccumulationEntity toEntity(StockInvestorChart c) {
            StockInvestorInvestAccumulationEntity e = new StockInvestorInvestAccumulationEntity();
            e.setStkCd(c.getStkCd());
            e.setDt(c.getDt());
            e.setCurPrc(c.getCurPrc());

            // Daily Raw Copy
            e.setIndInvsr(c.getIndInvsr());
            e.setFrgnrInvsr(c.getFrgnrInvsr());
            e.setOrgn(c.getOrgn());
            e.setFnncInvt(c.getFnncInvt());
            e.setInsrnc(c.getInsrnc());
            e.setInvtrt(c.getInvtrt());
            e.setEtcFnnc(c.getEtcFnnc());
            e.setBank(c.getBank());
            e.setPenfndEtc(c.getPenfndEtc());
            e.setSamoFund(c.getSamoFund());
            e.setNatn(c.getNatn());
            e.setEtcCorp(c.getEtcCorp());
            e.setNatfor(c.getNatfor());
            e.setFrgnrInvsrOrgn(
                    (c.getFrgnrInvsr() != null ? c.getFrgnrInvsr() : 0) + (c.getOrgn() != null ? c.getOrgn() : 0));

            // Cumulative Qty
            e.setIndInvsrNetBuyQty(indInvsrNetBuyQty);
            e.setFrgnrInvsrNetBuyQty(frgnrInvsrNetBuyQty);
            e.setOrgnNetBuyQty(orgnNetBuyQty);
            e.setFnncInvtNetBuyQty(fnncInvtNetBuyQty);
            e.setInsrncNetBuyQty(insrncNetBuyQty);
            e.setInvtrtNetBuyQty(invtrtNetBuyQty);
            e.setEtcFnncNetBuyQty(etcFnncNetBuyQty);
            e.setBankNetBuyQty(bankNetBuyQty);
            e.setPenfndEtcNetBuyQty(penfndEtcNetBuyQty);
            e.setSamoFundNetBuyQty(samoFundNetBuyQty);
            e.setNatnNetBuyQty(natnNetBuyQty);
            e.setEtcCorpNetBuyQty(etcCorpNetBuyQty);
            e.setNatforNetBuyQty(natforNetBuyQty);
            e.setFrgnrInvsrOrgnNetBuyQty(frgnrInvsrOrgnNetBuyQty);

            // Cumulative Amount
            e.setIndInvsrNetBuyAmount(indInvsrNetBuyAmount);
            e.setFrgnrInvsrNetBuyAmount(frgnrInvsrNetBuyAmount);
            e.setOrgnNetBuyAmount(orgnNetBuyAmount);
            e.setFnncInvtNetBuyAmount(fnncInvtNetBuyAmount);
            e.setInsrncNetBuyAmount(insrncNetBuyAmount);
            e.setInvtrtNetBuyAmount(invtrtNetBuyAmount);
            e.setEtcFnncNetBuyAmount(etcFnncNetBuyAmount);
            e.setBankNetBuyAmount(bankNetBuyAmount);
            e.setPenfndEtcNetBuyAmount(penfndEtcNetBuyAmount);
            e.setSamoFundNetBuyAmount(samoFundNetBuyAmount);
            e.setNatnNetBuyAmount(natnNetBuyAmount);
            e.setEtcCorpNetBuyAmount(etcCorpNetBuyAmount);
            e.setNatforNetBuyAmount(natforNetBuyAmount);
            e.setFrgnrInvsrOrgnNetBuyAmount(frgnrInvsrOrgnNetBuyAmount);

            return e;
        }
    }
}
