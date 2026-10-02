package com.paymentledger.ledger.security;

import java.util.UUID;

public record UserPrincipal(UUID userId, String email) {
}
