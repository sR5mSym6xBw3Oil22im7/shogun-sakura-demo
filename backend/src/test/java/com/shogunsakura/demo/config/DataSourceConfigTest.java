package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigTest {

  @Test
  void resolveConnectionSettingsUsesRenderDatabaseVariables() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DB_HOST", "dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com")
        .withProperty("DB_NAME", "shogun_sakura")
        .withProperty("DB_PORT", "5432")
        .withProperty("SPRING_DATASOURCE_USERNAME", "shogun_sakura_user")
        .withProperty("SPRING_DATASOURCE_PASSWORD", "secret");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura?sslmode=require");
    assertThat(settings.username()).isEqualTo("shogun_sakura_user");
    assertThat(settings.password()).isEqualTo("secret");
  }

  @Test
  void resolveConnectionSettingsUsesDatasourceUrlWhenProvided() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty(
            "SPRING_DATASOURCE_URL",
            "jdbc:postgresql://dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura")
        .withProperty("SPRING_DATASOURCE_USERNAME", "shogun_sakura_user")
        .withProperty("SPRING_DATASOURCE_PASSWORD", "secret");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura?sslmode=require");
  }

  @Test
  void resolveConnectionSettingsFallsBackToLocalDefaults() {
    MockEnvironment environment = new MockEnvironment();

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/shogun_sakura");
    assertThat(settings.username()).isEqualTo("postgres");
    assertThat(settings.password()).isEqualTo("postgres");
  }
}
