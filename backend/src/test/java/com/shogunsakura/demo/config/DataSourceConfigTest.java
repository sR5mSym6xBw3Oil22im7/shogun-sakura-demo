package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.jupiter.api.Test;

class DataSourceConfigTest {

  @Test
  void dataSourceUsesH2InMemoryDatabase() throws Exception {
    DataSourceConfig config = new DataSourceConfig();
    HikariDataSource dataSource = (HikariDataSource) config.dataSource();

    try (Connection connection = dataSource.getConnection()) {
      assertThat(dataSource.getJdbcUrl())
          .startsWith("jdbc:h2:mem:shogun_sakura");
      assertThat(dataSource.getDriverClassName()).isEqualTo("org.h2.Driver");
      assertThat(dataSource.getUsername()).isEqualTo("sa");
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
      assertThat(connection.isValid(1)).isTrue();
    } finally {
      dataSource.close();
    }
  }
}
