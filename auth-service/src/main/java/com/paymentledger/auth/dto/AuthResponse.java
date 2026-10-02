package com.paymentledger.auth.dto;

import java.util.UUID;

public record AuthResponse(
    String token,
    String tokenType,
    UUID userId,
    String name,
    String email,
    String role
) {
    public AuthResponse(String token, UUID userId, String name, String email, String role) {
        this(token, "Bearer", userId, name, email, role);
    }
}
