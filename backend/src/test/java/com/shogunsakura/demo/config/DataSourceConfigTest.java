package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigTest {

  @Test
  void resolveJdbcUrlConvertsRenderPostgresUrl() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "postgres://user:pass@example.com:5432/demo");

    assertThat(DataSourceConfig.resolveJdbcUrl(environment))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo");
  }

  @Test
  void resolveJdbcUrlPreservesSslMode() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "postgres://user:pass@example.com:5432/demo?sslmode=require");

    assertThat(DataSourceConfig.resolveJdbcUrl(environment))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo?sslmode=require");
  }

  @Test
  void resolveConnectionSettingsPrefersCredentialsFromDatabaseUrl() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "postgres://render_user:render_pass@example.com:5432/demo")
        .withProperty("DATABASE_USERNAME", "wrong_user")
        .withProperty("DATABASE_PASSWORD", "wrong_pass");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.username()).isEqualTo("render_user");
    assertThat(settings.password()).isEqualTo("render_pass");
  }

  @Test
  void resolveJdbcUrlKeepsJdbcUrlAsIs() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "jdbc:postgresql://example.com:5432/demo");

    assertThat(DataSourceConfig.resolveJdbcUrl(environment))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo");
  }

  @Test
  void resolveJdbcUrlFallsBackToLocalDatabase() {
    MockEnvironment environment = new MockEnvironment();

    assertThat(DataSourceConfig.resolveJdbcUrl(environment))
        .isEqualTo("jdbc:postgresql://localhost:5432/shogun_sakura");
  }
}
