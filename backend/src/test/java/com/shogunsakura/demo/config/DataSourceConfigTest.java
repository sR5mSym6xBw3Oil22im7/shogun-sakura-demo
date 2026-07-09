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
        .withProperty("DB_USER", "shogun_sakura_user")
        .withProperty("DB_HOST", "dpg-df9e4ic8aq0b73uvlouq-a")
        .withProperty("DB_PASSWORD", "secret")
        .withProperty("DB_PORT", "5432")
        .withProperty("DB_NAME", "shogun_sakura");

    DataSourceConfig config = new DataSourceConfig();
    HikariDataSource dataSource = (HikariDataSource) config.dataSource(environment);

    assertThat(dataSource.getJdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-df9e4ic8aq0b73uvlouq-a:5432/shogun_sakura?sslmode=require");
    assertThat(dataSource.getUsername()).isEqualTo("shogun_sakura_user");
    assertThat(dataSource.getPassword()).isEqualTo("secret");
    dataSource.close();
  }

  @Test
  void dataSourceFailsWhenEnvironmentVariableIsMissing() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DB_USER", "shogun_sakura_user")
        .withProperty("DB_HOST", "dpg-df9e4ic8aq0b73uvlouq-a")
        .withProperty("DB_PASSWORD", "secret")
        .withProperty("DB_PORT", "5432");

    DataSourceConfig config = new DataSourceConfig();

    assertThrows(IllegalStateException.class, () -> config.dataSource(environment));
  }
}
