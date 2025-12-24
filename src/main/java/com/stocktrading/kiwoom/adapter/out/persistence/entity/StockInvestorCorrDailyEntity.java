package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * 투자자별 상관계수 분석 결과 엔티티
 * Table: tb_stock_investor_corr_daily
 */
@Entity
@Table(name = "tb_stock_investor_corr_daily", indexes = {
        @Index(name = "idx_corr_stk_dt", columnList = "stk_cd, dt")
})
@IdClass(StockInvestorCorrDailyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorCorrDailyEntity {

    @Id
    @Column(name = "stk_cd", length = 20)
    private String stkCd;

    @Id
    @Column(name = "dt", length = 8)
    private String dt;

    @Id
    @Column(name = "corr_days")
    private int corrDays;

    @Column(name = "cur_prc")
    private Long curPrc;

    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "category1", length = 50)
    private String category1;

    @Column(name = "category2", length = 50)
    private String category2;

    @Column(name = "category3", length = 50)
    private String category3;

    // --- Investor Correlations ---

    @Column(name = "frgnr_invsr_corr", precision = 5, scale = 3)
    private BigDecimal frgnrInvsrCorr;

    @Column(name = "orgn_corr", precision = 5, scale = 3)
    private BigDecimal orgnCorr;

    @Column(name = "ind_invsr_corr", precision = 5, scale = 3)
    private BigDecimal indInvsrCorr;

    @Column(name = "fnnc_invt_corr", precision = 5, scale = 3)
    private BigDecimal fnncInvtCorr;

    @Column(name = "insrnc_corr", precision = 5, scale = 3)
    private BigDecimal insrncCorr;

    @Column(name = "invtrt_corr", precision = 5, scale = 3)
    private BigDecimal invtrtCorr;

    @Column(name = "etc_fnnc_corr", precision = 5, scale = 3)
    private BigDecimal etcFnncCorr;

    @Column(name = "bank_corr", precision = 5, scale = 3)
    private BigDecimal bankCorr;

    @Column(name = "penfnd_etc_corr", precision = 5, scale = 3)
    private BigDecimal penfndEtcCorr;

    @Column(name = "samo_fund_corr", precision = 5, scale = 3)
    private BigDecimal samoFundCorr;

    @Column(name = "natn_corr", precision = 5, scale = 3)
    private BigDecimal natnCorr;

    @Column(name = "etc_corp_corr", precision = 5, scale = 3)
    private BigDecimal etcCorpCorr;

    @Column(name = "natfor_corr", precision = 5, scale = 3)
    private BigDecimal natforCorr;

    // --- Metadata ---

    @Column(name = "reg_dt", insertable = false, updatable = false)
    private LocalDateTime regDt;
}
