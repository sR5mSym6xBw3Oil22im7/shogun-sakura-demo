package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;

class OrderRepositoryTest {

  @Test
  void saveUsesJdbcTemplateAndReturnsGeneratedId() {
    JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    OrderRepository repository = new OrderRepository(jdbcTemplate);

    when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(GeneratedKeyHolder.class)))
        .thenAnswer(invocation -> {
          GeneratedKeyHolder keyHolder = invocation.getArgument(1);
          keyHolder.getKeyList().add(java.util.Map.of("id", 1L));
          return 1;
        });

    OrderReceipt receipt = repository.save(
        "SAKURA_SHOGUN_SET",
        "Test Product",
        "Taro Yamada",
        "test@example.com",
        "100-0001",
        "Tokyo 1-1-1",
        2,
        4800,
        9600,
        "First order");

    assertThat(receipt.id()).isEqualTo(1L);
    assertThat(receipt.createdAt()).isNotNull();
  }
}
