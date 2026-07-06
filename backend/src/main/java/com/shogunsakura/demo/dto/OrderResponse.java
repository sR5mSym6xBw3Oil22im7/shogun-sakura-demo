package com.shogunsakura.demo.dto;

import java.time.OffsetDateTime;

public record OrderResponse(
    long orderId,
    String message,
    OffsetDateTime createdAt
) {
}
