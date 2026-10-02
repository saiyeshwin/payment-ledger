package com.paymentledger.reporting.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(int status, String error, String message, OffsetDateTime timestamp) {
    public ApiErrorResponse(int status, String error, String message) {
        this(status, error, message, OffsetDateTime.now());
    }
}
