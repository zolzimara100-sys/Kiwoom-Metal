package com.stocktrading.kiwoom.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * OAuth 토큰 도메인 모델
 */
@Value
@Builder
@JsonDeserialize(builder = Token.TokenBuilder.class)
public class Token {

    String accessToken;
    String tokenType;
    LocalDateTime expiresAt;
    String scope;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TokenBuilder {
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (버퍼 시간 포함)
     * @param bufferMinutes 버퍼 시간 (분)
     */
    @JsonIgnore
    public boolean isExpiringSoon(long bufferMinutes) {
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(bufferMinutes);
        return threshold.isAfter(expiresAt);
    }

    /**
     * 유효한 토큰인지 확인
     */
    @JsonIgnore
    public boolean isValid() {
        return !isExpired() && accessToken != null && !accessToken.isEmpty();
    }
}
