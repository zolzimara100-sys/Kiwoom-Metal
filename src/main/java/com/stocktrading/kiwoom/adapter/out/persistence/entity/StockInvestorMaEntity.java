package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

/**
 * 투자자 이동평균 엔티티
 * tb_stock_investor_ma 테이블 매핑
 */
@Entity
@Table(name = "tb_stock_investor_ma")
@IdClass(StockInvestorMaId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvestorMaEntity {

    @Id
    @Column(name = "stk_cd", length = 20)
    private String stkCd;

    @Id
    @Column(name = "dt", length = 8)
    private String dt;

    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "category1", length = 50)
    private String category1;

    @Column(name = "category2", length = 50)
    private String category2;

    @Column(name = "category3", length = 50)
    private String category3;

    @Column(name = "cur_prc")
    private Long curPrc;

    // 외국인 (Foreigner)
    @Column(name = "frgnr_invsr_ma5", precision = 15, scale = 2)
    private BigDecimal frgnrInvsrMa5;

    @Column(name = "frgnr_invsr_ma10", precision = 15, scale = 2)
    private BigDecimal frgnrInvsrMa10;

    @Column(name = "frgnr_invsr_ma20", precision = 15, scale = 2)
    private BigDecimal frgnrInvsrMa20;

    @Column(name = "frgnr_invsr_ma30", precision = 15)
    private BigDecimal frgnrInvsrMa30;

    @Column(name = "frgnr_invsr_ma40", precision = 15)
    private BigDecimal frgnrInvsrMa40;

    @Column(name = "frgnr_invsr_ma50", precision = 15)
    private BigDecimal frgnrInvsrMa50;

    @Column(name = "frgnr_invsr_ma60", precision = 15, scale = 2)
    private BigDecimal frgnrInvsrMa60;

    @Column(name = "frgnr_invsr_ma90", precision = 15)
    private BigDecimal frgnrInvsrMa90;
    @Column(name = "frgnr_invsr_ma120", precision = 15)
    private BigDecimal frgnrInvsrMa120;

    @Column(name = "frgnr_invsr_ma140", precision = 15)
    private BigDecimal frgnrInvsrMa140;

    // 기관계 (Organ)
    @Column(name = "orgn_ma5", precision = 15, scale = 2)
    private BigDecimal orgnMa5;

    @Column(name = "orgn_ma10", precision = 15, scale = 2)
    private BigDecimal orgnMa10;

    @Column(name = "orgn_ma20", precision = 15, scale = 2)
    private BigDecimal orgnMa20;

    @Column(name = "orgn_ma30", precision = 15)
    private BigDecimal orgnMa30;

    @Column(name = "orgn_ma40", precision = 15)
    private BigDecimal orgnMa40;

    @Column(name = "orgn_ma50", precision = 15)
    private BigDecimal orgnMa50;

    @Column(name = "orgn_ma60", precision = 15, scale = 2)
    private BigDecimal orgnMa60;

    @Column(name = "orgn_ma90", precision = 15)
    private BigDecimal orgnMa90;
    @Column(name = "orgn_ma120", precision = 15)
    private BigDecimal orgnMa120;

    @Column(name = "orgn_ma140", precision = 15)
    private BigDecimal orgnMa140;

    // 금융투자 (Financial Investment)
    @Column(name = "fnnc_invt_ma5", precision = 15, scale = 2)
    private BigDecimal fnncInvtMa5;

    @Column(name = "fnnc_invt_ma10", precision = 15, scale = 2)
    private BigDecimal fnncInvtMa10;

    @Column(name = "fnnc_invt_ma20", precision = 15, scale = 2)
    private BigDecimal fnncInvtMa20;

    @Column(name = "fnnc_invt_ma30", precision = 15)
    private BigDecimal fnncInvtMa30;

    @Column(name = "fnnc_invt_ma40", precision = 15)
    private BigDecimal fnncInvtMa40;

    @Column(name = "fnnc_invt_ma50", precision = 15)
    private BigDecimal fnncInvtMa50;

    @Column(name = "fnnc_invt_ma60", precision = 15, scale = 2)
    private BigDecimal fnncInvtMa60;

    @Column(name = "fnnc_invt_ma90", precision = 15)
    private BigDecimal fnncInvtMa90;
    @Column(name = "fnnc_invt_ma120", precision = 15)
    private BigDecimal fnncInvtMa120;

    @Column(name = "fnnc_invt_ma140", precision = 15)
    private BigDecimal fnncInvtMa140;

    // 보험 (Insurance)
    @Column(name = "insrnc_ma5", precision = 15, scale = 2)
    private BigDecimal insrncMa5;

    @Column(name = "insrnc_ma10", precision = 15, scale = 2)
    private BigDecimal insrncMa10;

    @Column(name = "insrnc_ma20", precision = 15, scale = 2)
    private BigDecimal insrncMa20;

    @Column(name = "insrnc_ma30", precision = 15)
    private BigDecimal insrncMa30;

    @Column(name = "insrnc_ma40", precision = 15)
    private BigDecimal insrncMa40;

    @Column(name = "insrnc_ma50", precision = 15)
    private BigDecimal insrncMa50;

    @Column(name = "insrnc_ma60", precision = 15, scale = 2)
    private BigDecimal insrncMa60;

    @Column(name = "insrnc_ma90", precision = 15)
    private BigDecimal insrncMa90;
    @Column(name = "insrnc_ma120", precision = 15)
    private BigDecimal insrncMa120;

    @Column(name = "insrnc_ma140", precision = 15)
    private BigDecimal insrncMa140;

    // 투신 (Investment Trust)
    @Column(name = "invtrt_ma5", precision = 15, scale = 2)
    private BigDecimal invtrtMa5;

    @Column(name = "invtrt_ma10", precision = 15, scale = 2)
    private BigDecimal invtrtMa10;

    @Column(name = "invtrt_ma20", precision = 15, scale = 2)
    private BigDecimal invtrtMa20;

    @Column(name = "invtrt_ma30", precision = 15)
    private BigDecimal invtrtMa30;

    @Column(name = "invtrt_ma40", precision = 15)
    private BigDecimal invtrtMa40;

    @Column(name = "invtrt_ma50", precision = 15)
    private BigDecimal invtrtMa50;

    @Column(name = "invtrt_ma60", precision = 15, scale = 2)
    private BigDecimal invtrtMa60;

    @Column(name = "invtrt_ma90", precision = 15)
    private BigDecimal invtrtMa90;
    @Column(name = "invtrt_ma120", precision = 15)
    private BigDecimal invtrtMa120;

    @Column(name = "invtrt_ma140", precision = 15)
    private BigDecimal invtrtMa140;

    // 기타금융 (Etc Finance)
    @Column(name = "etc_fnnc_ma5", precision = 15, scale = 2)
    private BigDecimal etcFnncMa5;

    @Column(name = "etc_fnnc_ma10", precision = 15, scale = 2)
    private BigDecimal etcFnncMa10;

    @Column(name = "etc_fnnc_ma20", precision = 15, scale = 2)
    private BigDecimal etcFnncMa20;

    @Column(name = "etc_fnnc_ma30", precision = 15)
    private BigDecimal etcFnncMa30;

    @Column(name = "etc_fnnc_ma40", precision = 15)
    private BigDecimal etcFnncMa40;

    @Column(name = "etc_fnnc_ma50", precision = 15)
    private BigDecimal etcFnncMa50;

    @Column(name = "etc_fnnc_ma60", precision = 15, scale = 2)
    private BigDecimal etcFnncMa60;

    @Column(name = "etc_fnnc_ma90", precision = 15)
    private BigDecimal etcFnncMa90;
    @Column(name = "etc_fnnc_ma120", precision = 15)
    private BigDecimal etcFnncMa120;

    @Column(name = "etc_fnnc_ma140", precision = 15)
    private BigDecimal etcFnncMa140;

    // 은행 (Bank)
    @Column(name = "bank_ma5", precision = 15, scale = 2)
    private BigDecimal bankMa5;

    @Column(name = "bank_ma10", precision = 15, scale = 2)
    private BigDecimal bankMa10;

    @Column(name = "bank_ma20", precision = 15, scale = 2)
    private BigDecimal bankMa20;

    @Column(name = "bank_ma30", precision = 15)
    private BigDecimal bankMa30;

    @Column(name = "bank_ma40", precision = 15)
    private BigDecimal bankMa40;

    @Column(name = "bank_ma50", precision = 15)
    private BigDecimal bankMa50;

    @Column(name = "bank_ma60", precision = 15, scale = 2)
    private BigDecimal bankMa60;

    @Column(name = "bank_ma90", precision = 15)
    private BigDecimal bankMa90;
    @Column(name = "bank_ma120", precision = 15)
    private BigDecimal bankMa120;

    @Column(name = "bank_ma140", precision = 15)
    private BigDecimal bankMa140;

    // 연기금등 (Pension Fund)
    @Column(name = "penfnd_etc_ma5", precision = 15, scale = 2)
    private BigDecimal penfndEtcMa5;

    @Column(name = "penfnd_etc_ma10", precision = 15, scale = 2)
    private BigDecimal penfndEtcMa10;

    @Column(name = "penfnd_etc_ma20", precision = 15, scale = 2)
    private BigDecimal penfndEtcMa20;

    @Column(name = "penfnd_etc_ma30", precision = 15)
    private BigDecimal penfndEtcMa30;

    @Column(name = "penfnd_etc_ma40", precision = 15)
    private BigDecimal penfndEtcMa40;

    @Column(name = "penfnd_etc_ma50", precision = 15)
    private BigDecimal penfndEtcMa50;

    @Column(name = "penfnd_etc_ma60", precision = 15, scale = 2)
    private BigDecimal penfndEtcMa60;

    @Column(name = "penfnd_etc_ma90", precision = 15)
    private BigDecimal penfndEtcMa90;
    @Column(name = "penfnd_etc_ma120", precision = 15)
    private BigDecimal penfndEtcMa120;

    @Column(name = "penfnd_etc_ma140", precision = 15)
    private BigDecimal penfndEtcMa140;

    // 사모펀드 (Private Equity)
    @Column(name = "samo_fund_ma5", precision = 15, scale = 2)
    private BigDecimal samoFundMa5;

    @Column(name = "samo_fund_ma10", precision = 15, scale = 2)
    private BigDecimal samoFundMa10;

    @Column(name = "samo_fund_ma20", precision = 15, scale = 2)
    private BigDecimal samoFundMa20;

    @Column(name = "samo_fund_ma30", precision = 15)
    private BigDecimal samoFundMa30;

    @Column(name = "samo_fund_ma40", precision = 15)
    private BigDecimal samoFundMa40;

    @Column(name = "samo_fund_ma50", precision = 15)
    private BigDecimal samoFundMa50;

    @Column(name = "samo_fund_ma60", precision = 15, scale = 2)
    private BigDecimal samoFundMa60;

    @Column(name = "samo_fund_ma90", precision = 15)
    private BigDecimal samoFundMa90;
    @Column(name = "samo_fund_ma120", precision = 15)
    private BigDecimal samoFundMa120;

    @Column(name = "samo_fund_ma140", precision = 15)
    private BigDecimal samoFundMa140;

    // 국가 (National)
    @Column(name = "natn_ma5", precision = 15, scale = 2)
    private BigDecimal natnMa5;

    @Column(name = "natn_ma10", precision = 15, scale = 2)
    private BigDecimal natnMa10;

    @Column(name = "natn_ma20", precision = 15, scale = 2)
    private BigDecimal natnMa20;

    @Column(name = "natn_ma30", precision = 15)
    private BigDecimal natnMa30;

    @Column(name = "natn_ma40", precision = 15)
    private BigDecimal natnMa40;

    @Column(name = "natn_ma50", precision = 15)
    private BigDecimal natnMa50;

    @Column(name = "natn_ma60", precision = 15, scale = 2)
    private BigDecimal natnMa60;

    @Column(name = "natn_ma90", precision = 15)
    private BigDecimal natnMa90;
    @Column(name = "natn_ma120", precision = 15)
    private BigDecimal natnMa120;

    @Column(name = "natn_ma140", precision = 15)
    private BigDecimal natnMa140;

    // 기타법인 (Etc Corp)
    @Column(name = "etc_corp_ma5", precision = 15, scale = 2)
    private BigDecimal etcCorpMa5;

    @Column(name = "etc_corp_ma10", precision = 15, scale = 2)
    private BigDecimal etcCorpMa10;

    @Column(name = "etc_corp_ma20", precision = 15, scale = 2)
    private BigDecimal etcCorpMa20;

    @Column(name = "etc_corp_ma30", precision = 15)
    private BigDecimal etcCorpMa30;

    @Column(name = "etc_corp_ma40", precision = 15)
    private BigDecimal etcCorpMa40;

    @Column(name = "etc_corp_ma50", precision = 15)
    private BigDecimal etcCorpMa50;

    @Column(name = "etc_corp_ma60", precision = 15, scale = 2)
    private BigDecimal etcCorpMa60;

    @Column(name = "etc_corp_ma90", precision = 15)
    private BigDecimal etcCorpMa90;
    @Column(name = "etc_corp_ma120", precision = 15)
    private BigDecimal etcCorpMa120;

    @Column(name = "etc_corp_ma140", precision = 15)
    private BigDecimal etcCorpMa140;

    // 내국인 (National Foreigner)
    @Column(name = "natfor_ma5", precision = 15, scale = 2)
    private BigDecimal natforMa5;

    @Column(name = "natfor_ma10", precision = 15, scale = 2)
    private BigDecimal natforMa10;

    @Column(name = "natfor_ma20", precision = 15, scale = 2)
    private BigDecimal natforMa20;

    @Column(name = "natfor_ma30", precision = 15)
    private BigDecimal natforMa30;

    @Column(name = "natfor_ma40", precision = 15)
    private BigDecimal natforMa40;

    @Column(name = "natfor_ma50", precision = 15)
    private BigDecimal natforMa50;

    @Column(name = "natfor_ma60", precision = 15, scale = 2)
    private BigDecimal natforMa60;

    @Column(name = "natfor_ma90", precision = 15)
    private BigDecimal natforMa90;
    @Column(name = "natfor_ma120", precision = 15)
    private BigDecimal natforMa120;

    @Column(name = "natfor_ma140", precision = 15)
    private BigDecimal natforMa140;

    // 개인 (Individual)
    @Column(name = "ind_invsr_ma5", precision = 15, scale = 2)
    private BigDecimal indInvsrMa5;

    @Column(name = "ind_invsr_ma10", precision = 15, scale = 2)
    private BigDecimal indInvsrMa10;

    @Column(name = "ind_invsr_ma20", precision = 15, scale = 2)
    private BigDecimal indInvsrMa20;

    @Column(name = "ind_invsr_ma30", precision = 15)
    private BigDecimal indInvsrMa30;

    @Column(name = "ind_invsr_ma40", precision = 15)
    private BigDecimal indInvsrMa40;

    @Column(name = "ind_invsr_ma50", precision = 15)
    private BigDecimal indInvsrMa50;

    @Column(name = "ind_invsr_ma60", precision = 15, scale = 2)
    private BigDecimal indInvsrMa60;

    @Column(name = "ind_invsr_ma90", precision = 15)
    private BigDecimal indInvsrMa90;

    @Column(name = "ind_invsr_ma120", precision = 15)
    private BigDecimal indInvsrMa120;

    @Column(name = "ind_invsr_ma140", precision = 15)
    private BigDecimal indInvsrMa140;
}
