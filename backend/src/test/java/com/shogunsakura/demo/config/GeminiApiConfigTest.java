package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeminiApiConfigTest {

  @Test
  void rejectsMissingApiKey() {
    assertThatThrownBy(() -> new GeminiApiConfig((String) null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GEMINI_API_KEY");
  }

  @Test
  void trimsConfiguredApiKey() {
    GeminiApiConfig config = new GeminiApiConfig("  test-key  ");

    assertThat(config.apiKey()).isEqualTo("test-key");
  }
}
