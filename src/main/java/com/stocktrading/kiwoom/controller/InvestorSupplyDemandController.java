package com.stocktrading.kiwoom.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorInvestAccumulationEntity;
import com.stocktrading.kiwoom.service.InvestorSupplyDemandService;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class InvestorSupplyDemandController {

    private final InvestorSupplyDemandService service;

    /**
     * 투자자별 수급분석 조회 (누적 수량/금액)
     */
    @GetMapping("/supply-demand")
    public List<StockInvestorInvestAccumulationEntity> getSupplyDemandAnalysis(
            @RequestParam("stkCd") String stkCd,
            @RequestParam(value = "startDt", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDt,
            @RequestParam(value = "endDt", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDt) {

        return service.getAnalysisData(stkCd, startDt, endDt);
    }
}
