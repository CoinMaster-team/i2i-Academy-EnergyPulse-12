package com.coinmaster.energypulse.auth.dto;

import java.util.UUID;

public record AuthPrincipal(
        UUID id,
        String fullName,
        String email) {
}
