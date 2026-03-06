package com.KhoiCG.TMDT.common.config;

import com.KhoiCG.TMDT.common.utils.CookieUtils;
import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import com.KhoiCG.TMDT.modules.auth.service.TokenService; // 🌟 Thêm dòng này
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepo userRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .role("USER")
                            .isActive(true)
                            .build();

                    UserProvider googleProvider = UserProvider.builder()
                            .user(newUser)
                            .provider(AuthProvider.GOOGLE)
                            .providerUserId(providerId)
                            .build();

                    newUser.getProviders().add(googleProvider);
                    return userRepository.save(newUser);
                });


        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        tokenService.saveRefreshToken(user, refreshToken);

        CookieUtils.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);


        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(false);
        accessTokenCookie.setMaxAge(60);
        response.addCookie(accessTokenCookie);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3002/oauth2/redirect")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}