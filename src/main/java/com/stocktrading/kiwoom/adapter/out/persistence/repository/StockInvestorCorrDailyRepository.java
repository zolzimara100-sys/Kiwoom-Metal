package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorCorrDailyEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorCorrDailyId;

@Repository
public interface StockInvestorCorrDailyRepository
                extends JpaRepository<StockInvestorCorrDailyEntity, StockInvestorCorrDailyId> {

        /**
         * 특정 종목, 기간의 최근 데이터 조회 (Limit 적용)
         * NaN 값을 NULL로 변환하여 조회
         */
        @Query(value = "SELECT stk_cd, dt, corr_days, cur_prc, sector, category1, category2, category3, " +
                       "NULLIF(frgnr_invsr_corr, 'NaN')::numeric as frgnr_invsr_corr, " +
                       "NULLIF(orgn_corr, 'NaN')::numeric as orgn_corr, " +
                       "NULLIF(ind_invsr_corr, 'NaN')::numeric as ind_invsr_corr, " +
                       "NULLIF(fnnc_invt_corr, 'NaN')::numeric as fnnc_invt_corr, " +
                       "NULLIF(insrnc_corr, 'NaN')::numeric as insrnc_corr, " +
                       "NULLIF(invtrt_corr, 'NaN')::numeric as invtrt_corr, " +
                       "NULLIF(etc_fnnc_corr, 'NaN')::numeric as etc_fnnc_corr, " +
                       "NULLIF(bank_corr, 'NaN')::numeric as bank_corr, " +
                       "NULLIF(penfnd_etc_corr, 'NaN')::numeric as penfnd_etc_corr, " +
                       "NULLIF(samo_fund_corr, 'NaN')::numeric as samo_fund_corr, " +
                       "NULLIF(natn_corr, 'NaN')::numeric as natn_corr, " +
                       "NULLIF(etc_corp_corr, 'NaN')::numeric as etc_corp_corr, " +
                       "NULLIF(natfor_corr, 'NaN')::numeric as natfor_corr, " +
                       "reg_dt " +
                       "FROM tb_stock_investor_corr_daily " +
                       "WHERE stk_cd = :stkCd AND corr_days = :corrDays " +
                       "ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
        List<StockInvestorCorrDailyEntity> findRecentByStkCdAndCorrDays(
                        @Param("stkCd") String stkCd,
                        @Param("corrDays") int corrDays,
                        @Param("limit") int limit);

        /**
         * 특정 날짜 이전의 데이터 조회 (무한 스크롤)
         * NaN 값을 NULL로 변환하여 조회
         */
        @Query(value = "SELECT stk_cd, dt, corr_days, cur_prc, sector, category1, category2, category3, " +
                       "NULLIF(frgnr_invsr_corr, 'NaN')::numeric as frgnr_invsr_corr, " +
                       "NULLIF(orgn_corr, 'NaN')::numeric as orgn_corr, " +
                       "NULLIF(ind_invsr_corr, 'NaN')::numeric as ind_invsr_corr, " +
                       "NULLIF(fnnc_invt_corr, 'NaN')::numeric as fnnc_invt_corr, " +
                       "NULLIF(insrnc_corr, 'NaN')::numeric as insrnc_corr, " +
                       "NULLIF(invtrt_corr, 'NaN')::numeric as invtrt_corr, " +
                       "NULLIF(etc_fnnc_corr, 'NaN')::numeric as etc_fnnc_corr, " +
                       "NULLIF(bank_corr, 'NaN')::numeric as bank_corr, " +
                       "NULLIF(penfnd_etc_corr, 'NaN')::numeric as penfnd_etc_corr, " +
                       "NULLIF(samo_fund_corr, 'NaN')::numeric as samo_fund_corr, " +
                       "NULLIF(natn_corr, 'NaN')::numeric as natn_corr, " +
                       "NULLIF(etc_corp_corr, 'NaN')::numeric as etc_corp_corr, " +
                       "NULLIF(natfor_corr, 'NaN')::numeric as natfor_corr, " +
                       "reg_dt " +
                       "FROM tb_stock_investor_corr_daily " +
                       "WHERE stk_cd = :stkCd AND corr_days = :corrDays AND dt < :beforeDate " +
                       "ORDER BY dt DESC LIMIT :limit", nativeQuery = true)
        List<StockInvestorCorrDailyEntity> findByStkCdAndCorrDaysBeforeDate(
                        @Param("stkCd") String stkCd,
                        @Param("corrDays") int corrDays,
                        @Param("beforeDate") String beforeDate,
                        @Param("limit") int limit);

        /**
         * 특정 종목의 최신 날짜 조회 (max dt)
         */
        @Query(value = "SELECT max(dt) FROM tb_stock_investor_corr_daily WHERE stk_cd = :stkCd", nativeQuery = true)
        String findMaxDateByStkCd(@Param("stkCd") String stkCd);
}
