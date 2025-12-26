package com.instagram.api.controller;

import com.instagram.api.dto.*;
import com.instagram.api.service.InstagramAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstagramController.class)
class InstagramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstagramAccountService accountService;

    private OAuthUrlResponse oAuthUrlResponse;
    private LinkAccountResponse linkAccountResponse;
    private InstagramDataResponse dataResponse;

    @BeforeEach
    void setUp() {
        oAuthUrlResponse = OAuthUrlResponse.builder()
                .authorizationUrl("https://facebook.com/oauth/authorize?...")
                .state("test-state-123")
                .build();

        linkAccountResponse = LinkAccountResponse.builder()
                .userId("user123")
                .instagramUserId("ig123")
                .username("testuser")
                .name("Test User")
                .profilePictureUrl("https://example.com/pic.jpg")
                .tokenExpiresAt(LocalDateTime.now().plusDays(60))
                .message("Success")
                .success(true)
                .build();

        InstagramProfileDTO profile = InstagramProfileDTO.builder()
                .id("ig123")
                .username("testuser")
                .name("Test User")
                .followersCount(1000)
                .followingCount(500)
                .mediaCount(50)
                .build();

        dataResponse = InstagramDataResponse.builder()
                .profile(profile)
                .media(Collections.emptyList())
                .build();
    }

    @Test
    @WithMockUser
    void initiateLink_ShouldReturnAuthorizationUrl() throws Exception {
        when(accountService.initiateOAuth(anyString())).thenReturn(oAuthUrlResponse);

        mockMvc.perform(get("/api/instagram/link")
                        .param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authorizationUrl").exists())
                .andExpect(jsonPath("$.data.state").value("test-state-123"));

        verify(accountService).initiateOAuth("user123");
    }

    @Test
    @WithMockUser
    void handleCallback_ShouldReturnLinkAccountResponse() throws Exception {
        when(accountService.handleOAuthCallback(anyString(), anyString()))
                .thenReturn(linkAccountResponse);

        mockMvc.perform(get("/api/instagram/callback")
                        .param("code", "auth-code-123")
                        .param("state", "test-state-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.instagramUserId").value("ig123"));

        verify(accountService).handleOAuthCallback("auth-code-123", "test-state-123");
    }

    @Test
    @WithMockUser
    void fetchData_ShouldReturnInstagramData() throws Exception {
        when(accountService.fetchData(anyString(), anyInt())).thenReturn(dataResponse);

        mockMvc.perform(get("/api/instagram/data")
                        .param("userId", "user123")
                        .param("mediaLimit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.username").value("testuser"))
                .andExpect(jsonPath("$.data.profile.followersCount").value(1000));

        verify(accountService).fetchData("user123", 25);
    }

    @Test
    @WithMockUser
    void refreshToken_ShouldReturnUpdatedAccount() throws Exception {
        when(accountService.refreshToken(anyString())).thenReturn(linkAccountResponse);

        mockMvc.perform(post("/api/instagram/refresh")
                        .param("userId", "user123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(accountService).refreshToken("user123");
    }

    @Test
    @WithMockUser
    void unlinkAccount_ShouldReturnSuccess() throws Exception {
        doNothing().when(accountService).unlinkAccount(anyString(), anyBoolean());

        mockMvc.perform(delete("/api/instagram/unlink")
                        .param("userId", "user123")
                        .param("deleteData", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(accountService).unlinkAccount("user123", true);
    }

    @Test
    @WithMockUser
    void checkStatus_ShouldReturnLinkStatus() throws Exception {
        when(accountService.isAccountLinked(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/instagram/status")
                        .param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        verify(accountService).isAccountLinked("user123");
    }
}
