package com.coinmaster.energypulse.auth.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String token,
        OffsetDateTime expiresAt,
        AuthUserResponse user) {
}
