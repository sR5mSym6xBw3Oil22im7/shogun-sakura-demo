package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

  @Mock
  JdbcTemplate jdbcTemplate;

  @Test
  void saveMapsInsertedOrderToReceipt() throws Exception {
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-08T12:34:56+09:00");

    when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          RowMapper<OrderReceipt> rowMapper = invocation.getArgument(1);
          ResultSet resultSet = mock(ResultSet.class);
          when(resultSet.getLong("id")).thenReturn(101L);
          when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(createdAt);
          return rowMapper.mapRow(resultSet, 0);
        });

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

    assertThat(receipt.id()).isEqualTo(101L);
    assertThat(receipt.createdAt()).isEqualTo(createdAt);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).queryForObject(
        sqlCaptor.capture(),
        any(RowMapper.class),
        eq("SAKURA_SHOGUN_SET"),
        eq("SHOGUN SAKURA Demo Set"),
        eq("Taro Yamada"),
        eq("taro@example.com"),
        eq("100-0001"),
        eq("Tokyo 1-1-1"),
        eq(2),
        eq(4800),
        eq(9600),
        eq("Demo order"));

    assertThat(sqlCaptor.getValue())
        .contains("WITH sequence_reset AS")
        .contains("NOT EXISTS (SELECT 1 FROM orders)")
        .contains("setval(pg_get_serial_sequence('orders', 'id'), 1, false)");
  }
}
