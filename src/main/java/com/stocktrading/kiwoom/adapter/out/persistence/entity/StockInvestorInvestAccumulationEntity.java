package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
 * 투자자별 수급분석 (누적수량/누적금액) Entity
 * Table: tb_stock_investor_invest_accumulation
 */
@Entity
@Table(name = "tb_stock_investor_invest_accumulation", indexes = {
        @Index(name = "idx_accumulation_stk_dt", columnList = "stk_cd, dt")
})
@IdClass(StockInvestorInvestAccumulationId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorInvestAccumulationEntity {

    // ============================================
    // PK
    // ============================================
    @Id
    @Column(name = "stk_cd", length = 20, nullable = false)
    private String stkCd; // 종목코드

    @Id
    @Column(name = "dt", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate dt; // 일자

    @Column(name = "cur_prc")
    private Long curPrc;

    // ============================================
    // Meta Info
    // ============================================
    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "category1", length = 50)
    private String category1;

    @Column(name = "category2", length = 50)
    private String category2;

    @Column(name = "category3", length = 50)
    private String category3;

    // ============================================
    // 1. Daily Net Buy Quantity (일별 순매수량)
    // ============================================
    @Column(name = "ind_invsr")
    private Long indInvsr;
    @Column(name = "frgnr_invsr")
    private Long frgnrInvsr;
    @Column(name = "orgn")
    private Long orgn;
    @Column(name = "fnnc_invt")
    private Long fnncInvt;
    @Column(name = "insrnc")
    private Long insrnc;
    @Column(name = "invtrt")
    private Long invtrt;
    @Column(name = "etc_fnnc")
    private Long etcFnnc;
    @Column(name = "bank")
    private Long bank;
    @Column(name = "penfnd_etc")
    private Long penfndEtc;
    @Column(name = "samo_fund")
    private Long samoFund;
    @Column(name = "natn")
    private Long natn;
    @Column(name = "etc_corp")
    private Long etcCorp;
    @Column(name = "natfor")
    private Long natfor;
    @Column(name = "frgnr_invsr_orgn")
    private Long frgnrInvsrOrgn;

    // ============================================
    // 2. Cumulative Net Buy Quantity (누적 순매수량)
    // ============================================
    @Column(name = "ind_invsr_net_buy_qty")
    private Long indInvsrNetBuyQty;
    @Column(name = "frgnr_invsr_net_buy_qty")
    private Long frgnrInvsrNetBuyQty;
    @Column(name = "orgn_net_buy_qty")
    private Long orgnNetBuyQty;
    @Column(name = "fnnc_invt_net_buy_qty")
    private Long fnncInvtNetBuyQty;
    @Column(name = "insrnc_net_buy_qty")
    private Long insrncNetBuyQty;
    @Column(name = "invtrt_net_buy_qty")
    private Long invtrtNetBuyQty;
    @Column(name = "etc_fnnc_net_buy_qty")
    private Long etcFnncNetBuyQty;
    @Column(name = "bank_net_buy_qty")
    private Long bankNetBuyQty;
    @Column(name = "penfnd_etc_net_buy_qty")
    private Long penfndEtcNetBuyQty;
    @Column(name = "samo_fund_net_buy_qty")
    private Long samoFundNetBuyQty;
    @Column(name = "natn_net_buy_qty")
    private Long natnNetBuyQty;
    @Column(name = "etc_corp_net_buy_qty")
    private Long etcCorpNetBuyQty;
    @Column(name = "natfor_net_buy_qty")
    private Long natforNetBuyQty;
    @Column(name = "frgnr_invsr_orgn_net_buy_qty")
    private Long frgnrInvsrOrgnNetBuyQty;

    // ============================================
    // 3. Cumulative Net Buy Amount (누적 투자(순매수)금액)
    // ============================================
    @Column(name = "ind_invsr_net_buy_amount")
    private Long indInvsrNetBuyAmount;
    @Column(name = "frgnr_invsr_net_buy_amount")
    private Long frgnrInvsrNetBuyAmount;
    @Column(name = "orgn_net_buy_amount")
    private Long orgnNetBuyAmount;
    @Column(name = "fnnc_invt_net_buy_amount")
    private Long fnncInvtNetBuyAmount;
    @Column(name = "insrnc_net_buy_amount")
    private Long insrncNetBuyAmount;
    @Column(name = "invtrt_net_buy_amount")
    private Long invtrtNetBuyAmount;
    @Column(name = "etc_fnnc_net_buy_amount")
    private Long etcFnncNetBuyAmount;
    @Column(name = "bank_net_buy_amount")
    private Long bankNetBuyAmount;
    @Column(name = "penfnd_etc_net_buy_amount")
    private Long penfndEtcNetBuyAmount;
    @Column(name = "samo_fund_net_buy_amount")
    private Long samoFundNetBuyAmount;
    @Column(name = "natn_net_buy_amount")
    private Long natnNetBuyAmount;
    @Column(name = "etc_corp_net_buy_amount")
    private Long etcCorpNetBuyAmount;
    @Column(name = "natfor_net_buy_amount")
    private Long natforNetBuyAmount;
    @Column(name = "frgnr_invsr_orgn_net_buy_amount")
    private Long frgnrInvsrOrgnNetBuyAmount;

    // ============================================
    // System Audit
    // ============================================
    @Column(name = "reg_dt", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime regDt = LocalDateTime.now();

    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    @PreUpdate
    protected void onUpdate() {
        this.updDt = LocalDateTime.now();
    }
}
