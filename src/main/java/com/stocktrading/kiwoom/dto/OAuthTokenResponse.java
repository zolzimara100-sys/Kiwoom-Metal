package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenResponse {
    @JsonProperty("expires_dt")
    private String expiresDt;

    @JsonProperty("token_type")
    private String tokenType;

    private String token;

    @JsonProperty("return_code")
    private Integer returnCode;

    @JsonProperty("return_msg")
    private String returnMsg;

    private boolean success;
    private String message;

    /**
     * 만료 시간을 LocalDateTime으로 파싱
     * 키움 API 형식: "yyyyMMddHHmmss" (예: 20251203035221)
     */
    public LocalDateTime getExpiresDateTime() {
        if (expiresDt == null || expiresDt.isEmpty()) {
            return null;
        }
        try {
            // 키움 API 실제 형식: yyyyMMddHHmmss
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(expiresDt, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        LocalDateTime expiresDateTime = getExpiresDateTime();
        if (expiresDateTime == null) {
            return true;  // 만료 시간을 알 수 없으면 만료된 것으로 간주
        }
        return LocalDateTime.now().isAfter(expiresDateTime);
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (기본: 5분 전)
     */
    public boolean isExpiringSoon() {
        return isExpiringSoon(5);
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인
     * @param minutesBefore 만료 몇 분 전부터 갱신할지
     */
    public boolean isExpiringSoon(int minutesBefore) {
        LocalDateTime expiresDateTime = getExpiresDateTime();
        if (expiresDateTime == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(minutesBefore).isAfter(expiresDateTime);
    }
}
