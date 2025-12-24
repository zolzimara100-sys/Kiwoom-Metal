package com.stocktrading.kiwoom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 루트 경로 안내 컨트롤러
 * 백엔드는 REST API 전용이므로 프론트엔드 URL을 안내합니다.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Kiwoom Stock Trading Backend API Server");
        response.put("status", "running");
        response.put("frontend", "http://localhost:3000");
        response.put("api", Map.of(
            "oauth", "/api/v1/oauth/token",
            "investorChart", "/api/v1/investor-chart/{stockCode}",
            "stockList", "/api/v1/stock-list"
        ));
        response.put("note", "This is a REST API server. Please access the frontend at http://localhost:3000");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "kiwoom-backend");

        return ResponseEntity.ok(response);
    }
}
