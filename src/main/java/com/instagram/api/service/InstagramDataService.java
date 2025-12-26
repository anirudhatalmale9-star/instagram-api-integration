package com.instagram.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagram.api.config.InstagramConfig;
import com.instagram.api.dto.InstagramDataResponse;
import com.instagram.api.dto.InstagramMediaDTO;
import com.instagram.api.dto.InstagramProfileDTO;
import com.instagram.api.entity.InstagramAccount;
import com.instagram.api.entity.InstagramMedia;
import com.instagram.api.exception.InstagramApiException;
import com.instagram.api.repository.InstagramMediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstagramDataService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramDataService.class);

    private final WebClient webClient;
    private final InstagramConfig instagramConfig;
    private final InstagramMediaRepository mediaRepository;

    private static final String PROFILE_FIELDS = "id,username,name,profile_picture_url,biography,website,followers_count,follows_count,media_count,account_type";
    private static final String MEDIA_FIELDS = "id,media_type,media_url,thumbnail_url,permalink,caption,timestamp,like_count,comments_count";

    public InstagramDataService(WebClient webClient, InstagramConfig instagramConfig,
                                 InstagramMediaRepository mediaRepository) {
        this.webClient = webClient;
        this.instagramConfig = instagramConfig;
        this.mediaRepository = mediaRepository;
    }

    public InstagramProfileDTO fetchProfile(String accessToken, String instagramBusinessAccountId) {
        logger.info("Fetching Instagram profile for account: {}", instagramBusinessAccountId);

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v18.0/" + instagramBusinessAccountId)
                            .queryParam("fields", PROFILE_FIELDS)
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(InstagramProfileDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Failed to fetch profile: {}", e.getMessage());
            throw new InstagramApiException("Failed to fetch Instagram profile", e);
        }
    }

    public String getInstagramBusinessAccountId(String accessToken) {
        logger.info("Fetching Instagram Business Account ID");

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v18.0/me/accounts")
                            .queryParam("fields", "instagram_business_account")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("data") && response.get("data").isArray()) {
                for (JsonNode page : response.get("data")) {
                    if (page.has("instagram_business_account")) {
                        return page.get("instagram_business_account").get("id").asText();
                    }
                }
            }

            throw new InstagramApiException("No Instagram Business Account found. Make sure your Instagram account is connected to a Facebook Page.", "NO_BUSINESS_ACCOUNT", 400);
        } catch (InstagramApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get Instagram Business Account ID: {}", e.getMessage());
            throw new InstagramApiException("Failed to retrieve Instagram Business Account", e);
        }
    }

    public List<InstagramMediaDTO> fetchMedia(String accessToken, String instagramBusinessAccountId, Integer limit) {
        logger.info("Fetching media for account: {}", instagramBusinessAccountId);

        int fetchLimit = limit != null ? limit : 25;

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/v18.0/" + instagramBusinessAccountId + "/media")
                            .queryParam("fields", MEDIA_FIELDS)
                            .queryParam("limit", fetchLimit)
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<InstagramMediaDTO> mediaList = new ArrayList<>();

            if (response != null && response.has("data")) {
                for (JsonNode mediaNode : response.get("data")) {
                    InstagramMediaDTO media = InstagramMediaDTO.builder()
                            .id(getTextValue(mediaNode, "id"))
                            .mediaType(getTextValue(mediaNode, "media_type"))
                            .mediaUrl(getTextValue(mediaNode, "media_url"))
                            .thumbnailUrl(getTextValue(mediaNode, "thumbnail_url"))
                            .permalink(getTextValue(mediaNode, "permalink"))
                            .caption(getTextValue(mediaNode, "caption"))
                            .timestamp(getTextValue(mediaNode, "timestamp"))
                            .likeCount(getIntValue(mediaNode, "like_count"))
                            .commentsCount(getIntValue(mediaNode, "comments_count"))
                            .build();
                    mediaList.add(media);
                }
            }

            return mediaList;
        } catch (Exception e) {
            logger.error("Failed to fetch media: {}", e.getMessage());
            throw new InstagramApiException("Failed to fetch Instagram media", e);
        }
    }

    public InstagramDataResponse fetchAllData(String accessToken, String instagramBusinessAccountId, Integer mediaLimit) {
        InstagramProfileDTO profile = fetchProfile(accessToken, instagramBusinessAccountId);
        List<InstagramMediaDTO> media = fetchMedia(accessToken, instagramBusinessAccountId, mediaLimit);

        return InstagramDataResponse.builder()
                .profile(profile)
                .media(media)
                .paging(InstagramDataResponse.PagingInfo.builder()
                        .hasMore(media.size() == (mediaLimit != null ? mediaLimit : 25))
                        .build())
                .build();
    }

    @Transactional
    public void persistMedia(InstagramAccount account, List<InstagramMediaDTO> mediaList) {
        logger.info("Persisting {} media items for account: {}", mediaList.size(), account.getUsername());

        for (InstagramMediaDTO dto : mediaList) {
            InstagramMedia media = mediaRepository.findByMediaId(dto.getId())
                    .orElse(new InstagramMedia());

            media.setMediaId(dto.getId());
            media.setAccount(account);
            media.setMediaType(dto.getMediaType());
            media.setMediaUrl(dto.getMediaUrl());
            media.setThumbnailUrl(dto.getThumbnailUrl());
            media.setPermalink(dto.getPermalink());
            media.setCaption(dto.getCaption());
            media.setLikeCount(dto.getLikeCount());
            media.setCommentsCount(dto.getCommentsCount());

            if (dto.getTimestamp() != null) {
                try {
                    media.setTimestamp(LocalDateTime.parse(dto.getTimestamp(),
                            DateTimeFormatter.ISO_DATE_TIME));
                } catch (Exception e) {
                    logger.warn("Failed to parse timestamp: {}", dto.getTimestamp());
                }
            }

            mediaRepository.save(media);
        }
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }
}
