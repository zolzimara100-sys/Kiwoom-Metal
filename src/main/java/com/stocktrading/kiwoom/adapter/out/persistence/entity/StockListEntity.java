package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 종목 정보 Entity
 * Table: tb_stock_list
 */
@Entity
@Table(name = "tb_stock_list", indexes = {
        @Index(name = "idx_market_code", columnList = "market_code"),
        @Index(name = "idx_market_name", columnList = "market_name"),
        @Index(name = "idx_state", columnList = "state")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StockListEntity {

    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 40)
    private String name;

    @Column(name = "list_count")
    private Long listCount;

    @Column(name = "audit_info", length = 20)
    private String auditInfo;

    @Column(name = "reg_day", length = 20)
    private String regDay;

    @Column(name = "last_price")
    private Long lastPrice;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "market_code", length = 20)
    private String marketCode;

    @Column(name = "market_name", length = 20)
    private String marketName;

    @Column(name = "up_name", length = 20)
    private String upName;

    @Column(name = "up_size_name", length = 20)
    private String upSizeName;

    @Column(name = "company_class_name", length = 20)
    private String companyClassName;

    @Column(name = "order_warning", length = 20)
    private String orderWarning;

    @Column(name = "nxt_enable", length = 20)
    private String nxtEnable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
