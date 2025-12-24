package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 종목 리스트 Repository
 */
@Repository
public interface StockListRepository extends JpaRepository<StockListEntity, String> {

    /**
     * 시장구분코드로 조회
     */
    List<StockListEntity> findByMarketCode(String marketCode);

    /**
     * 시장구분코드별 건수 조회
     */
    long countByMarketCode(String marketCode);

    /**
     * 전체 삭제 (Native Query로 최적화)
     */
    @Modifying
    @Query(value = "TRUNCATE TABLE tb_stock_list", nativeQuery = true)
    void truncate();

    /**
     * 종목명으로 LIKE 검색
     */
    List<StockListEntity> findByNameContaining(String name);

    /**
     * 종목명 LIKE 검색 + 시장명 우선 정렬 (거래소: KOSPI, KOSDAQ, KONEX 먼저, ELW는 그 다음, 기타) 후 종목명 정렬
     * 결과를 화면에서 '거래소' 우선으로 보여주기 위함
     */
        @Query("SELECT s FROM StockListEntity s WHERE s.name LIKE %:name% ORDER BY " +
            "CASE s.marketName " +
            "WHEN 'KOSPI' THEN 1 " +
            "WHEN 'KOSDAQ' THEN 2 " +
            "WHEN 'KONEX' THEN 3 " +
            "WHEN 'ELW' THEN 4 " +
            "ELSE 5 END, s.name ASC")
    List<StockListEntity> searchOrderedByMarket(@org.springframework.data.repository.query.Param("name") String name);
}
