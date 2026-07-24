package com.coinmaster.energypulse.auth.service;

import com.coinmaster.energypulse.auth.domain.AuthSession;
import com.coinmaster.energypulse.auth.domain.UserAccount;
import com.coinmaster.energypulse.auth.dto.AuthPrincipal;
import com.coinmaster.energypulse.auth.dto.AuthResponse;
import com.coinmaster.energypulse.auth.dto.LoginRequest;
import com.coinmaster.energypulse.auth.dto.RegisterRequest;
import com.coinmaster.energypulse.auth.exception.AuthenticationException;
import com.coinmaster.energypulse.auth.repository.AuthSessionRepository;
import com.coinmaster.energypulse.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private AuthSessionRepository sessionRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                24);
    }

    @Test
    void shouldRegisterUserWithHashedPasswordAndSession() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com"))
                .thenReturn(false);
        when(userRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                "Energy User",
                " USER@Example.com ",
                "SecurePass123!"));

        ArgumentCaptor<UserAccount> userCaptor =
                ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("user@example.com", savedUser.getEmail());
        assertFalse(savedUser.getPasswordHash().contains("SecurePass123!"));
        assertTrue(passwordEncoder.matches(
                "SecurePass123!",
                savedUser.getPasswordHash()));
        assertNotNull(response.token());
        assertEquals("Energy User", response.user().fullName());
        verify(sessionRepository).save(any(AuthSession.class));
    }

    @Test
    void shouldRejectInvalidLoginCredentials() {
        UserAccount user = new UserAccount(
                "Energy User",
                "user@example.com",
                passwordEncoder.encode("CorrectPass123!"));
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(new LoginRequest(
                        "user@example.com",
                        "WrongPass123!")));

        assertEquals("INVALID_CREDENTIALS", exception.getCode());
    }

    @Test
    void shouldAuthenticateValidSessionToken() {
        UserAccount user = new UserAccount(
                "Energy User",
                "user@example.com",
                passwordEncoder.encode("SecurePass123!"));
        AuthSession session = new AuthSession(
                user,
                "hashed-token",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(sessionRepository.findByTokenHashAndExpiresAtAfter(
                anyString(),
                any(OffsetDateTime.class)))
                .thenReturn(Optional.of(session));

        AuthPrincipal principal = authService.authenticate("opaque-token");

        assertEquals("Energy User", principal.fullName());
        assertEquals("user@example.com", principal.email());
    }
}
