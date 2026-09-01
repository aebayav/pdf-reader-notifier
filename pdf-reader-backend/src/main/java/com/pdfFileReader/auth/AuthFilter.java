package com.pdfFileReader.auth;

import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.domain.service.impl.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Bearer token dogrulamasi: /api/v1/auth/** ve CORS preflight (OPTIONS)
 * serbest, diger tum /api/** rotalari token ister. 401 dondugunde govde
 * JSON'dur (frontend parse eder).
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "auth.userId";
    public static final String ATTR_USERNAME = "auth.username";

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer "))
                ? header.substring("Bearer ".length()).trim()
                : null;

        Optional<User> user = authService.validate(token);

        if (user.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Oturum gecersiz veya suresi dolmus. Lutfen tekrar giris yapin.\"}");
            return;
        }

        request.setAttribute(ATTR_USER_ID, user.get().getId());
        request.setAttribute(ATTR_USERNAME, user.get().getUsername());
        filterChain.doFilter(request, response);
    }
}
