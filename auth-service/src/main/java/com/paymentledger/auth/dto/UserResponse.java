package com.paymentledger.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String role,
    OffsetDateTime createdAt
) {}
