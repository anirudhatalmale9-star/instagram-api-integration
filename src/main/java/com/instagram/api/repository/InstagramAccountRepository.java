package com.instagram.api.repository;

import com.instagram.api.entity.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, Long> {

    Optional<InstagramAccount> findByUserId(String userId);

    Optional<InstagramAccount> findByInstagramUserId(String instagramUserId);

    Optional<InstagramAccount> findByUserIdAndIsActiveTrue(String userId);

    boolean existsByUserIdAndIsActiveTrue(String userId);

    void deleteByUserId(String userId);
}
