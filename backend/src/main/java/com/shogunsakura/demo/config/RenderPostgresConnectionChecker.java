package com.shogunsakura.demo.config;

import java.sql.Connection;
import java.sql.DriverManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RenderPostgresConnectionChecker implements CommandLineRunner {

  private final Environment environment;
  private final ConfigurableApplicationContext applicationContext;

  public RenderPostgresConnectionChecker(Environment environment,
                                         ConfigurableApplicationContext applicationContext) {
    this.environment = environment;
    this.applicationContext = applicationContext;
  }

  @Override
  public void run(String... args) throws Exception {
    String user = required("DB_USER");
    String host = required("DB_HOST");
    String password = required("DB_PASSWORD");
    String port = required("DB_PORT");
    String database = required("DB_NAME");

    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require";

    try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
      connection.close();
    }

    int exitCode = SpringApplication.exit(applicationContext, () -> 0);
    System.exit(exitCode);
  }

  private String required(String key) {
    String value = environment.getProperty(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing environment variable: " + key);
    }
    return value.trim();
  }
}
