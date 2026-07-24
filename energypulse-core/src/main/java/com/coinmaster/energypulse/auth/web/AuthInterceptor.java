package com.coinmaster.energypulse.auth.web;

import com.coinmaster.energypulse.auth.dto.AuthPrincipal;
import com.coinmaster.energypulse.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "authPrincipal";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || isPublicAuthEndpoint(request)) {
            return true;
        }

        AuthPrincipal principal = authService.authenticate(
                bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION)));
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        return true;
    }

    public static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring("Bearer ".length()).trim();
    }

    private boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login");
    }
}
