package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 섹터별 투자자 이동평균 복합키
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInvestorSectorMaId implements Serializable {

    private String sectorCd;
    private String dt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockInvestorSectorMaId)) return false;
        StockInvestorSectorMaId that = (StockInvestorSectorMaId) o;
        return Objects.equals(sectorCd, that.sectorCd) &&
               Objects.equals(dt, that.dt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectorCd, dt);
    }
}
