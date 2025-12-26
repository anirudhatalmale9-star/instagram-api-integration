package com.instagram.api.controller;

import com.instagram.api.dto.*;
import com.instagram.api.service.InstagramAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instagram")
@Validated
@Tag(name = "Instagram API", description = "Endpoints for Instagram Graph API integration - OAuth linking, data fetching, and token management")
public class InstagramController {

    private static final Logger logger = LoggerFactory.getLogger(InstagramController.class);

    private final InstagramAccountService accountService;

    public InstagramController(InstagramAccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Initiate Instagram OAuth Flow",
            description = "Generates an authorization URL for the user to authenticate with Instagram/Facebook. " +
                    "Redirect the user to the returned URL to complete the OAuth flow."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Authorization URL generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/link")
    public ResponseEntity<ApiResponse<OAuthUrlResponse>> initiateLink(
            @Parameter(description = "Your application's unique user identifier", required = true)
            @RequestParam @NotBlank String userId) {
        logger.info("Initiating Instagram link for user: {}", userId);

        OAuthUrlResponse response = accountService.initiateOAuth(userId);

        return ResponseEntity.ok(ApiResponse.success(
                "Redirect user to the authorization URL to complete linking",
                response));
    }

    @Operation(
            summary = "OAuth Callback Handler",
            description = "Handles the OAuth callback from Facebook/Instagram. This endpoint is called automatically " +
                    "after user authorization. Exchanges the code for access tokens and stores account data."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account linked successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired state parameter"
            )
    })
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LinkAccountResponse>> handleCallback(
            @Parameter(description = "Authorization code from OAuth redirect", required = true)
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF protection", required = true)
            @RequestParam String state) {
        logger.info("Handling OAuth callback with state: {}", state);

        LinkAccountResponse response = accountService.handleOAuthCallback(code, state);

        return ResponseEntity.ok(ApiResponse.success(
                "Instagram account linked successfully",
                response));
    }

    @Operation(
            summary = "Fetch Instagram Data",
            description = "Retrieves the user's Instagram profile details and recent media (posts/reels). " +
                    "Data is automatically persisted to the database. Returns profile info, followers/following count, and media list."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Data fetched successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No linked Instagram account found"
            )
    })
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<InstagramDataResponse>> fetchData(
            @Parameter(description = "Your application's unique user identifier", required = true)
            @RequestParam @NotBlank String userId,
            @Parameter(description = "Number of media items to fetch (default: 25, max: 100)")
            @RequestParam(required = false, defaultValue = "25") Integer mediaLimit) {
        logger.info("Fetching Instagram data for user: {}, mediaLimit: {}", userId, mediaLimit);

        InstagramDataResponse response = accountService.fetchData(userId, mediaLimit);

        return ResponseEntity.ok(ApiResponse.success(
                "Instagram data fetched and stored successfully",
                response));
    }

    @Operation(
            summary = "Refresh Access Token",
            description = "Refreshes an expired or expiring Instagram access token. " +
                    "Long-lived tokens expire after 60 days and should be refreshed before expiration."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No linked Instagram account found"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LinkAccountResponse>> refreshToken(
            @Parameter(description = "Your application's unique user identifier", required = true)
            @RequestParam @NotBlank String userId) {
        logger.info("Refreshing token for user: {}", userId);

        LinkAccountResponse response = accountService.refreshToken(userId);

        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed successfully",
                response));
    }

    @Operation(
            summary = "Unlink Instagram Account",
            description = "Revokes Instagram access and optionally deletes all stored data. " +
                    "Set deleteData=true to permanently remove all account and media data."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account unlinked successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No Instagram account found"
            )
    })
    @DeleteMapping("/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkAccount(
            @Parameter(description = "Your application's unique user identifier", required = true)
            @RequestParam @NotBlank String userId,
            @Parameter(description = "If true, permanently deletes all stored data")
            @RequestParam(required = false, defaultValue = "false") boolean deleteData) {
        logger.info("Unlinking Instagram account for user: {}, deleteData: {}", userId, deleteData);

        accountService.unlinkAccount(userId, deleteData);

        String message = deleteData
                ? "Instagram account unlinked and all data deleted"
                : "Instagram account unlinked (data retained)";

        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @Operation(
            summary = "Check Link Status",
            description = "Checks if an Instagram account is linked for the specified user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully"
            )
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkStatus(
            @Parameter(description = "Your application's unique user identifier", required = true)
            @RequestParam @NotBlank String userId) {
        logger.info("Checking Instagram link status for user: {}", userId);

        boolean isLinked = accountService.isAccountLinked(userId);

        return ResponseEntity.ok(ApiResponse.success(
                isLinked ? "Instagram account is linked" : "No Instagram account linked",
                isLinked));
    }
}
