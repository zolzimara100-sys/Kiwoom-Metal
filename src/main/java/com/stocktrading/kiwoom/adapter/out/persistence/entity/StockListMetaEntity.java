package com.stocktrading.kiwoom.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * KOSPI200 종목 메타 정보 Entity
 * Table: tb_stock_list_meta
 */
@Entity
@Table(name = "tb_stock_list_meta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StockListMetaEntity {

    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "main", length = 20)
    private String main;

    @Column(name = "sub", length = 20)
    private String sub;

    @Column(name = "detail", length = 20)
    private String detail;

    @Column(name = "sector", length = 255)
    private String sector;
}
