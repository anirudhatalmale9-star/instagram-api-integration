package com.instagram.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "instagram.api")
public class InstagramConfig {

    private String baseUrl = "https://graph.instagram.com";
    private String oauthUrl = "https://api.instagram.com/oauth";
    private String facebookGraphUrl = "https://graph.facebook.com/v18.0";
}
