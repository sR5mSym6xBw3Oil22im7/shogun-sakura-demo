package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigTest {

  @Test
  void normalizeJdbcUrlConvertsRenderPostgresUrl() {
    assertThat(DataSourceConfig.normalizeJdbcUrl("postgres://user:pass@example.com:5432/demo"))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo");
  }

  @Test
  void normalizeJdbcUrlKeepsJdbcUrlAsIs() {
    assertThat(DataSourceConfig.normalizeJdbcUrl("jdbc:postgresql://example.com:5432/demo"))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo");
  }

  @Test
  void resolveConnectionSettingsPrefersDatabaseUrlCredentials() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "postgres://render_user:render_pass@example.com:5432/demo")
        .withProperty("DATABASE_USERNAME", "wrong_user")
        .withProperty("DATABASE_PASSWORD", "wrong_pass");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://example.com:5432/demo");
    assertThat(settings.username()).isEqualTo("render_user");
    assertThat(settings.password()).isEqualTo("render_pass");
  }

  @Test
  void resolveConnectionSettingsFallsBackToSeparateValues() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_INTERNAL_URL", "postgresql://example.com:5432/demo")
        .withProperty("DATABASE_USERNAME", "demo_user")
        .withProperty("DATABASE_PASSWORD", "demo_pass");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://example.com:5432/demo");
    assertThat(settings.username()).isEqualTo("demo_user");
    assertThat(settings.password()).isEqualTo("demo_pass");
  }
}
