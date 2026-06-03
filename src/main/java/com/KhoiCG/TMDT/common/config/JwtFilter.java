package com.KhoiCG.TMDT.common.config;

import com.KhoiCG.TMDT.common.utils.CookieUtils;
import com.KhoiCG.TMDT.modules.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Lazy
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String token;
        final String userEmail;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = CookieUtils.getCookie(request, "admin_token")
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .orElse(null);
        }

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Refresh token không được dùng để xác thực request
        if (!jwtService.isAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            userEmail = jwtService.extractUserName(token);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.validateToken(token, userDetails) && userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Token sai định dạng / hết hạn / ký không hợp lệ: không chặn request (endpoint public vẫn hoạt động).
            // Route yêu cầu đăng nhập sẽ do SecurityFilterChain trả 401 vì không có authentication.
            log.warn("JWT skipped for {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }
}