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
    String jdbcUrl = environment.getProperty("SPRING_DATASOURCE_URL",
        "jdbc:h2:mem:shogun_sakura;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
    String user = environment.getProperty("SPRING_DATASOURCE_USERNAME", "sa");
    String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD", "");

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("org.h2.Driver");
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(user);
    config.setPassword(password);
    config.setInitializationFailTimeout(-1);
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
