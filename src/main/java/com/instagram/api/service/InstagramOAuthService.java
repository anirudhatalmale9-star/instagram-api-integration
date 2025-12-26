package com.instagram.api.service;

import com.instagram.api.config.InstagramConfig;
import com.instagram.api.dto.InstagramTokenResponse;
import com.instagram.api.dto.OAuthUrlResponse;
import com.instagram.api.exception.InstagramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InstagramOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramOAuthService.class);

    private final WebClient webClient;
    private final InstagramConfig instagramConfig;
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();

    @Value("${spring.security.oauth2.client.registration.instagram.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.instagram.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.instagram.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.instagram.scope}")
    private String scope;

    public InstagramOAuthService(WebClient webClient, InstagramConfig instagramConfig) {
        this.webClient = webClient;
        this.instagramConfig = instagramConfig;
    }

    public OAuthUrlResponse generateAuthorizationUrl(String userId) {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, userId);

        String authUrl = UriComponentsBuilder
                .fromUriString("https://www.facebook.com/v18.0/dialog/oauth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();

        logger.info("Generated authorization URL for user: {}", userId);

        return OAuthUrlResponse.builder()
                .authorizationUrl(authUrl)
                .state(state)
                .build();
    }

    public String validateStateAndGetUserId(String state) {
        String userId = stateStore.remove(state);
        if (userId == null) {
            throw new InstagramApiException("Invalid or expired state parameter", "INVALID_STATE", 400);
        }
        return userId;
    }

    public InstagramTokenResponse exchangeCodeForToken(String code) {
        logger.info("Exchanging authorization code for access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "authorization_code");
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);

        try {
            return webClient.post()
                    .uri(instagramConfig.getFacebookGraphUrl() + "/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(InstagramTokenResponse.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to exchange code for token: {}", e.getMessage());
            throw new InstagramApiException("Failed to exchange authorization code for token", e);
        }
    }

    public InstagramTokenResponse exchangeLongLivedToken(String shortLivedToken) {
        logger.info("Exchanging for long-lived token");

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v18.0/oauth/access_token")
                            .queryParam("grant_type", "fb_exchange_token")
                            .queryParam("client_id", clientId)
                            .queryParam("client_secret", clientSecret)
                            .queryParam("fb_exchange_token", shortLivedToken)
                            .build())
                    .retrieve()
                    .bodyToMono(InstagramTokenResponse.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to exchange for long-lived token: {}", e.getMessage());
            throw new InstagramApiException("Failed to exchange for long-lived token", e);
        }
    }

    public InstagramTokenResponse refreshToken(String accessToken) {
        logger.info("Refreshing access token");

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.instagram.com")
                            .path("/refresh_access_token")
                            .queryParam("grant_type", "ig_refresh_token")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(InstagramTokenResponse.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to refresh token: {}", e.getMessage());
            throw new InstagramApiException("Failed to refresh access token", e);
        }
    }
}
