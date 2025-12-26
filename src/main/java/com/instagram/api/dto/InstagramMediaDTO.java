package com.instagram.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramMediaDTO {

    private String id;

    @JsonProperty("media_type")
    private String mediaType;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    private String permalink;
    private String caption;
    private String timestamp;

    @JsonProperty("like_count")
    private Integer likeCount;

    @JsonProperty("comments_count")
    private Integer commentsCount;
}
