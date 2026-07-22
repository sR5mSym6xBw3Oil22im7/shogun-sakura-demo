package com.shogunsakura.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiConfig {

  @Value("${GEMINI_API_KEY:not-configured}")
  private String apiKey = "not-configured";
  public static final String MODEL_ID = "gemini-2.5-flash-lite";

  public String apiKey() {
    return apiKey == null ? "" : apiKey.trim();
  }
}
