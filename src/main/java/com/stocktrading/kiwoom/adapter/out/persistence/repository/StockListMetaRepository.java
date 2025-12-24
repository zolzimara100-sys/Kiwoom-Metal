package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListMetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * KOSPI200 종목 메타 정보 Repository
 * tb_stock_list_meta 테이블 조회
 */
@Repository
public interface StockListMetaRepository extends JpaRepository<StockListMetaEntity, String> {

    /**
     * 특정 섹터에 속한 종목 목록 조회 (REQ-004-1)
     * @param sectorCd 섹터 코드 (예: semicon, ai_infra 등)
     * @return 해당 섹터의 종목 목록
     */
    @Query("SELECT s FROM StockListMetaEntity s WHERE s.sector = :sectorCd ORDER BY s.code")
    List<StockListMetaEntity> findBySector(@Param("sectorCd") String sectorCd);
}
