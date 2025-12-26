package com.instagram.api.service;

import com.instagram.api.dto.*;
import com.instagram.api.entity.InstagramAccount;
import com.instagram.api.exception.AccountNotFoundException;
import com.instagram.api.repository.InstagramAccountRepository;
import com.instagram.api.repository.InstagramMediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstagramAccountServiceTest {

    @Mock
    private InstagramAccountRepository accountRepository;

    @Mock
    private InstagramMediaRepository mediaRepository;

    @Mock
    private InstagramOAuthService oAuthService;

    @Mock
    private InstagramDataService dataService;

    @InjectMocks
    private InstagramAccountService accountService;

    private InstagramAccount testAccount;
    private InstagramProfileDTO testProfile;

    @BeforeEach
    void setUp() {
        testAccount = InstagramAccount.builder()
                .id(1L)
                .userId("user123")
                .instagramUserId("ig123")
                .instagramBusinessAccountId("business123")
                .username("testuser")
                .name("Test User")
                .accessToken("test-access-token")
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .isActive(true)
                .build();

        testProfile = InstagramProfileDTO.builder()
                .id("ig123")
                .username("testuser")
                .name("Test User")
                .followersCount(1000)
                .followingCount(500)
                .mediaCount(50)
                .build();
    }

    @Test
    void initiateOAuth_ShouldReturnAuthorizationUrl() {
        OAuthUrlResponse expected = OAuthUrlResponse.builder()
                .authorizationUrl("https://facebook.com/oauth")
                .state("state123")
                .build();

        when(oAuthService.generateAuthorizationUrl(anyString())).thenReturn(expected);

        OAuthUrlResponse result = accountService.initiateOAuth("user123");

        assertNotNull(result);
        assertEquals(expected.getAuthorizationUrl(), result.getAuthorizationUrl());
        verify(oAuthService).generateAuthorizationUrl("user123");
    }

    @Test
    void handleOAuthCallback_ShouldCreateNewAccount() {
        String code = "auth-code";
        String state = "state123";
        String userId = "user123";

        InstagramTokenResponse shortToken = InstagramTokenResponse.builder()
                .accessToken("short-token")
                .build();

        InstagramTokenResponse longToken = InstagramTokenResponse.builder()
                .accessToken("long-token")
                .expiresIn(5184000L)
                .build();

        when(oAuthService.validateStateAndGetUserId(state)).thenReturn(userId);
        when(oAuthService.exchangeCodeForToken(code)).thenReturn(shortToken);
        when(oAuthService.exchangeLongLivedToken(anyString())).thenReturn(longToken);
        when(dataService.getInstagramBusinessAccountId(anyString())).thenReturn("business123");
        when(dataService.fetchProfile(anyString(), anyString())).thenReturn(testProfile);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(accountRepository.save(any(InstagramAccount.class))).thenAnswer(i -> i.getArgument(0));

        LinkAccountResponse result = accountService.handleOAuthCallback(code, state);

        assertTrue(result.isSuccess());
        assertEquals("testuser", result.getUsername());
        verify(accountRepository).save(any(InstagramAccount.class));
    }

    @Test
    void fetchData_ShouldReturnInstagramData() {
        InstagramDataResponse expectedData = InstagramDataResponse.builder()
                .profile(testProfile)
                .media(Collections.emptyList())
                .build();

        when(accountRepository.findByUserIdAndIsActiveTrue("user123"))
                .thenReturn(Optional.of(testAccount));
        when(dataService.fetchAllData(anyString(), anyString(), anyInt()))
                .thenReturn(expectedData);
        when(accountRepository.save(any(InstagramAccount.class))).thenReturn(testAccount);

        InstagramDataResponse result = accountService.fetchData("user123", 25);

        assertNotNull(result);
        assertEquals("testuser", result.getProfile().getUsername());
        verify(dataService).persistMedia(any(InstagramAccount.class), anyList());
    }

    @Test
    void fetchData_ShouldThrowWhenAccountNotFound() {
        when(accountRepository.findByUserIdAndIsActiveTrue("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.fetchData("unknown", 25));
    }

    @Test
    void refreshToken_ShouldUpdateTokenExpiry() {
        InstagramTokenResponse refreshedToken = InstagramTokenResponse.builder()
                .accessToken("new-token")
                .expiresIn(5184000L)
                .build();

        when(accountRepository.findByUserIdAndIsActiveTrue("user123"))
                .thenReturn(Optional.of(testAccount));
        when(oAuthService.refreshToken(anyString())).thenReturn(refreshedToken);
        when(accountRepository.save(any(InstagramAccount.class))).thenReturn(testAccount);

        LinkAccountResponse result = accountService.refreshToken("user123");

        assertTrue(result.isSuccess());
        verify(accountRepository, times(1)).save(any(InstagramAccount.class));
    }

    @Test
    void unlinkAccount_WithDeleteData_ShouldDeleteAllData() {
        when(accountRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testAccount));
        doNothing().when(mediaRepository).deleteByAccount(any(InstagramAccount.class));
        doNothing().when(accountRepository).delete(any(InstagramAccount.class));

        accountService.unlinkAccount("user123", true);

        verify(mediaRepository).deleteByAccount(testAccount);
        verify(accountRepository).delete(testAccount);
    }

    @Test
    void unlinkAccount_WithoutDeleteData_ShouldDeactivateAccount() {
        when(accountRepository.findByUserId("user123"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(InstagramAccount.class))).thenReturn(testAccount);

        accountService.unlinkAccount("user123", false);

        verify(accountRepository).save(argThat(account ->
                !account.getIsActive() && account.getAccessToken() == null));
        verify(mediaRepository, never()).deleteByAccount(any());
    }

    @Test
    void isAccountLinked_ShouldReturnTrue_WhenActiveAccountExists() {
        when(accountRepository.existsByUserIdAndIsActiveTrue("user123"))
                .thenReturn(true);

        assertTrue(accountService.isAccountLinked("user123"));
    }

    @Test
    void isAccountLinked_ShouldReturnFalse_WhenNoActiveAccount() {
        when(accountRepository.existsByUserIdAndIsActiveTrue("user123"))
                .thenReturn(false);

        assertFalse(accountService.isAccountLinked("user123"));
    }
}
