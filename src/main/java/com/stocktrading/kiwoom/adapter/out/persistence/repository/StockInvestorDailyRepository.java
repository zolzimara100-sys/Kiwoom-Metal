package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 투자자별 거래내역 Repository
 */
@Repository
public interface StockInvestorDailyRepository extends JpaRepository<StockInvestorDaily, StockInvestorDailyId> {

    /**
     * 특정 종목의 특정 기간 데이터 조회
     */
    List<StockInvestorDaily> findByStkCdAndDtBetween(String stkCd, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 일자의 모든 종목 데이터 조회
     */
    List<StockInvestorDaily> findByDt(LocalDate dt);

    /**
     * 특정 종목의 특정 일자 데이터 조회
     */
    List<StockInvestorDaily> findByStkCdAndDt(String stkCd, LocalDate dt);

    /**
     * 특정 기간의 모든 데이터 조회
     */
    List<StockInvestorDaily> findByDtBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 종목의 최신 데이터 조회
     */
    @Query("SELECT s FROM StockInvestorDaily s WHERE s.stkCd = :stkCd ORDER BY s.dt DESC")
    List<StockInvestorDaily> findLatestByStkCd(@Param("stkCd") String stkCd);

    /**
     * 특정 일자, 특정 매매구분의 데이터 조회
     */
    List<StockInvestorDaily> findByDtAndTrdeTp(LocalDate dt, String trdeTp);

    /**
     * 데이터 존재 여부 확인 (중복 방지용)
     */
    boolean existsByStkCdAndDtAndTrdeTpAndAmtQtyTp(String stkCd, LocalDate dt, String trdeTp, String amtQtyTp);
}
