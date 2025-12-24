package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorInvestAccumulationEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorInvestAccumulationId;

@Repository
public interface StockInvestorInvestAccumulationRepository
        extends JpaRepository<StockInvestorInvestAccumulationEntity, StockInvestorInvestAccumulationId> {

    /**
     * 특정 종목의 가장 최근 데이터 날짜 조회
     */
    @Query("SELECT MAX(e.dt) FROM StockInvestorInvestAccumulationEntity e WHERE e.stkCd = :stkCd")
    Optional<LocalDate> findMaxDateByStkCd(@Param("stkCd") String stkCd);

    /**
     * 특정 종목의 가장 최근 데이터 전체 조회 (마지막 누적값 확인용)
     */
    Optional<StockInvestorInvestAccumulationEntity> findTopByStkCdOrderByDtDesc(String stkCd);

    /**
     * 기간별 조회
     */
    List<StockInvestorInvestAccumulationEntity> findByStkCdAndDtBetweenOrderByDtAsc(String stkCd, LocalDate startDt,
            LocalDate endDt);

    /**
     * 전체 기간 조회 (초기 적재용 등)
     */
    List<StockInvestorInvestAccumulationEntity> findByStkCdOrderByDtAsc(String stkCd);
}
