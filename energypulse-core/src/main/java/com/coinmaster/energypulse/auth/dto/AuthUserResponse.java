package com.coinmaster.energypulse.auth.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String fullName,
        String email) {
}
