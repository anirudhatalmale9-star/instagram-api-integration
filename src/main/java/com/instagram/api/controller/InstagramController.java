package com.instagram.api.controller;

import com.instagram.api.dto.*;
import com.instagram.api.service.InstagramAccountService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram")
@Validated
public class InstagramController {

    private static final Logger logger = LoggerFactory.getLogger(InstagramController.class);

    private final InstagramAccountService accountService;

    public InstagramController(InstagramAccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Endpoint 1: Link Account - Initiates OAuth flow
     * GET /api/instagram/link?userId={userId}
     *
     * Returns the authorization URL that the user should be redirected to
     */
    @GetMapping("/link")
    public ResponseEntity<ApiResponse<OAuthUrlResponse>> initiateLink(
            @RequestParam @NotBlank String userId) {
        logger.info("Initiating Instagram link for user: {}", userId);

        OAuthUrlResponse response = accountService.initiateOAuth(userId);

        return ResponseEntity.ok(ApiResponse.success(
                "Redirect user to the authorization URL to complete linking",
                response));
    }

    /**
     * Endpoint 1b: OAuth Callback Handler
     * GET /api/instagram/callback?code={code}&state={state}
     *
     * Handles the OAuth callback from Instagram/Facebook
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LinkAccountResponse>> handleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        logger.info("Handling OAuth callback with state: {}", state);

        LinkAccountResponse response = accountService.handleOAuthCallback(code, state);

        return ResponseEntity.ok(ApiResponse.success(
                "Instagram account linked successfully",
                response));
    }

    /**
     * Endpoint 2: Fetch Data - Retrieves profile and media data
     * GET /api/instagram/data?userId={userId}&mediaLimit={limit}
     *
     * Fetches user's profile details, posts/reels, and stores them in the database
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<InstagramDataResponse>> fetchData(
            @RequestParam @NotBlank String userId,
            @RequestParam(required = false, defaultValue = "25") Integer mediaLimit) {
        logger.info("Fetching Instagram data for user: {}, mediaLimit: {}", userId, mediaLimit);

        InstagramDataResponse response = accountService.fetchData(userId, mediaLimit);

        return ResponseEntity.ok(ApiResponse.success(
                "Instagram data fetched and stored successfully",
                response));
    }

    /**
     * Endpoint 3a: Refresh Token
     * POST /api/instagram/refresh?userId={userId}
     *
     * Refreshes an expired or about-to-expire access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LinkAccountResponse>> refreshToken(
            @RequestParam @NotBlank String userId) {
        logger.info("Refreshing token for user: {}", userId);

        LinkAccountResponse response = accountService.refreshToken(userId);

        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed successfully",
                response));
    }

    /**
     * Endpoint 3b: Unlink Account
     * DELETE /api/instagram/unlink?userId={userId}&deleteData={deleteData}
     *
     * Revokes access and optionally deletes all stored data
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkAccount(
            @RequestParam @NotBlank String userId,
            @RequestParam(required = false, defaultValue = "false") boolean deleteData) {
        logger.info("Unlinking Instagram account for user: {}, deleteData: {}", userId, deleteData);

        accountService.unlinkAccount(userId, deleteData);

        String message = deleteData
                ? "Instagram account unlinked and all data deleted"
                : "Instagram account unlinked (data retained)";

        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    /**
     * Check if account is linked
     * GET /api/instagram/status?userId={userId}
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkStatus(
            @RequestParam @NotBlank String userId) {
        logger.info("Checking Instagram link status for user: {}", userId);

        boolean isLinked = accountService.isAccountLinked(userId);

        return ResponseEntity.ok(ApiResponse.success(
                isLinked ? "Instagram account is linked" : "No Instagram account linked",
                isLinked));
    }
}
