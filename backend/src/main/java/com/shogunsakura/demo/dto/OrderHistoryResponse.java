package com.shogunsakura.demo.dto;

import java.time.OffsetDateTime;

public record OrderHistoryResponse(
    long orderId,
    String productName,
    String customerName,
    String email,
    String postalCode,
    String address,
    int quantity,
    int totalAmount,
    String note,
    OffsetDateTime createdAt
) {
}
