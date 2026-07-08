package com.shogunsakura.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfig {

  private static final String DEFAULT_LOCAL_JDBC_URL =
      "jdbc:postgresql://localhost:5432/shogun_sakura";

  @Bean
  public DataSource dataSource(Environment environment) {
    ConnectionSettings settings = resolveConnectionSettings(environment);
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(settings.jdbcUrl());
    config.setUsername(settings.username());
    config.setPassword(settings.password());
    config.setDriverClassName("org.postgresql.Driver");
    return new HikariDataSource(config);
  }

  static ConnectionSettings resolveConnectionSettings(Environment environment) {
    String rawUrl = firstText(
        environment.getProperty("SPRING_DATASOURCE_URL"),
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("DATABASE_INTERNAL_URL"),
        environment.getProperty("RENDER_DATABASE_URL"),
        environment.getProperty("RENDER_DATABASE_INTERNAL_URL"));

    String jdbcUrl = normalizeJdbcUrl(rawUrl);
    String username = firstText(
        extractUserInfo(rawUrl, true),
        environment.getProperty("DATABASE_USERNAME"),
        environment.getProperty("PGUSER"),
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        "postgres");
    String password = firstText(
        extractUserInfo(rawUrl, false),
        environment.getProperty("DATABASE_PASSWORD"),
        environment.getProperty("PGPASSWORD"),
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        "postgres");

    return new ConnectionSettings(jdbcUrl, username, password);
  }

  static String resolveJdbcUrl(Environment environment) {
    return resolveConnectionSettings(environment).jdbcUrl();
  }

  static String resolveUsername(Environment environment) {
    return resolveConnectionSettings(environment).username();
  }

  static String resolvePassword(Environment environment) {
    return resolveConnectionSettings(environment).password();
  }

  static String normalizeJdbcUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return DEFAULT_LOCAL_JDBC_URL;
    }

    String trimmed = value.trim();
    if (trimmed.startsWith("jdbc:")) {
      return trimmed;
    }
    if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
      return trimmed;
    }

    String schemePrefix = trimmed.startsWith("postgresql://") ? "postgresql://" : "postgres://";
    String withoutScheme = trimmed.substring(schemePrefix.length());

    String authority = withoutScheme;
    int firstSlash = withoutScheme.indexOf('/');
    if (firstSlash >= 0) {
      authority = withoutScheme.substring(0, firstSlash);
    }

    String host = authority;
    int atIndex = authority.lastIndexOf('@');
    if (atIndex >= 0) {
      host = authority.substring(atIndex + 1);
    }

    return "jdbc:postgresql://" + host + (firstSlash >= 0 ? withoutScheme.substring(firstSlash) : "/");
  }

  private static String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static String extractUserInfo(String rawUrl, boolean username) {
    if (!StringUtils.hasText(rawUrl)) {
      return null;
    }

    String trimmed = rawUrl.trim();
    if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
      return null;
    }

    String scheme = trimmed.startsWith("postgresql://") ? "postgresql://" : "postgres://";
    URI uri = URI.create("postgres://" + trimmed.substring(scheme.length()));
    String userInfo = uri.getUserInfo();
    if (!StringUtils.hasText(userInfo)) {
      return null;
    }

    int separator = userInfo.indexOf(':');
    if (username) {
      return separator >= 0 ? userInfo.substring(0, separator) : userInfo;
    }
    return separator >= 0 ? userInfo.substring(separator + 1) : null;
  }

  record ConnectionSettings(String jdbcUrl, String username, String password) {
  }
}
