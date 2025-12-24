package com.stocktrading.kiwoom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "kiwoom.api")
@Getter
@Setter
public class KiwoomApiConfig {
    
    private String baseUrl;
    private String authUrl;
    private String appKey;
    private String appSecret;
    private String accountNumber;
    
}
