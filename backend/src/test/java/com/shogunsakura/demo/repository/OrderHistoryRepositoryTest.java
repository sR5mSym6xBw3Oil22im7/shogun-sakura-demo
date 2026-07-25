package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.shogunsakura.demo.config.DataSourceConfig;
import com.shogunsakura.demo.dto.OrderHistoryResponse;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class OrderHistoryRepositoryTest {

  private HikariDataSource dataSource;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    DataSourceConfig config = new DataSourceConfig();
    dataSource = (HikariDataSource) config.dataSource();
    jdbcTemplate = new JdbcTemplate(dataSource);
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
    jdbcTemplate.execute("DELETE FROM orders");
  }

  @AfterEach
  void tearDown() {
    dataSource.close();
  }

  @Test
  void findAllMapsRowsToHistoryResponses() {
    new OrderRepository(jdbcTemplate).save(
        "SAKURA_SHOGUN_SET",
        "SHOGUN SAKURA Demo Set",
        "Taro Yamada",
        "test@example.com",
        "100-0001",
        "Tokyo 1-1-1",
        2,
        4800,
        9600,
        "First order");

    OrderHistoryRepository repository = new OrderHistoryRepository(jdbcTemplate);
    List<OrderHistoryResponse> items = repository.findAll();

    assertThat(items).hasSize(1);
    assertThat(items.getFirst().orderId()).isGreaterThanOrEqualTo(1L);
    assertThat(items.getFirst().productName()).isEqualTo("SHOGUN SAKURA Demo Set");
    assertThat(items.getFirst().customerName()).isEqualTo("Taro Yamada");
    assertThat(items.getFirst().email()).isEqualTo("test@example.com");
    assertThat(items.getFirst().quantity()).isEqualTo(2);
    assertThat(items.getFirst().totalAmount()).isEqualTo(9600);
  }
}
