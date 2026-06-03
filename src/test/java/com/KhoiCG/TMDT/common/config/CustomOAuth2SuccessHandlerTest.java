package com.KhoiCG.TMDT.common.config;

import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import com.KhoiCG.TMDT.modules.auth.service.TokenService;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2SuccessHandlerTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenService tokenService;
    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @InjectMocks
    private CustomOAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "secureCookies", false);
        ReflectionTestUtils.setField(successHandler, "refreshCookieSameSite", "Lax");
        ReflectionTestUtils.setField(successHandler, "defaultOAuth2RedirectUri", "http://localhost:3002/oauth2/redirect");
        ReflectionTestUtils.setField(
                successHandler,
                "authorizedRedirectUris",
                "http://localhost:3002/oauth2/redirect,http://localhost:3003/oauth2/redirect"
        );
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getRefreshExpiration()).thenReturn(604800000L);
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("OAuth success: map provider GOOGLE and keep allowed redirect")
    void onAuthenticationSuccess_Google_MapsProviderAndRedirectsAllowedUri() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3002/oauth2/redirect"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauthToken(
                "google",
                Map.of("email", "user@gmail.com", "name", "User", "sub", "google-sub-1")
        );
        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertEquals(AuthProvider.GOOGLE, userCaptor.getValue().getProviders().get(0).getProvider());
        assertTrue(response.getRedirectedUrl().startsWith("http://localhost:3002/oauth2/redirect?oauth=success"));
    }

    @Test
    @DisplayName("OAuth success: map provider FACEBOOK and read provider id from id")
    void onAuthenticationSuccess_Facebook_MapsProviderAndUsesIdClaim() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:3003/oauth2/redirect"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauthToken(
                "facebook",
                Map.of("email", "fb@user.com", "name", "FB User", "id", "fb-id-9")
        );
        when(userRepo.findByEmail("fb@user.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertEquals(AuthProvider.FACEBOOK, userCaptor.getValue().getProviders().get(0).getProvider());
        assertEquals("fb-id-9", userCaptor.getValue().getProviders().get(0).getProviderUserId());
        assertTrue(response.getRedirectedUrl().startsWith("http://localhost:3003/oauth2/redirect?oauth=success"));
    }

    @Test
    @DisplayName("OAuth success: reject untrusted redirect URI and fallback default")
    void onAuthenticationSuccess_UntrustedRedirect_FallsBackToDefault() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://evil.com/oauth2/redirect"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauthToken(
                "google",
                Map.of("email", "safe@user.com", "name", "Safe User", "sub", "safe-sub-1")
        );
        when(userRepo.findByEmail("safe@user.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertTrue(response.getRedirectedUrl().startsWith("http://localhost:3002/oauth2/redirect?oauth=success"));
        verify(cookieAuthorizationRequestRepository).removeAuthorizationRequestCookies(request, response);
    }

    private OAuth2AuthenticationToken oauthToken(String registrationId, Map<String, Object> attributes) {
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), registrationId);
    }
}
