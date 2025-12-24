package com.stocktrading.kiwoom.domain.port.out;

import com.stocktrading.kiwoom.domain.model.StockInfo;

import java.util.List;
import java.util.Optional;

/**
 * 종목 리스트 저장소 포트
 */
public interface StockListPort {

    /**
     * 전체 종목 정보 저장 (Batch Insert)
     *
     * @param stocks 종목 리스트
     * @return 저장된 건수
     */
    int saveAll(List<StockInfo> stocks);

    /**
     * 전체 종목 정보 삭제
     *
     * @return 삭제된 건수
     */
    int deleteAll();

    /**
     * 종목코드로 조회
     *
     * @param code 종목코드
     * @return 종목 정보
     */
    Optional<StockInfo> findByCode(String code);

    /**
     * 시장구분으로 조회
     *
     * @param marketCode 시장구분코드
     * @return 종목 리스트
     */
    List<StockInfo> findByMarketCode(String marketCode);

    /**
     * 전체 종목 수 조회
     *
     * @return 전체 종목 수
     */
    long count();

    /**
     * 시장별 종목 수 조회
     *
     * @param marketCode 시장구분코드
     * @return 종목 수
     */
    long countByMarketCode(String marketCode);

    /**
     * 종목명으로 검색 (LIKE 검색)
     *
     * @param keyword 검색 키워드
     * @return 종목 리스트
     */
    List<StockInfo> searchByName(String keyword);

    /**
     * KOSPI200 종목 리스트 조회 (tb_stock_list_meta에서)
     *
     * @return KOSPI200 종목 리스트
     */
    List<StockInfo> findAllKospi200Stocks();
}
