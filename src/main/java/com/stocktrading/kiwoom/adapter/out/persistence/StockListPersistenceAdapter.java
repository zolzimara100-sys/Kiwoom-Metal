package com.stocktrading.kiwoom.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockListMetaRepository;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockListRepository;
import com.stocktrading.kiwoom.domain.model.StockInfo;
import com.stocktrading.kiwoom.domain.port.out.StockListPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 종목 리스트 Persistence Adapter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockListPersistenceAdapter implements StockListPort {

    private final StockListRepository repository;
    private final StockListMetaRepository metaRepository;
    private final StockListMapper mapper;

    @Override
    @Transactional
    public int saveAll(List<StockInfo> stocks) {
        log.info("종목 리스트 Batch 저장 시작 - {}건", stocks.size());

        // 요구사항: tb_stock_list는 입력 전에 전체 삭제 (TRUNCATE)
        log.info("tb_stock_list 전체 삭제 후 신규 데이터 저장 - 기존 데이터 제거 시작");
        repository.truncate();
        log.info("tb_stock_list 전체 삭제 완료");

        List<com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListEntity> entities = stocks.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        List<com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListEntity> saved = repository
                .saveAll(entities);

        log.info("종목 리스트 Batch 저장 완료 - {}건", saved.size());
        return saved.size();
    }

    @Override
    @Transactional
    public int deleteAll() {
        log.info("종목 리스트 전체 삭제 시작");
        long count = repository.count();
        repository.truncate();
        log.info("종목 리스트 전체 삭제 완료 - {}건 삭제됨", count);
        return (int) count;
    }

    @Override
    public Optional<StockInfo> findByCode(String code) {
        return repository.findById(code)
                .map(mapper::toDomain);
    }

    @Override
    public List<StockInfo> findByMarketCode(String marketCode) {
        return repository.findByMarketCode(marketCode).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByMarketCode(String marketCode) {
        return repository.countByMarketCode(marketCode);
    }

    @Override
    public List<StockInfo> searchByName(String keyword) {
        // 시장명 우선 정렬 적용 (KOSPI, KOSDAQ, KONEX, 기타)
        List<com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListEntity> entities = repository
                .searchOrderedByMarket(keyword);
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockInfo> findAllKospi200Stocks() {
        log.info("KOSPI200 종목 리스트 조회 - tb_stock_list_meta");
        List<com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListMetaEntity> metaEntities = metaRepository
                .findAll();

        // StockListMetaEntity -> StockInfo 변환 (code와 name만 설정)
        List<StockInfo> stocks = metaEntities.stream()
                .map(entity -> StockInfo.builder()
                        .code(entity.getCode())
                        .name(entity.getName())
                        .sector(entity.getSector())
                        .build())
                .collect(Collectors.toList());

        log.info("KOSPI200 종목 리스트 조회 완료 - {}건", stocks.size());
        return stocks;
    }
}
