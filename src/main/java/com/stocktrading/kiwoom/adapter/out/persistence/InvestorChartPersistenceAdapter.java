package com.stocktrading.kiwoom.adapter.out.persistence;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChart;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorChartRepository;
import com.stocktrading.kiwoom.domain.model.InvestorChart;
import com.stocktrading.kiwoom.domain.port.out.InvestorChartPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 투자자 기관별 차트 Persistence Adapter
 * InvestorChartPort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestorChartPersistenceAdapter implements InvestorChartPort {

    private final StockInvestorChartRepository repository;
    private final InvestorChartMapper mapper;

    @Override
    @Transactional
    public InvestorChart save(InvestorChart chart) {
        StockInvestorChart entity = mapper.toEntity(chart);

        // Upsert: 기존 데이터가 있으면 업데이트
        Optional<StockInvestorChart> existing = repository.findByStkCdAndDt(
                chart.getStockCode(), chart.getDate());

        if (existing.isPresent()) {
            StockInvestorChart existingEntity = existing.get();
            updateEntity(existingEntity, entity);
            StockInvestorChart saved = repository.save(existingEntity);
            log.debug("투자자 차트 데이터 업데이트 - 종목: {}, 일자: {}",
                    chart.getStockCode(), chart.getDate());
            return mapper.toDomain(saved);
        }

        StockInvestorChart saved = repository.save(entity);
        log.debug("투자자 차트 데이터 저장 - 종목: {}, 일자: {}",
                chart.getStockCode(), chart.getDate());
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<InvestorChart> saveAll(List<InvestorChart> charts) {
        return charts.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InvestorChart> findByStockAndDate(String stockCode, LocalDate date) {
        return repository.findByStkCdAndDt(stockCode, date)
                .map(mapper::toDomain);
    }

    @Override
    public List<InvestorChart> findByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate) {
        return repository.findByStkCdAndDtBetweenOrderByDtAsc(stockCode, startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorChart> findByDate(LocalDate date) {
        return repository.findByDt(date)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InvestorChart> findLatestByStock(String stockCode) {
        return repository.findFirstByStkCdOrderByDtDesc(stockCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<InvestorChart> findForeignerInstitutionBuyStocks(LocalDate date) {
        return repository.findForeignerInstitutionBuyStocks(date)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorChart> findPensionFundBuyStocks(LocalDate date) {
        return repository.findPensionFundBuyStocks(date)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByStockAndDate(String stockCode, LocalDate date) {
        return repository.existsByStkCdAndDt(stockCode, date);
    }

    @Override
    @Transactional
    public void deleteByStockAndDate(String stockCode, LocalDate date) {
        repository.deleteByStkCdAndDt(stockCode, date);
        log.debug("투자자 차트 데이터 삭제 - 종목: {}, 일자: {}", stockCode, date);
    }

    @Override
    public DateRange findDateRangeByStock(String stockCode) {
        LocalDate minDate = repository.findMinDateByStock(stockCode).orElse(null);
        LocalDate maxDate = repository.findMaxDateByStock(stockCode).orElse(null);
        return new DateRange(minDate, maxDate);
    }

    @Override
    public java.util.Map<String, DateRange> findDateRangesByStocks(java.util.List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        
        java.util.List<Object[]> results = repository.findDateRangesByStocks(stockCodes);
        java.util.Map<String, DateRange> map = new java.util.HashMap<>();
        
        for (Object[] row : results) {
            String stkCd = (String) row[0];
            java.time.LocalDate minDate = (java.time.LocalDate) row[1];
            java.time.LocalDate maxDate = (java.time.LocalDate) row[2];
            map.put(stkCd, new DateRange(minDate, maxDate));
        }
        
        log.info("종목별 날짜 범위 일괄 조회 완료 - 요청: {}개, 결과: {}개", stockCodes.size(), map.size());
        return map;
    }

    /**
     * 기존 Entity 업데이트
     */
    private void updateEntity(StockInvestorChart existing, StockInvestorChart updated) {
        existing.setCurPrc(updated.getCurPrc());
        existing.setPredPre(updated.getPredPre());
        existing.setAccTrdePrica(updated.getAccTrdePrica());
        existing.setIndInvsr(updated.getIndInvsr());
        existing.setFrgnrInvsr(updated.getFrgnrInvsr());
        existing.setOrgn(updated.getOrgn());
        existing.setNatfor(updated.getNatfor());
        existing.setFnncInvt(updated.getFnncInvt());
        existing.setInsrnc(updated.getInsrnc());
        existing.setInvtrt(updated.getInvtrt());
        existing.setEtcFnnc(updated.getEtcFnnc());
        existing.setBank(updated.getBank());
        existing.setPenfndEtc(updated.getPenfndEtc());
        existing.setSamoFund(updated.getSamoFund());
        existing.setNatn(updated.getNatn());
        existing.setEtcCorp(updated.getEtcCorp());
    }
}
