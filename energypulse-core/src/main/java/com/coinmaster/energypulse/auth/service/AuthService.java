package com.coinmaster.energypulse.auth.service;

import com.coinmaster.energypulse.auth.domain.AuthSession;
import com.coinmaster.energypulse.auth.domain.UserAccount;
import com.coinmaster.energypulse.auth.dto.AuthPrincipal;
import com.coinmaster.energypulse.auth.dto.AuthResponse;
import com.coinmaster.energypulse.auth.dto.AuthUserResponse;
import com.coinmaster.energypulse.auth.dto.LoginRequest;
import com.coinmaster.energypulse.auth.dto.RegisterRequest;
import com.coinmaster.energypulse.auth.exception.AuthenticationException;
import com.coinmaster.energypulse.auth.repository.AuthSessionRepository;
import com.coinmaster.energypulse.auth.repository.UserAccountRepository;
import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Duration sessionDuration;

    public AuthService(
            UserAccountRepository userRepository,
            AuthSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.session-duration-hours:24}") long sessionDurationHours) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionDuration = Duration.ofHours(Math.max(1, sessionDurationHours));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(
                    "EMAIL_ALREADY_REGISTERED",
                    "An account already exists for this email address.");
        }

        UserAccount user = userRepository.save(new UserAccount(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password())));

        return createSession(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(candidate -> passwordEncoder.matches(
                        request.password(),
                        candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationException(
                        "INVALID_CREDENTIALS",
                        "Email address or password is incorrect."));

        return createSession(user);
    }

    @Transactional(readOnly = true)
    public AuthPrincipal authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized();
        }

        AuthSession session = sessionRepository
                .findByTokenHashAndExpiresAtAfter(
                        hashToken(token),
                        OffsetDateTime.now(ZoneOffset.UTC))
                .orElseThrow(this::unauthorized);

        UserAccount user = session.getUser();
        return new AuthPrincipal(
                user.getId(),
                user.getFullName(),
                user.getEmail());
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionRepository.deleteByTokenHash(hashToken(token));
        }
    }

    private AuthResponse createSession(UserAccount user) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
        OffsetDateTime expiresAt = OffsetDateTime
                .now(ZoneOffset.UTC)
                .plus(sessionDuration);

        sessionRepository.save(new AuthSession(
                user,
                hashToken(token),
                expiresAt));

        return new AuthResponse(
                token,
                expiresAt,
                new AuthUserResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private AuthenticationException unauthorized() {
        return new AuthenticationException(
                "AUTHENTICATION_REQUIRED",
                "A valid session token is required.");
    }
}
