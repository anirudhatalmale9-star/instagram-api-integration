package com.instagram.api.service;

import com.instagram.api.dto.*;
import com.instagram.api.entity.InstagramAccount;
import com.instagram.api.exception.AccountNotFoundException;
import com.instagram.api.exception.InstagramApiException;
import com.instagram.api.repository.InstagramAccountRepository;
import com.instagram.api.repository.InstagramMediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstagramAccountService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramAccountService.class);

    private final InstagramAccountRepository accountRepository;
    private final InstagramMediaRepository mediaRepository;
    private final InstagramOAuthService oAuthService;
    private final InstagramDataService dataService;

    public InstagramAccountService(InstagramAccountRepository accountRepository,
                                    InstagramMediaRepository mediaRepository,
                                    InstagramOAuthService oAuthService,
                                    InstagramDataService dataService) {
        this.accountRepository = accountRepository;
        this.mediaRepository = mediaRepository;
        this.oAuthService = oAuthService;
        this.dataService = dataService;
    }

    public OAuthUrlResponse initiateOAuth(String userId) {
        logger.info("Initiating OAuth for user: {}", userId);
        return oAuthService.generateAuthorizationUrl(userId);
    }

    @Transactional
    public LinkAccountResponse handleOAuthCallback(String code, String state) {
        logger.info("Handling OAuth callback with state: {}", state);

        String userId = oAuthService.validateStateAndGetUserId(state);

        // Exchange code for short-lived token
        InstagramTokenResponse shortLivedToken = oAuthService.exchangeCodeForToken(code);

        // Exchange for long-lived token
        InstagramTokenResponse longLivedToken = oAuthService.exchangeLongLivedToken(shortLivedToken.getAccessToken());

        // Get Instagram Business Account ID
        String instagramBusinessAccountId = dataService.getInstagramBusinessAccountId(longLivedToken.getAccessToken());

        // Fetch profile data
        InstagramProfileDTO profile = dataService.fetchProfile(longLivedToken.getAccessToken(), instagramBusinessAccountId);

        // Calculate token expiration (long-lived tokens typically expire in 60 days)
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(
                longLivedToken.getExpiresIn() != null ? longLivedToken.getExpiresIn() : 5184000L);

        // Create or update account
        InstagramAccount account = accountRepository.findByUserId(userId)
                .orElse(new InstagramAccount());

        account.setUserId(userId);
        account.setInstagramUserId(profile.getId());
        account.setInstagramBusinessAccountId(instagramBusinessAccountId);
        account.setUsername(profile.getUsername());
        account.setName(profile.getName());
        account.setProfilePictureUrl(profile.getProfilePictureUrl());
        account.setBiography(profile.getBiography());
        account.setWebsite(profile.getWebsite());
        account.setFollowersCount(profile.getFollowersCount());
        account.setFollowingCount(profile.getFollowingCount());
        account.setMediaCount(profile.getMediaCount());
        account.setAccessToken(longLivedToken.getAccessToken());
        account.setTokenType(longLivedToken.getTokenType());
        account.setTokenExpiresAt(expiresAt);
        account.setIsActive(true);

        accountRepository.save(account);

        logger.info("Successfully linked Instagram account: {} for user: {}", profile.getUsername(), userId);

        return LinkAccountResponse.builder()
                .userId(userId)
                .instagramUserId(profile.getId())
                .username(profile.getUsername())
                .name(profile.getName())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .tokenExpiresAt(expiresAt)
                .message("Instagram account linked successfully")
                .success(true)
                .build();
    }

    public InstagramDataResponse fetchData(String userId, Integer mediaLimit) {
        logger.info("Fetching Instagram data for user: {}", userId);

        InstagramAccount account = accountRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AccountNotFoundException("No active Instagram account found for user: " + userId));

        // Check if token is expired or about to expire
        if (account.getTokenExpiresAt() != null &&
                account.getTokenExpiresAt().isBefore(LocalDateTime.now().plusDays(7))) {
            logger.info("Token expiring soon, attempting refresh");
            refreshAccountToken(account);
        }

        InstagramDataResponse data = dataService.fetchAllData(
                account.getAccessToken(),
                account.getInstagramBusinessAccountId(),
                mediaLimit);

        // Update account with latest profile data
        updateAccountFromProfile(account, data.getProfile());

        // Persist media to database
        dataService.persistMedia(account, data.getMedia());

        return data;
    }

    @Transactional
    public LinkAccountResponse refreshToken(String userId) {
        logger.info("Refreshing token for user: {}", userId);

        InstagramAccount account = accountRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AccountNotFoundException("No active Instagram account found for user: " + userId));

        refreshAccountToken(account);

        return LinkAccountResponse.builder()
                .userId(userId)
                .instagramUserId(account.getInstagramUserId())
                .username(account.getUsername())
                .name(account.getName())
                .profilePictureUrl(account.getProfilePictureUrl())
                .tokenExpiresAt(account.getTokenExpiresAt())
                .message("Token refreshed successfully")
                .success(true)
                .build();
    }

    @Transactional
    public void unlinkAccount(String userId, boolean deleteData) {
        logger.info("Unlinking Instagram account for user: {}, deleteData: {}", userId, deleteData);

        InstagramAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("No Instagram account found for user: " + userId));

        if (deleteData) {
            mediaRepository.deleteByAccount(account);
            accountRepository.delete(account);
            logger.info("Deleted all data for user: {}", userId);
        } else {
            account.setIsActive(false);
            account.setAccessToken(null);
            accountRepository.save(account);
            logger.info("Deactivated account for user: {}", userId);
        }
    }

    public InstagramAccount getAccount(String userId) {
        return accountRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new AccountNotFoundException("No active Instagram account found for user: " + userId));
    }

    public boolean isAccountLinked(String userId) {
        return accountRepository.existsByUserIdAndIsActiveTrue(userId);
    }

    private void refreshAccountToken(InstagramAccount account) {
        try {
            InstagramTokenResponse refreshedToken = oAuthService.refreshToken(account.getAccessToken());

            account.setAccessToken(refreshedToken.getAccessToken());
            account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(
                    refreshedToken.getExpiresIn() != null ? refreshedToken.getExpiresIn() : 5184000L));

            accountRepository.save(account);
            logger.info("Token refreshed successfully for account: {}", account.getUsername());
        } catch (Exception e) {
            logger.error("Failed to refresh token for account: {}", account.getUsername(), e);
            throw new InstagramApiException("Failed to refresh token. User may need to re-authenticate.", e);
        }
    }

    private void updateAccountFromProfile(InstagramAccount account, InstagramProfileDTO profile) {
        account.setUsername(profile.getUsername());
        account.setName(profile.getName());
        account.setProfilePictureUrl(profile.getProfilePictureUrl());
        account.setBiography(profile.getBiography());
        account.setWebsite(profile.getWebsite());
        account.setFollowersCount(profile.getFollowersCount());
        account.setFollowingCount(profile.getFollowingCount());
        account.setMediaCount(profile.getMediaCount());
        accountRepository.save(account);
    }
}
