package com.stocktrading.kiwoom.adapter.out.persistence;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorDailyRepository;
import com.stocktrading.kiwoom.domain.model.InvestorTrading;
import com.stocktrading.kiwoom.domain.port.out.InvestorTradingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 투자자 거래 데이터 Persistence Adapter
 * InvestorTradingPort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestorTradingPersistenceAdapter implements InvestorTradingPort {

    private final StockInvestorDailyRepository repository;
    private final InvestorTradingMapper mapper;

    @Override
    public InvestorTrading save(InvestorTrading trading) {
        StockInvestorDaily entity = mapper.toEntity(trading);
        StockInvestorDaily saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<InvestorTrading> saveAll(List<InvestorTrading> tradings) {
        List<StockInvestorDaily> entities = tradings.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        List<StockInvestorDaily> saved = repository.saveAll(entities);

        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorTrading> findByStockAndDate(String stockCode, LocalDate date) {
        return repository.findByStkCdAndDt(stockCode, date).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorTrading> findByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate) {
        return repository.findByStkCdAndDtBetween(stockCode, startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorTrading> findByDate(LocalDate date) {
        return repository.findByDt(date).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public InvestorTrading findLatestByStock(String stockCode) {
        List<StockInvestorDaily> entities = repository.findLatestByStkCd(stockCode);
        if (entities.isEmpty()) {
            return null;
        }
        return mapper.toDomain(entities.get(0));
    }

    @Override
    public boolean exists(String stockCode, LocalDate date, String tradeType, String amountQuantityType) {
        return repository.existsByStkCdAndDtAndTrdeTpAndAmtQtyTp(stockCode, date, tradeType, amountQuantityType);
    }
}
