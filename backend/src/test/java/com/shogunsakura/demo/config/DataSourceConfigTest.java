package com.shogunsakura.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigTest {

  @Test
  void resolveConnectionSettingsUsesPgVariablesAndAddsSslMode() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("PGHOST", "dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com")
        .withProperty("PGPORT", "5432")
        .withProperty("PGDATABASE", "shogun_sakura")
        .withProperty("PGUSER", "shogun_sakura_user")
        .withProperty("PGPASSWORD", "secret");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura?sslmode=require");
    assertThat(settings.username()).isEqualTo("shogun_sakura_user");
    assertThat(settings.password()).isEqualTo("secret");
  }

  @Test
  void resolveConnectionSettingsUsesDatabaseUrlAndKeepsSslMode() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty(
            "DATABASE_URL",
            "postgresql://shogun_sakura_user:secret@dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura?sslmode=require");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-d96dt3eq1p3s73bvlueg-a.oregon-postgres.render.com:5432/shogun_sakura?sslmode=require");
    assertThat(settings.username()).isEqualTo("shogun_sakura_user");
    assertThat(settings.password()).isEqualTo("secret");
  }

  @Test
  void resolveConnectionSettingsUsesRenderWebServiceVariables() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("DB_HOST", "dpg-df9e4ic8aq0b73uv1ouq-a")
        .withProperty("DB_NAME", "shogun_sakura")
        .withProperty("DB_PORT", "5432")
        .withProperty("SPRING_DATASOURCE_USERNAME", "shogun_sakura_user")
        .withProperty("SPRING_DATASOURCE_PASSWORD", "secret");

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl())
        .isEqualTo("jdbc:postgresql://dpg-df9e4ic8aq0b73uv1ouq-a:5432/shogun_sakura?sslmode=require");
    assertThat(settings.username()).isEqualTo("shogun_sakura_user");
    assertThat(settings.password()).isEqualTo("secret");
  }

  @Test
  void resolveConnectionSettingsFallsBackToLocalDefaults() {
    MockEnvironment environment = new MockEnvironment();

    DataSourceConfig.ConnectionSettings settings = DataSourceConfig.resolveConnectionSettings(environment);

    assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/shogun_sakura");
    assertThat(settings.username()).isEqualTo("postgres");
    assertThat(settings.password()).isEqualTo("postgres");
  }

  @Test
  void resolveConnectionSettingsFailsFastOnRenderWithoutDatabaseConfig() {
    MockEnvironment environment = new MockEnvironment()
        .withProperty("PORT", "10000");

    assertThatThrownBy(() -> DataSourceConfig.resolveConnectionSettings(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing PostgreSQL configuration for Render");
  }
}
