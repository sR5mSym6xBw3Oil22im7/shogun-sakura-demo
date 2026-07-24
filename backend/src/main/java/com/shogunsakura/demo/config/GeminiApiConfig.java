package com.shogunsakura.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiConfig {

  private static final String DEFAULT_API_KEY = "AQ.Ab8RN6KaT1m6eE3V6WJyth0saMkfapTHGZ17K-eCuypTUK3fGQ";

  @Value("${GEMINI_API_KEY:not-configured}")
  private String apiKey = DEFAULT_API_KEY;
  public static final String MODEL_ID = "gemini-flash-lite-latest";

  public String apiKey() {
    if (apiKey == null || apiKey.isBlank() || "not-configured".equals(apiKey.trim())) {
      return DEFAULT_API_KEY;
    }
    return apiKey.trim();
  }
}
