package com.shogunsakura.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(resolveJdbcUrl(environment));
    config.setUsername(resolveUsername(environment));
    config.setPassword(resolvePassword(environment));
    config.setDriverClassName("org.postgresql.Driver");
    return new HikariDataSource(config);
  }

  static String resolveJdbcUrl(Environment environment) {
    return normalizeJdbcUrl(firstText(
        environment.getProperty("SPRING_DATASOURCE_URL"),
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("DATABASE_INTERNAL_URL"),
        DEFAULT_LOCAL_JDBC_URL));
  }

  static String resolveUsername(Environment environment) {
    return firstText(
        environment.getProperty("DATABASE_USERNAME"),
        environment.getProperty("PGUSER"),
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        "postgres");
  }

  static String resolvePassword(Environment environment) {
    return firstText(
        environment.getProperty("DATABASE_PASSWORD"),
        environment.getProperty("PGPASSWORD"),
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        "postgres");
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

    String pathAndQuery = "/";
    String authority = withoutScheme;
    int firstSlash = withoutScheme.indexOf('/');
    if (firstSlash >= 0) {
      authority = withoutScheme.substring(0, firstSlash);
      pathAndQuery = withoutScheme.substring(firstSlash);
    }

    String query = "";
    int queryIndex = pathAndQuery.indexOf('?');
    if (queryIndex >= 0) {
      query = pathAndQuery.substring(queryIndex + 1);
      pathAndQuery = pathAndQuery.substring(0, queryIndex);
    }

    String host = authority;
    String userInfo = null;
    int atIndex = authority.lastIndexOf('@');
    if (atIndex >= 0) {
      userInfo = authority.substring(0, atIndex);
      host = authority.substring(atIndex + 1);
    }

    StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(host);
    if (!"/".equals(pathAndQuery)) {
      jdbcUrl.append(pathAndQuery);
    } else if (pathAndQuery.isEmpty()) {
      jdbcUrl.append('/');
    }

    StringBuilder queryBuilder = new StringBuilder(query);
    if (StringUtils.hasText(userInfo)) {
      int passwordSeparator = userInfo.indexOf(':');
      String username = userInfo;
      String password = null;
      if (passwordSeparator >= 0) {
        username = userInfo.substring(0, passwordSeparator);
        password = userInfo.substring(passwordSeparator + 1);
      }

      appendQueryParameter(queryBuilder, "user", username);
      if (StringUtils.hasText(password)) {
        appendQueryParameter(queryBuilder, "password", password);
      }
    }

    if (queryBuilder.length() > 0) {
      jdbcUrl.append('?').append(queryBuilder);
    }

    return jdbcUrl.toString();
  }

  private static void appendQueryParameter(StringBuilder queryBuilder, String name, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    if (queryBuilder.length() > 0 && queryBuilder.charAt(queryBuilder.length() - 1) != '&') {
      queryBuilder.append('&');
    }
    queryBuilder.append(name).append('=').append(value);
  }

  private static String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }
}
