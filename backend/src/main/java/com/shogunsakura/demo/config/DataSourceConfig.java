package com.shogunsakura.demo.config;

import java.net.URI;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class DataSourceConfig {

  private static final String DEFAULT_LOCAL_HOST = "localhost";
  private static final String DEFAULT_LOCAL_PORT = "5432";
  private static final String DEFAULT_LOCAL_DATABASE = "shogun_sakura";
  private static final String DEFAULT_LOCAL_USERNAME = "postgres";
  private static final String DEFAULT_LOCAL_PASSWORD = "postgres";

  static ConnectionSettings resolveConnectionSettings(Environment environment) {
    String jdbcUrl = resolveJdbcUrl(environment);
    String username = resolveUsername(environment);
    String password = resolvePassword(environment);
    return new ConnectionSettings(jdbcUrl, username, password);
  }

  static String resolveJdbcUrl(Environment environment) {
    String explicitUrl = firstText(
        environment.getProperty("SPRING_DATASOURCE_URL"),
        environment.getProperty("SPRING_DATASOURCE_UR"),
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("DATABASE_INTERNAL_URL"),
        environment.getProperty("RENDER_DATABASE_URL"),
        environment.getProperty("RENDER_DATABASE_INTERNAL_URL"));
    if (StringUtils.hasText(explicitUrl)) {
      return normalizeJdbcUrl(explicitUrl);
    }

    String host = firstText(
        environment.getProperty("DB_HOST"),
        environment.getProperty("PGHOST"),
        environment.getProperty("DATABASE_HOST"));
    String database = firstText(
        environment.getProperty("DB_NAME"),
        environment.getProperty("PGDATABASE"),
        environment.getProperty("DATABASE_NAME"));
    String port = firstText(
        environment.getProperty("DB_PORT"),
        environment.getProperty("PGPORT"),
        environment.getProperty("DATABASE_PORT"));

    if (StringUtils.hasText(host) || StringUtils.hasText(database) || StringUtils.hasText(port)) {
      String resolvedHost = defaultIfBlank(host, DEFAULT_LOCAL_HOST);
      String resolvedDatabase = defaultIfBlank(database, DEFAULT_LOCAL_DATABASE);
      String resolvedPort = defaultIfBlank(port, DEFAULT_LOCAL_PORT);
      return buildJdbcUrl(resolvedHost, resolvedPort, resolvedDatabase, shouldRequireSsl(resolvedHost));
    }

    return buildJdbcUrl(DEFAULT_LOCAL_HOST, DEFAULT_LOCAL_PORT, DEFAULT_LOCAL_DATABASE, false);
  }

  static String resolveUsername(Environment environment) {
    return firstText(
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        environment.getProperty("PGUSER"),
        environment.getProperty("DATABASE_USERNAME"),
        DEFAULT_LOCAL_USERNAME);
  }

  static String resolvePassword(Environment environment) {
    return firstText(
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        environment.getProperty("PGPASSWORD"),
        environment.getProperty("DATABASE_PASSWORD"),
        DEFAULT_LOCAL_PASSWORD);
  }

  static String normalizeJdbcUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return buildJdbcUrl(DEFAULT_LOCAL_HOST, DEFAULT_LOCAL_PORT, DEFAULT_LOCAL_DATABASE, false);
    }

    String trimmed = value.trim();
    if (trimmed.startsWith("jdbc:postgresql://")) {
      return ensureSslMode(trimmed);
    }
    if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
      return ensureSslMode(convertPostgresUriToJdbc(trimmed));
    }
    return trimmed;
  }

  private static String convertPostgresUriToJdbc(String value) {
    ParsedPostgresUrl parsed = parsePostgresUrl(value);
    return buildJdbcUrl(parsed.host(), parsed.port(), parsed.database(), shouldRequireSsl(parsed.host()));
  }

  private static ParsedPostgresUrl parsePostgresUrl(String value) {
    try {
      URI uri = URI.create(value);
      String host = uri.getHost();
      if (!StringUtils.hasText(host)) {
        String authority = uri.getAuthority();
        if (StringUtils.hasText(authority)) {
          int atIndex = authority.lastIndexOf('@');
          host = atIndex >= 0 ? authority.substring(atIndex + 1) : authority;
        }
      }
      String path = uri.getPath();
      String database = StringUtils.hasText(path) ? path.replaceFirst("^/", "") : DEFAULT_LOCAL_DATABASE;
      String port = uri.getPort() >= 0 ? Integer.toString(uri.getPort()) : DEFAULT_LOCAL_PORT;
      return new ParsedPostgresUrl(host, port, database);
    } catch (IllegalArgumentException ex) {
      return new ParsedPostgresUrl(DEFAULT_LOCAL_HOST, DEFAULT_LOCAL_PORT, DEFAULT_LOCAL_DATABASE);
    }
  }

  private static String buildJdbcUrl(String host, String port, String database, boolean sslRequired) {
    StringBuilder builder = new StringBuilder("jdbc:postgresql://")
        .append(host)
        .append(':')
        .append(port)
        .append('/')
        .append(database);
    if (sslRequired) {
      builder.append("?sslmode=require");
    }
    return builder.toString();
  }

  private static String ensureSslMode(String jdbcUrl) {
    if (!StringUtils.hasText(jdbcUrl)) {
      return jdbcUrl;
    }
    if (jdbcUrl.toLowerCase(Locale.ROOT).contains("sslmode=")) {
      return jdbcUrl;
    }
    if (jdbcUrl.contains("?")) {
      return jdbcUrl + "&sslmode=require";
    }
    return jdbcUrl + "?sslmode=require";
  }

  private static boolean shouldRequireSsl(String host) {
    if (!StringUtils.hasText(host)) {
      return false;
    }

    String normalized = host.toLowerCase(Locale.ROOT);
    return !normalized.equals("localhost") && !normalized.equals("127.0.0.1");
  }

  private static String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static String defaultIfBlank(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }

  record ConnectionSettings(String jdbcUrl, String username, String password) {
  }

  private record ParsedPostgresUrl(
      String host,
      String port,
      String database) {
  }
}
