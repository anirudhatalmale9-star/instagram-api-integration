package com.instagram.api.repository;

import com.instagram.api.entity.InstagramAccount;
import com.instagram.api.entity.InstagramMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstagramMediaRepository extends JpaRepository<InstagramMedia, Long> {

    List<InstagramMedia> findByAccount(InstagramAccount account);

    Page<InstagramMedia> findByAccount(InstagramAccount account, Pageable pageable);

    Optional<InstagramMedia> findByMediaId(String mediaId);

    void deleteByAccount(InstagramAccount account);

    boolean existsByMediaId(String mediaId);
}
