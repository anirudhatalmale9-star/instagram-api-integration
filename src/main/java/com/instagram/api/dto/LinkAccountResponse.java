package com.instagram.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccountResponse {

    private String userId;
    private String instagramUserId;
    private String username;
    private String name;
    private String profilePictureUrl;
    private LocalDateTime tokenExpiresAt;
    private String message;
    private boolean success;
}
