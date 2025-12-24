package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 종목별 투자자 기관별 차트 Entity (ka10060)
 * Table: tb_stock_investor_chart
 */
@Entity
@Table(name = "tb_stock_investor_chart", indexes = {
        @Index(name = "idx_chart_stk_dt", columnList = "stk_cd, dt"),
        @Index(name = "idx_chart_dt", columnList = "dt"),
        @Index(name = "idx_chart_frgnr_orgn", columnList = "dt, frgnr_invsr, orgn")
})
@IdClass(StockInvestorChartId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorChart {

    // ============================================
    // 1. Primary Keys
    // ============================================

    @Id
    @Column(name = "stk_cd", length = 20, nullable = false)
    private String stkCd; // 종목코드

    @Id
    @Column(name = "dt", nullable = false)
    private LocalDate dt; // 일자

    // ============================================
    // 2. Request Metadata
    // ============================================

    @Column(name = "unit_tp", length = 4, nullable = false)
    private String unitTp; // 단위구분 (1:단주, 1000:천주)

    // ============================================
    // 3. Market Data (시세 정보)
    // ============================================

    @Column(name = "cur_prc")
    private Long curPrc; // 현재가

    @Column(name = "pred_pre")
    private Long predPre; // 전일대비

    @Column(name = "acc_trde_prica")
    private Long accTrdePrica; // 누적거래대금

    // ============================================
    // 4. 주요 투자자별 데이터
    // ============================================

    @Column(name = "ind_invsr")
    @Builder.Default
    private Long indInvsr = 0L; // 개인투자자

    @Column(name = "frgnr_invsr")
    @Builder.Default
    private Long frgnrInvsr = 0L; // 외국인투자자

    @Column(name = "orgn")
    @Builder.Default
    private Long orgn = 0L; // 기관계

    @Column(name = "natfor")
    @Builder.Default
    private Long natfor = 0L; // 내외국인

    // ============================================
    // 5. 기관 세부 내역
    // ============================================

    @Column(name = "fnnc_invt")
    @Builder.Default
    private Long fnncInvt = 0L; // 금융투자

    @Column(name = "insrnc")
    @Builder.Default
    private Long insrnc = 0L; // 보험

    @Column(name = "invtrt")
    @Builder.Default
    private Long invtrt = 0L; // 투신

    @Column(name = "etc_fnnc")
    @Builder.Default
    private Long etcFnnc = 0L; // 기타금융

    @Column(name = "bank")
    @Builder.Default
    private Long bank = 0L; // 은행

    @Column(name = "penfnd_etc")
    @Builder.Default
    private Long penfndEtc = 0L; // 연기금등

    @Column(name = "samo_fund")
    @Builder.Default
    private Long samoFund = 0L; // 사모펀드

    @Column(name = "natn")
    @Builder.Default
    private Long natn = 0L; // 국가

    @Column(name = "etc_corp")
    @Builder.Default
    private Long etcCorp = 0L; // 기타법인

    // ============================================
    // 6. System Audit
    // ============================================

    @Column(name = "reg_dt", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime regDt = LocalDateTime.now(); // 등록일시

    @Column(name = "upd_dt")
    private LocalDateTime updDt; // 수정일시

    @PreUpdate
    protected void onUpdate() {
        this.updDt = LocalDateTime.now();
    }
}
