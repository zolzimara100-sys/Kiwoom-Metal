package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorSectorMaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorSectorMaId;

/**
 * 섹터별 투자자 이동평균 Repository
 */
@Repository
public interface StockInvestorSectorMaRepository extends JpaRepository<StockInvestorSectorMaEntity, StockInvestorSectorMaId> {

    /**
     * 섹터코드로 이동평균 데이터 조회 (날짜 오름차순)
     */
    List<StockInvestorSectorMaEntity> findBySectorCdOrderByDtAsc(String sectorCd);

    /**
     * 섹터코드와 날짜 범위로 이동평균 데이터 조회
     */
    @Query("SELECT e FROM StockInvestorSectorMaEntity e WHERE e.sectorCd = :sectorCd AND e.dt BETWEEN :startDt AND :endDt ORDER BY e.dt ASC")
    List<StockInvestorSectorMaEntity> findBySectorCdAndDtBetween(
            @Param("sectorCd") String sectorCd,
            @Param("startDt") String startDt,
            @Param("endDt") String endDt);

    /**
     * 최근 N일 데이터 조회 (차트용)
     */
    @Query(value = "SELECT * FROM tb_stock_investor_sector_ma WHERE sector_cd = :sectorCd ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
    List<StockInvestorSectorMaEntity> findRecentBySectorCd(
            @Param("sectorCd") String sectorCd,
            @Param("limit") int limit);

    /**
     * 특정 날짜 이전의 N일 데이터 조회 (무한 스크롤용)
     */
    @Query(value = "SELECT * FROM tb_stock_investor_sector_ma WHERE sector_cd = :sectorCd AND dt < :beforeDate ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
    List<StockInvestorSectorMaEntity> findBySectorCdBeforeDateOrderByDtDesc(
            @Param("sectorCd") String sectorCd,
            @Param("beforeDate") String beforeDate,
            @Param("limit") int limit);

    /**
     * 특정 섹터의 최신 날짜 조회 (max dt)
     */
    @Query(value = "SELECT max(dt) FROM tb_stock_investor_sector_ma WHERE sector_cd = :sectorCd", nativeQuery = true)
    String findMaxDateBySectorCd(@Param("sectorCd") String sectorCd);

    /**
     * 모든 섹터 코드 조회
     */
    @Query(value = "SELECT DISTINCT sector_cd FROM tb_stock_investor_sector_ma", nativeQuery = true)
    List<String> findAllSectorCodes();
}
