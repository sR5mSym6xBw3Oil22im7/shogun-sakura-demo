package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigTest {

  @Test
  void dataSourceUsesRenderEnvironmentVariables() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://dpg-df9e4ic8aq0b73uvlouq-a:5432/shogun_sakura")
        .withProperty("SPRING_DATASOURCE_USERNAME", "shogun_sakura_user")
        .withProperty("SPRING_DATASOURCE_PASSWORD", "secret");

    DataSourceConfig config = new DataSourceConfig();
    HikariDataSource dataSource = (HikariDataSource) config.dataSource(environment);

    assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-df9e4ic8aq0b73uvlouq-a:5432/shogun_sakura");
    assertThat(dataSource.getUsername()).isEqualTo("shogun_sakura_user");
    assertThat(dataSource.getPassword()).isEqualTo("secret");
    dataSource.close();
  }

  @Test
  void dataSourceFailsWhenEnvironmentVariableIsMissing() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/shogun_sakura")
        .withProperty("SPRING_DATASOURCE_USERNAME", "shogun_sakura_user");

    DataSourceConfig config = new DataSourceConfig();

    assertThrows(IllegalStateException.class, () -> config.dataSource(environment));
  }
}
