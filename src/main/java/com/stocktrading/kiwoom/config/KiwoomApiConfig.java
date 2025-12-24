package com.stocktrading.kiwoom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "kiwoom")
@Getter
@Setter
public class KiwoomApiConfig {

    private String baseUrl = "https://api.kiwoom.com";
    private String authUrl = "https://api.kiwoom.com";
    private Api api = new Api();

    @Getter
    @Setter
    public static class Api {
        private String appKey;
        private String appSecret;
        private String accountNumber;
    }

    // Convenience methods for backward compatibility
    public String getAppKey() {
        return api.getAppKey();
    }

    public String getAppSecret() {
        return api.getAppSecret();
    }

    public String getAccountNumber() {
        return api.getAccountNumber();
    }

}
