package com.shogunsakura.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
    @NotBlank(message = "お名前を入力してください。")
    @Size(max = 50, message = "お名前は50文字以内で入力してください。")
    String name,

    @NotBlank(message = "メールアドレスを入力してください。")
    @Email(message = "メールアドレスを正しい形式で入力してください。")
    @Size(max = 100, message = "メールアドレスは100文字以内で入力してください。")
    String email,

    @Pattern(regexp = "^[0-9-]{1,10}$", message = "郵便番号は数字とハイフンで入力してください。")
    String postalCode,

    @NotBlank(message = "住所を入力してください。")
    @Size(max = 200, message = "住所は200文字以内で入力してください。")
    String address,

    @NotNull(message = "数量を入力してください。")
    @Min(value = 1, message = "数量は1〜9の範囲で入力してください。")
    @Max(value = 9, message = "数量は1〜9の範囲で入力してください。")
    Integer quantity,

    @Size(max = 200, message = "備考は200文字以内で入力してください。")
    String note
) {
}
