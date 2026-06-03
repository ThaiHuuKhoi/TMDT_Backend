package com.KhoiCG.TMDT.common.config;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.common.utils.CookieUtils;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.auth.service.AuthService;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${application.security.cookies.secure:true}")
    private boolean secureCookies;

    @Value("${application.security.cookies.refresh.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${application.frontend.oauth2.redirect-uri:http://localhost:3002/oauth2/redirect}")
    private String defaultOAuth2RedirectUri;

    @Value("${application.security.oauth2.authorized-redirect-uris:http://localhost:3002/oauth2/redirect,http://localhost:3003/oauth2/redirect}")
    private String authorizedRedirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = getProviderUserId(oAuth2User);
        AuthProvider provider = getAuthProvider(authentication);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth2 provider did not return email");
        }
        if (name == null || name.isBlank()) {
            name = email;
        }

        // find-or-create user + lưu refresh token trong một transaction duy nhất
        final String refreshToken;
        try {
            refreshToken = authService.processOAuth2Login(email, name, provider, providerId);
        } catch (ApiException ex) {
            log.warn("OAuth2 login rejected for email={} code={}", email, ex.getCode());
            cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            String errorRedirect = UriComponentsBuilder.fromUriString(defaultOAuth2RedirectUri)
                    .queryParam("error", ex.getCode().toLowerCase())
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorRedirect);
            return;
        }

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .maxAge(jwtService.getRefreshExpiration() / 1000)
                .sameSite(refreshCookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String targetRedirectUri = CookieUtils.getCookie(
                        request,
                        HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME
                )
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(v -> !v.isBlank())
                .orElse(defaultOAuth2RedirectUri);
        if (!isAuthorizedRedirectUri(targetRedirectUri)) {
            targetRedirectUri = defaultOAuth2RedirectUri;
        }

        String targetUrl = UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("oauth", "success")
                .build().toUriString();
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getProviderUserId(OAuth2User oAuth2User) {
        String sub = oAuth2User.getAttribute("sub");
        if (sub != null && !sub.isBlank()) {
            return sub;
        }
        String id = oAuth2User.getAttribute("id");
        if (id != null && !id.isBlank()) {
            return id;
        }
        throw new IllegalArgumentException("OAuth2 provider user id is missing");
    }

    private AuthProvider getAuthProvider(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return AuthProvider.GOOGLE;
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        if (registrationId == null) {
            return AuthProvider.GOOGLE;
        }
        return switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> AuthProvider.GOOGLE;
        };
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientUri;
        try {
            clientUri = URI.create(uri);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        List<String> allowedUris = Arrays.stream(authorizedRedirectUris.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
        return allowedUris.stream().anyMatch(allowed -> {
            URI allowedUri = URI.create(allowed);
            return allowedUri.getHost().equalsIgnoreCase(clientUri.getHost())
                    && allowedUri.getPort() == clientUri.getPort()
                    && allowedUri.getScheme().equalsIgnoreCase(clientUri.getScheme())
                    && allowedUri.getPath().equals(clientUri.getPath());
        });
    }
}
