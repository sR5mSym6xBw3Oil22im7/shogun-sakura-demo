package com.shogunsakura.demo.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GeminiApiConfig {

  public static final String MODEL_ID = "gemini-flash-lite-latest";

  private final String apiKey;

  public GeminiApiConfig(Environment environment) {
    this(environment.getRequiredProperty("GEMINI_API_KEY"));
  }

  public GeminiApiConfig() {
    this((String) null);
  }

  public GeminiApiConfig(String apiKey) {
    this.apiKey = normalizeApiKey(apiKey);
  }

  public String apiKey() {
    return apiKey;
  }

  private static String normalizeApiKey(String configuredApiKey) {
    if (!StringUtils.hasText(configuredApiKey)) {
      throw new IllegalStateException("GEMINI_API_KEY environment variable is required.");
    }
    return configuredApiKey.trim();
  }
}
