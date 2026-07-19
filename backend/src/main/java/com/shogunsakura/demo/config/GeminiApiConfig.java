package com.shogunsakura.demo.config;

import org.springframework.stereotype.Component;

@Component
public class GeminiApiConfig {

  private static final String API_KEY = "AQ.Ab8RN6KaT1m6eE3V6WJyth0saMkfapTHGZ17K-eCuypTUK3fGQ";
  public static final String MODEL_ID = "gemini-2.5-flash-lite";

  public String apiKey() {
    return API_KEY;
  }
}
