package com.shogunsakura.demo.model;

import java.time.OffsetDateTime;

public record OrderReceipt(
    long id,
    OffsetDateTime createdAt
) {
}
