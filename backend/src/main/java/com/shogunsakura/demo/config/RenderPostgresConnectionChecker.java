package com.shogunsakura.demo.config;

import java.sql.Connection;
import java.sql.DriverManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RenderPostgresConnectionChecker implements CommandLineRunner {

  private final Environment environment;

  public RenderPostgresConnectionChecker(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void run(String... args) throws Exception {
    String jdbcUrl = required("SPRING_DATASOURCE_URL");
    String user = required("SPRING_DATASOURCE_USERNAME");
    String password = required("SPRING_DATASOURCE_PASSWORD");

    try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
      connection.close();
    }
  }

  private String required(String key) {
    String value = environment.getProperty(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing environment variable: " + key);
    }
    return value.trim();
  }
}
