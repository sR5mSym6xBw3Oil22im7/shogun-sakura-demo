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
    if (trimmed.startsWith("postgres://")) {
      return "jdbc:postgresql://" + trimmed.substring("postgres://".length());
    }
    if (trimmed.startsWith("postgresql://")) {
      return "jdbc:postgresql://" + trimmed.substring("postgresql://".length());
    }
    return trimmed;
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
