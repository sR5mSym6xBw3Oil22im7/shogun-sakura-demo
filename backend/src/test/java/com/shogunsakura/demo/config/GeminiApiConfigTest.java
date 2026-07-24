package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

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

  @Test
  void resolvesApiKeyFromSystemPropertyWhenNoSpringPropertyIsPresent() {
    String original = System.getProperty("GEMINI_API_KEY");
    try {
      System.setProperty("GEMINI_API_KEY", "  system-key  ");

      GeminiApiConfig config = new GeminiApiConfig(new MockEnvironment());

      assertThat(config.apiKey()).isEqualTo("system-key");
    } finally {
      if (original == null) {
        System.clearProperty("GEMINI_API_KEY");
      } else {
        System.setProperty("GEMINI_API_KEY", original);
      }
    }
  }

  @Test
  void resolvesApiKeyFromSystemEnvironmentInDefaultConstructor() {
    String original = System.getProperty("GEMINI_API_KEY");
    try {
      System.clearProperty("GEMINI_API_KEY");
      System.setProperty("GEMINI_API_KEY", "  default-constructor-key  ");

      GeminiApiConfig config = new GeminiApiConfig();

      assertThat(config.apiKey()).isEqualTo("default-constructor-key");
    } finally {
      if (original == null) {
        System.clearProperty("GEMINI_API_KEY");
      } else {
        System.setProperty("GEMINI_API_KEY", original);
      }
    }
  }
}
