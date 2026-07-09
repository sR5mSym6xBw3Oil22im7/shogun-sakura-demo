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

  @Bean
  public DataSource dataSource(Environment environment) {
    String user = required(environment, "DB_USER");
    String host = required(environment, "DB_HOST");
    String password = required(environment, "DB_PASSWORD");
    String port = required(environment, "DB_PORT");
    String database = required(environment, "DB_NAME");

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("org.postgresql.Driver");
    config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require");
    config.setUsername(user);
    config.setPassword(password);
    return new HikariDataSource(config);
  }

  private String required(Environment environment, String key) {
    String value = environment.getProperty(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing environment variable: " + key);
    }
    return value.trim();
  }
}
