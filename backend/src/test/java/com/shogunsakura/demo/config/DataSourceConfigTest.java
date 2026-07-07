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
        .isEqualTo("jdbc:postgresql://example.com:5432/demo?user=user&password=pass");
  }

  @Test
  void resolveJdbcUrlPreservesSslModeAndCredentials() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DATABASE_URL", "postgres://user:pass@example.com:5432/demo?sslmode=require");

    assertThat(DataSourceConfig.resolveJdbcUrl(environment))
        .isEqualTo("jdbc:postgresql://example.com:5432/demo?sslmode=require&user=user&password=pass");
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
