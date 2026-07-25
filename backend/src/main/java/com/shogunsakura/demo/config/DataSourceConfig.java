package com.shogunsakura.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

  private static final String JDBC_URL =
      "jdbc:h2:mem:shogun_sakura;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=false";

  @Bean
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setDriverClassName("org.h2.Driver");
    config.setJdbcUrl(JDBC_URL);
    config.setUsername("sa");
    config.setPassword("");
    return new HikariDataSource(config);
  }
}
