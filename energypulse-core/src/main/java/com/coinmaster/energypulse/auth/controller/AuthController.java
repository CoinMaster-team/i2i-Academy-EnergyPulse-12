package com.coinmaster.energypulse.auth.controller;

import com.coinmaster.energypulse.auth.dto.AuthPrincipal;
import com.coinmaster.energypulse.auth.dto.AuthResponse;
import com.coinmaster.energypulse.auth.dto.AuthUserResponse;
import com.coinmaster.energypulse.auth.dto.LoginRequest;
import com.coinmaster.energypulse.auth.dto.RegisterRequest;
import com.coinmaster.energypulse.auth.service.AuthService;
import com.coinmaster.energypulse.auth.web.AuthInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(AuthInterceptor.bearerToken(authorizationHeader));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(
            @RequestAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE)
            AuthPrincipal principal) {
        return ResponseEntity.ok(new AuthUserResponse(
                principal.id(),
                principal.fullName(),
                principal.email()));
    }
}
