package com.KhoiCG.TMDT.common.config;

import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        // Tìm hoặc tạo user (Đã cập nhật theo cấu trúc UserProvider mới)
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .role("USER")
                            .isActive(true)
                            .build();

                    // Tạo đối tượng Provider (Google)
                    UserProvider googleProvider = UserProvider.builder()
                            .user(newUser)
                            .provider(AuthProvider.GOOGLE)
                            .providerUserId(providerId)
                            .build();

                    // Thêm vào danh sách và nhờ Hibernate Cascade tự động lưu cả 2
                    newUser.getProviders().add(googleProvider);

                    return userRepository.save(newUser);
                });

        // Cấp phát Token mới
        String token = jwtService.generateToken(user.getEmail());

        // Redirect về Frontend (Mang theo token)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3003/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}