package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_dt")
    private String expiresDt;
    
    @JsonProperty("return_code")
    private int returnCode;
    
    @JsonProperty("return_msg")
    private String returnMsg;
    
    // 기존 코드와의 호환성을 위한 메서드
    public String getAccessToken() {
        return token;
    }
    
    public long getExpiresIn() {
        // expires_dt를 파싱하여 초 단위로 반환 (간단히 24시간으로 설정)
        return 86400;
    }
}
