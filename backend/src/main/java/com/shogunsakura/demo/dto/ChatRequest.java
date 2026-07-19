package com.shogunsakura.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
    @NotBlank(message = "メッセージを入力してください。")
    @Size(max = 500, message = "メッセージは500文字以内で入力してください。")
    String message
) {
}
