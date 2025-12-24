package com.stocktrading.kiwoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTokenRequest {
    private String grant_type;
    private String appkey;
    private String secretkey;
}
