package com.KhoiCG.TMDT.common.config;
import com.KhoiCG.TMDT.modules.auth.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.auth.entity.User;
import com.KhoiCG.TMDT.modules.auth.repository.UserRepo;
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

        // Tìm hoặc tạo user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRole("USER");
                    newUser.setProvider(AuthProvider.GOOGLE);
                    newUser.setProviderId(providerId);
                    return userRepository.save(newUser);
                });

        // Tạo Token từ Email
        String token = jwtService.generateToken(user.getEmail());

        // Redirect về Frontend
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3003/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}