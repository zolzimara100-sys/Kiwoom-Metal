package com.stocktrading.kiwoom.adapter.out.persistence;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockListEntity;
import com.stocktrading.kiwoom.domain.model.StockInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * StockInfo <-> StockListEntity 매퍼
 */
@Component
public class StockListMapper {

    /**
     * Domain Model -> Entity
     */
    public StockListEntity toEntity(StockInfo stockInfo) {
        LocalDateTime now = LocalDateTime.now();

        return StockListEntity.builder()
                .code(stockInfo.getCode())
                .name(stockInfo.getName())
                .listCount(stockInfo.getListCount())
                .auditInfo(stockInfo.getAuditInfo())
                .regDay(stockInfo.getRegDay())
                .lastPrice(stockInfo.getLastPrice())
                .state(stockInfo.getState())
                .marketCode(stockInfo.getMarketCode())
                .marketName(stockInfo.getMarketName())
                .upName(stockInfo.getUpName())
                .upSizeName(stockInfo.getUpSizeName())
                .companyClassName(stockInfo.getCompanyClassName())
                .orderWarning(stockInfo.getOrderWarning())
                .nxtEnable(stockInfo.getNxtEnable())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Entity -> Domain Model
     */
    public StockInfo toDomain(StockListEntity entity) {
        return StockInfo.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .listCount(entity.getListCount())
                .auditInfo(entity.getAuditInfo())
                .regDay(entity.getRegDay())
                .lastPrice(entity.getLastPrice())
                .state(entity.getState())
                .marketCode(entity.getMarketCode())
                .marketName(entity.getMarketName())
                .upName(entity.getUpName())
                .upSizeName(entity.getUpSizeName())
                .companyClassName(entity.getCompanyClassName())
                .orderWarning(entity.getOrderWarning())
                .nxtEnable(entity.getNxtEnable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
