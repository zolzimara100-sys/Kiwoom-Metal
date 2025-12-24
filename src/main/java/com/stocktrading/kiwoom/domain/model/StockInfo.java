package com.stocktrading.kiwoom.domain.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * 종목 정보 도메인 모델
 * TR: KA10099 - 종목정보리스트
 */
@Getter
@Builder
public class StockInfo {

    /**
     * 종목코드 (단축코드)
     */
    private final String code;

    /**
     * 종목명
     */
    private final String name;

    /**
     * 상장주식수
     */
    private final Long listCount;

    /**
     * 감리구분
     */
    private final String auditInfo;

    /**
     * 상장일
     */
    private final String regDay;

    /**
     * 전일종가
     */
    private final Long lastPrice;

    /**
     * 종목상태
     */
    private final String state;

    /**
     * 시장구분코드
     */
    private final String marketCode;

    /**
     * 시장명
     */
    private final String marketName;

    /**
     * 업종명
     */
    private final String upName;

    /**
     * 섹터 분류 (KOSPI200 등 별도 메타정보)
     */
    private final String sector;

    /**
     * 회사크기분류
     */
    private final String upSizeName;

    /**
     * 회사분류 (코스닥만 존재)
     */
    private final String companyClassName;

    /**
     * 투자유의종목여부
     * 0: 해당없음, 2: 정리매매, 3: 단기과열, 4: 투자위험, 5: 투자경과
     * 1: ETF투자주의요망(ETF인 경우만 전달)
     */
    private final String orderWarning;

    /**
     * NXT가능여부 (Y: 가능)
     */
    private final String nxtEnable;

    /**
     * 생성일시
     */
    private final LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private final LocalDateTime updatedAt;

    /**
     * 정상 종목 여부 확인
     */
    public boolean isNormalState() {
        return "정상".equals(state);
    }

    /**
     * NXT 가능 여부 확인
     */
    public boolean isNxtAvailable() {
        return "Y".equals(nxtEnable);
    }

    /**
     * 투자 유의 종목 여부 확인
     */
    public boolean isWarningStock() {
        return orderWarning != null && !"0".equals(orderWarning);
    }
}
