package com.shogunsakura.demo.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Render の PostgreSQL 接続文字列を Spring JDBC 用に自動変換する。
 *
 * Render の External Database URL は postgres:// 形式で配られることがあるため、
 * そのままでは Spring Boot の datasource.url に使えない。
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final String PROPERTY_SOURCE_NAME = "render-database-url";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> overrides = new LinkedHashMap<>();

    String existingJdbcUrl = environment.getProperty("spring.datasource.url");
    if (!StringUtils.hasText(existingJdbcUrl)) {
      String jdbcUrl = toJdbcUrl(environment.getProperty("DATABASE_URL"));
      if (!StringUtils.hasText(jdbcUrl)) {
        jdbcUrl = toJdbcUrl(environment.getProperty("DATABASE_INTERNAL_URL"));
      }
      if (StringUtils.hasText(jdbcUrl)) {
        overrides.put("spring.datasource.url", jdbcUrl);
      }
    }

    if (!StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
      String username = firstText(
          environment.getProperty("DATABASE_USERNAME"),
          environment.getProperty("PGUSER"));
      if (StringUtils.hasText(username)) {
        overrides.put("spring.datasource.username", username);
      }
    }

    if (!StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
      String password = firstText(
          environment.getProperty("DATABASE_PASSWORD"),
          environment.getProperty("PGPASSWORD"));
      if (StringUtils.hasText(password)) {
        overrides.put("spring.datasource.password", password);
      }
    }

    if (!StringUtils.hasText(environment.getProperty("spring.datasource.driver-class-name"))) {
      overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
    }

    if (!overrides.isEmpty()) {
      environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }
  }

  private String toJdbcUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    String trimmed = value.trim();
    if (trimmed.startsWith("jdbc:")) {
      return trimmed;
    }
    if (trimmed.startsWith("postgres://")) {
      return "jdbc:postgresql://" + trimmed.substring("postgres://".length());
    }
    if (trimmed.startsWith("postgresql://")) {
      return "jdbc:postgresql://" + trimmed.substring("postgresql://".length());
    }
    return null;
  }

  private String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
