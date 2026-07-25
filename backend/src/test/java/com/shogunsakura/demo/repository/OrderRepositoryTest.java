package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.shogunsakura.demo.config.DataSourceConfig;
import com.shogunsakura.demo.dto.OrderHistoryResponse;
import com.shogunsakura.demo.model.OrderReceipt;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class OrderRepositoryTest {

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
  void saveInsertsOrderAndReturnsGeneratedReceipt() {
    OrderRepository repository = new OrderRepository(jdbcTemplate);

    OrderReceipt receipt = repository.save(
        "SAKURA_SHOGUN_SET",
        "SHOGUN SAKURA Demo Set",
        "Taro Yamada",
        "taro@example.com",
        "100-0001",
        "Tokyo 1-1-1",
        2,
        4800,
        9600,
        "Demo order");

    assertThat(receipt.id()).isGreaterThanOrEqualTo(1L);
    assertThat(receipt.createdAt()).isNotNull();

    List<OrderHistoryResponse> history = new OrderHistoryRepository(jdbcTemplate).findAll();
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().customerName()).isEqualTo("Taro Yamada");
    assertThat(history.getFirst().email()).isEqualTo("taro@example.com");
    assertThat(history.getFirst().quantity()).isEqualTo(2);
    assertThat(history.getFirst().totalAmount()).isEqualTo(9600);
  }

  @Test
  void findAllReturnsRowsInIdAscendingOrder() {
    OrderRepository repository = new OrderRepository(jdbcTemplate);

    repository.save(
        "SAKURA_SHOGUN_SET",
        "SHOGUN SAKURA Demo Set",
        "First Customer",
        "first@example.com",
        "100-0001",
        "Tokyo 1-1-1",
        1,
        4800,
        4800,
        "First order");
    repository.save(
        "SAKURA_SHOGUN_SET",
        "SHOGUN SAKURA Demo Set",
        "Second Customer",
        "second@example.com",
        "100-0002",
        "Tokyo 2-2-2",
        3,
        4800,
        14400,
        "Second order");

    List<OrderHistoryResponse> history = new OrderHistoryRepository(jdbcTemplate).findAll();
    assertThat(history).hasSize(2);
    assertThat(history).extracting(OrderHistoryResponse::customerName)
        .containsExactly("First Customer", "Second Customer");
    assertThat(history).extracting(OrderHistoryResponse::email)
        .containsExactly("first@example.com", "second@example.com");
    assertThat(history).extracting(OrderHistoryResponse::quantity)
        .containsExactly(1, 3);
  }
}
