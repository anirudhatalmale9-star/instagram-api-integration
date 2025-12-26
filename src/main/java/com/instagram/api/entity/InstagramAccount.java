package com.instagram.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "instagram_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(name = "instagram_user_id", unique = true)
    private String instagramUserId;

    @Column(name = "instagram_business_account_id")
    private String instagramBusinessAccountId;

    @Column(name = "username")
    private String username;

    @Column(name = "name")
    private String name;

    @Column(name = "profile_picture_url", length = 1000)
    private String profilePictureUrl;

    @Column(name = "biography", length = 2000)
    private String biography;

    @Column(name = "website")
    private String website;

    @Column(name = "followers_count")
    private Integer followersCount;

    @Column(name = "following_count")
    private Integer followingCount;

    @Column(name = "media_count")
    private Integer mediaCount;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
