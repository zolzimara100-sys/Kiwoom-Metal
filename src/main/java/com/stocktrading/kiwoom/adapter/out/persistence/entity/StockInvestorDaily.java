package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 키움증권 투자자별 거래내역(ka10059) Entity
 * Table: tb_stock_investor_daily
 */
@Entity
@Table(name = "tb_stock_investor_daily")
@IdClass(StockInvestorDailyId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorDaily {

    // ============================================
    // 1. Primary Keys (데이터 식별자)
    // ============================================

    @Id
    @Column(name = "stk_cd", length = 10, nullable = false)
    private String stkCd;           // 종목코드 (예: 005930)

    @Id
    @Column(name = "dt", nullable = false)
    private LocalDate dt;           // 일자

    @Id
    @Column(name = "trde_tp", length = 1, nullable = false)
    private String trdeTp;          // 매매구분 (0:순매수, 1:매수, 2:매도)

    @Id
    @Column(name = "amt_qty_tp", length = 1, nullable = false)
    private String amtQtyTp;        // 금액수량구분 (1:금액, 2:수량)

    // ============================================
    // 2. Request Metadata (데이터 속성)
    // ============================================

    @Column(name = "unit_tp", length = 4, nullable = false)
    private String unitTp;          // 단위구분 (1:단주, 1000:천주)

    // ============================================
    // 3. Market Data (시세 및 거래 정보)
    // ============================================

    @Column(name = "cur_prc")
    private Long curPrc;            // 현재가 (부호 제거됨)

    @Column(name = "pre_sig", length = 1)
    private String preSig;          // 대비기호 (1:상한, 2:상승 등)

    @Column(name = "pred_pre")
    private Long predPre;           // 전일대비

    @Column(name = "flu_rt", precision = 5, scale = 2)
    private BigDecimal fluRt;       // 등락율 (예: 6.98)

    @Column(name = "acc_trde_qty")
    private Long accTrdeQty;        // 누적거래량

    @Column(name = "acc_trde_prica")
    private Long accTrdePrica;      // 누적거래대금

    // ============================================
    // 4. Investor Breakdown (투자자별 수치)
    // ============================================

    @Column(name = "ind_invsr")
    @Builder.Default
    private Long indInvsr = 0L;     // 개인투자자

    @Column(name = "frgnr_invsr")
    @Builder.Default
    private Long frgnrInvsr = 0L;   // 외국인투자자

    @Column(name = "orgn")
    @Builder.Default
    private Long orgn = 0L;         // 기관계

    @Column(name = "fnnc_invt")
    @Builder.Default
    private Long fnncInvt = 0L;     // 금융투자

    @Column(name = "insrnc")
    @Builder.Default
    private Long insrnc = 0L;       // 보험

    @Column(name = "invtrt")
    @Builder.Default
    private Long invtrt = 0L;       // 투신

    @Column(name = "etc_fnnc")
    @Builder.Default
    private Long etcFnnc = 0L;      // 기타금융

    @Column(name = "bank")
    @Builder.Default
    private Long bank = 0L;         // 은행

    @Column(name = "penfnd_etc")
    @Builder.Default
    private Long penfndEtc = 0L;    // 연기금등

    @Column(name = "samo_fund")
    @Builder.Default
    private Long samoFund = 0L;     // 사모펀드

    @Column(name = "natn")
    @Builder.Default
    private Long natn = 0L;         // 국가

    @Column(name = "etc_corp")
    @Builder.Default
    private Long etcCorp = 0L;      // 기타법인

    @Column(name = "natfor")
    @Builder.Default
    private Long natfor = 0L;       // 내외국인

    // ============================================
    // 5. System Audit (관리 정보)
    // ============================================

    @Column(name = "reg_dt", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime regDt = LocalDateTime.now();  // 데이터 적재 일시
}
