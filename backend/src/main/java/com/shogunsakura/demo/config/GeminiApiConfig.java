package com.shogunsakura.demo.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GeminiApiConfig {

  public static final String MODEL_ID = "gemini-flash-lite-latest";

  private final String apiKey;

  public GeminiApiConfig(Environment environment) {
    this(resolveApiKey(environment));
  }

  public GeminiApiConfig() {
    this(resolveApiKey());
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

  private static String resolveApiKey() {
    return resolveApiKey(null);
  }

  private static String resolveApiKey(Environment environment) {
    String configuredApiKey = null;
    if (environment != null) {
      configuredApiKey = environment.getProperty("GEMINI_API_KEY");
      if (!StringUtils.hasText(configuredApiKey)) {
        configuredApiKey = environment.getProperty("gemini.api.key");
      }
    }
    if (!StringUtils.hasText(configuredApiKey)) {
      configuredApiKey = System.getProperty("GEMINI_API_KEY");
    }
    if (!StringUtils.hasText(configuredApiKey)) {
      configuredApiKey = System.getenv("GEMINI_API_KEY");
    }
    return configuredApiKey;
  }
}
