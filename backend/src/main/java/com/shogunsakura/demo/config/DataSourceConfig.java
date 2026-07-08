package com.shogunsakura.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfig {

  private static final String DEFAULT_LOCAL_HOST = "localhost";
  private static final String DEFAULT_LOCAL_PORT = "5432";
  private static final String DEFAULT_LOCAL_DATABASE = "shogun_sakura";
  private static final String DEFAULT_LOCAL_USERNAME = "postgres";
  private static final String DEFAULT_LOCAL_PASSWORD = "postgres";

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
    ConnectionSettings fromPgVariables = resolveFromPgVariables(environment);
    if (fromPgVariables != null) {
      return fromPgVariables;
    }

    ConnectionSettings fromDatabaseUrl = resolveFromDatabaseUrl(environment);
    if (fromDatabaseUrl != null) {
      return fromDatabaseUrl;
    }

    return new ConnectionSettings(
        buildJdbcUrl(DEFAULT_LOCAL_HOST, DEFAULT_LOCAL_PORT, DEFAULT_LOCAL_DATABASE, false),
        DEFAULT_LOCAL_USERNAME,
        DEFAULT_LOCAL_PASSWORD);
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

  private static ConnectionSettings resolveFromPgVariables(Environment environment) {
    String host = firstText(
        environment.getProperty("PGHOST"),
        environment.getProperty("DB_HOST"));
    String database = firstText(
        environment.getProperty("PGDATABASE"),
        environment.getProperty("DB_NAME"));
    String username = firstText(
        environment.getProperty("PGUSER"),
        environment.getProperty("DB_USER"),
        environment.getProperty("DATABASE_USERNAME"));
    String password = firstText(
        environment.getProperty("PGPASSWORD"),
        environment.getProperty("DB_PASSWORD"),
        environment.getProperty("DATABASE_PASSWORD"));
    String port = firstText(
        environment.getProperty("PGPORT"),
        environment.getProperty("DB_PORT"));

    if (!StringUtils.hasText(host)
        && !StringUtils.hasText(database)
        && !StringUtils.hasText(username)
        && !StringUtils.hasText(password)
        && !StringUtils.hasText(port)) {
      return null;
    }

    String resolvedHost = defaultIfBlank(host, DEFAULT_LOCAL_HOST);
    String resolvedPort = defaultIfBlank(port, DEFAULT_LOCAL_PORT);
    String resolvedDatabase = defaultIfBlank(database, DEFAULT_LOCAL_DATABASE);
    String resolvedUsername = defaultIfBlank(username, DEFAULT_LOCAL_USERNAME);
    String resolvedPassword = defaultIfBlank(password, DEFAULT_LOCAL_PASSWORD);
    String jdbcUrl = buildJdbcUrl(
        resolvedHost,
        resolvedPort,
        resolvedDatabase,
        shouldRequireSsl(resolvedHost));

    return new ConnectionSettings(jdbcUrl, resolvedUsername, resolvedPassword);
  }

  private static ConnectionSettings resolveFromDatabaseUrl(Environment environment) {
    String rawUrl = firstText(
        environment.getProperty("SPRING_DATASOURCE_URL"),
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("DATABASE_INTERNAL_URL"),
        environment.getProperty("RENDER_DATABASE_URL"),
        environment.getProperty("RENDER_DATABASE_INTERNAL_URL"));

    if (!StringUtils.hasText(rawUrl)) {
      return null;
    }

    String trimmed = rawUrl.trim();
    if (trimmed.startsWith("jdbc:postgresql://")) {
      return new ConnectionSettings(
          ensureSslMode(trimmed),
          defaultIfBlank(firstText(
              environment.getProperty("DATABASE_USERNAME"),
              environment.getProperty("PGUSER"),
              environment.getProperty("SPRING_DATASOURCE_USERNAME")), DEFAULT_LOCAL_USERNAME),
          defaultIfBlank(firstText(
              environment.getProperty("DATABASE_PASSWORD"),
              environment.getProperty("PGPASSWORD"),
              environment.getProperty("SPRING_DATASOURCE_PASSWORD")), DEFAULT_LOCAL_PASSWORD));
    }

    if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
      return null;
    }

    ParsedPostgresUrl parsed = parsePostgresUrl(trimmed);
    String jdbcUrl = buildJdbcUrl(
        parsed.host(),
        parsed.port(),
        parsed.database(),
        shouldRequireSsl(parsed.host()));

    String username = firstText(
        parsed.username(),
        environment.getProperty("DATABASE_USERNAME"),
        environment.getProperty("PGUSER"),
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        DEFAULT_LOCAL_USERNAME);
    String password = firstText(
        parsed.password(),
        environment.getProperty("DATABASE_PASSWORD"),
        environment.getProperty("PGPASSWORD"),
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        DEFAULT_LOCAL_PASSWORD);

    return new ConnectionSettings(jdbcUrl, username, password);
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
      String userInfo = uri.getUserInfo();
      String username = null;
      String password = null;
      if (StringUtils.hasText(userInfo)) {
        int separator = userInfo.indexOf(':');
        username = separator >= 0 ? userInfo.substring(0, separator) : userInfo;
        password = separator >= 0 ? userInfo.substring(separator + 1) : null;
      }
      String port = uri.getPort() >= 0 ? Integer.toString(uri.getPort()) : DEFAULT_LOCAL_PORT;
      return new ParsedPostgresUrl(host, port, database, username, password);
    } catch (IllegalArgumentException ex) {
      return new ParsedPostgresUrl(DEFAULT_LOCAL_HOST, DEFAULT_LOCAL_PORT, DEFAULT_LOCAL_DATABASE, null, null);
    }
  }

  private static String convertPostgresUriToJdbc(String value) {
    ParsedPostgresUrl parsed = parsePostgresUrl(value);
    return buildJdbcUrl(parsed.host(), parsed.port(), parsed.database(), shouldRequireSsl(parsed.host()));
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
      String database,
      String username,
      String password) {
  }
}
