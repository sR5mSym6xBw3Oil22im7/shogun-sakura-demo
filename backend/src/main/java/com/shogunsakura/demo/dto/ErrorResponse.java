package com.shogunsakura.demo.dto;

import java.util.Map;

public record ErrorResponse(
    String message,
    Map<String, String> fieldErrors
) {
}
