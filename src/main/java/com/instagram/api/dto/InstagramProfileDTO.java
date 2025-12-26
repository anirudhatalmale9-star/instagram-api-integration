package com.instagram.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramProfileDTO {

    private String id;
    private String username;
    private String name;

    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;

    private String biography;
    private String website;

    @JsonProperty("followers_count")
    private Integer followersCount;

    @JsonProperty("follows_count")
    private Integer followingCount;

    @JsonProperty("media_count")
    private Integer mediaCount;

    @JsonProperty("account_type")
    private String accountType;
}
