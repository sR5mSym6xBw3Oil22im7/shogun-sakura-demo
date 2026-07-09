package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shogunsakura.demo.dto.OrderHistoryResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OrderHistoryRepositoryTest {

  @Test
  void findAllMapsRowsToHistoryResponses() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(Map.of(
        "id", 3L,
        "product_name", "Test Product",
        "customer_name", "Taro Yamada",
        "email", "test@example.com",
        "postal_code", "100-0001",
        "address", "Tokyo 1-1-1",
        "quantity", 2,
        "total_amount", 9600,
        "note", "First order",
        "created_at", OffsetDateTime.parse("2026-07-09T20:59:18.246321+09:00")
    )));

    OrderHistoryRepository repository = new OrderHistoryRepository(jdbcTemplate);
    List<OrderHistoryResponse> items = repository.findAll();

    assertThat(items).hasSize(1);
    assertThat(items.getFirst().orderId()).isEqualTo(3L);
    assertThat(items.getFirst().productName()).isEqualTo("Test Product");
    assertThat(items.getFirst().customerName()).isEqualTo("Taro Yamada");
    assertThat(items.getFirst().totalAmount()).isEqualTo(9600);
  }
}
