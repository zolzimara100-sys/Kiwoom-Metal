package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorMaEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorMaId;

/**
 * 투자자 이동평균 Repository
 */
@Repository
public interface StockInvestorMaRepository extends JpaRepository<StockInvestorMaEntity, StockInvestorMaId> {

        /**
         * 종목코드로 이동평균 데이터 조회 (날짜 오름차순)
         */
        List<StockInvestorMaEntity> findByStkCdOrderByDtAsc(String stkCd);

        /**
         * 종목코드와 날짜 범위로 이동평균 데이터 조회
         */
        @Query("SELECT e FROM StockInvestorMaEntity e WHERE e.stkCd = :stkCd AND e.dt BETWEEN :startDt AND :endDt ORDER BY e.dt ASC")
        List<StockInvestorMaEntity> findByStkCdAndDtBetween(
                        @Param("stkCd") String stkCd,
                        @Param("startDt") String startDt,
                        @Param("endDt") String endDt);

        /**
         * 최근 N일 데이터 조회 (차트용)
         */
        @Query(value = "SELECT * FROM tb_stock_investor_ma WHERE stk_cd = :stkCd ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
        List<StockInvestorMaEntity> findRecentByStkCd(
                        @Param("stkCd") String stkCd,
                        @Param("limit") int limit);

        /**
         * 특정 날짜 이전의 N일 데이터 조회 (무한 스크롤용)
         */
        @Query(value = "SELECT * FROM tb_stock_investor_ma WHERE stk_cd = :stkCd AND dt < :beforeDate ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
        List<StockInvestorMaEntity> findByStkCdBeforeDateOrderByDtDesc(
                        @Param("stkCd") String stkCd,
                        @Param("beforeDate") String beforeDate,
                        @Param("limit") int limit);

        /**
         * 특정 종목의 최신 날짜 조회 (max dt)
         */
        @Query(value = "SELECT max(dt) FROM tb_stock_investor_ma WHERE stk_cd = :stkCd", nativeQuery = true)
        String findMaxDateByStkCd(@Param("stkCd") String stkCd);
}
