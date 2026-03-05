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
    private final TokenService tokenService; // 🌟 Inject TokenService để lưu RefreshToken

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        // Tìm hoặc tạo user
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

        // 🌟 1. CẤP PHÁT CẢ 2 TOKEN
        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // 🌟 2. LƯU REFRESH TOKEN VÀO DATABASE
        tokenService.saveRefreshToken(user, refreshToken);

        // 🌟 3. CÀI ĐẶT HTTP-ONLY COOKIE CHO REFRESH TOKEN (Bảo mật tuyệt đối)
        // (Hàm addCookie của bạn trong CookieUtils đã mặc định set HttpOnly = true rồi)
        CookieUtils.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60); // 7 ngày

        // 🌟 4. CHUYỂN GIAO ACCESS TOKEN BẰNG COOKIE TẠM THỜI
        // Tạo một cookie thường (JS đọc được), chỉ sống 60 giây để Frontend đọc rồi tự hủy
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(false); // Cho phép JS (React/Vue) đọc được
        accessTokenCookie.setMaxAge(60); // Tự động chết sau 60 giây
        response.addCookie(accessTokenCookie);

        // 🌟 5. REDIRECT VỀ FRONTEND VỚI URL SẠCH SẼ (Không dính dáng gì tới Token)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3003/oauth2/redirect")
                // Đã XÓA dòng .queryParam("token", token) gây rò rỉ bảo mật
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}