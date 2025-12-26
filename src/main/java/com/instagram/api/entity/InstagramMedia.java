package com.instagram.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "instagram_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "media_id", unique = true, nullable = false)
    private String mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private InstagramAccount account;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "media_url", length = 2000)
    private String mediaUrl;

    @Column(name = "thumbnail_url", length = 2000)
    private String thumbnailUrl;

    @Column(name = "permalink", length = 1000)
    private String permalink;

    @Column(name = "caption", length = 5000)
    private String caption;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "comments_count")
    private Integer commentsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
