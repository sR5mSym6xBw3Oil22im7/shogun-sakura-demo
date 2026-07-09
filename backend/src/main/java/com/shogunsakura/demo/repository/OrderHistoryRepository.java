package com.shogunsakura.demo.repository;

import com.shogunsakura.demo.dto.OrderHistoryResponse;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderHistoryRepository {

  private static final ZoneId TOKYO_ZONE = ZoneId.of("Asia/Tokyo");
  private static final String SELECT_ALL_SQL = """
      SELECT
        id,
        product_name,
        customer_name,
        email,
        postal_code,
        address,
        quantity,
        total_amount,
        note,
        created_at
      FROM orders
      ORDER BY created_at DESC, id DESC
      """;

  private final JdbcTemplate jdbcTemplate;

  public OrderHistoryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<OrderHistoryResponse> findAll() {
    return jdbcTemplate.queryForList(SELECT_ALL_SQL).stream()
        .map(this::toOrderHistoryResponse)
        .toList();
  }

  private OrderHistoryResponse toOrderHistoryResponse(Map<String, Object> row) {
    return new OrderHistoryResponse(
        toLong(row.get("id")),
        toStringValue(row.get("product_name")),
        toStringValue(row.get("customer_name")),
        toStringValue(row.get("email")),
        toStringValue(row.get("postal_code")),
        toStringValue(row.get("address")),
        toInt(row.get("quantity")),
        toInt(row.get("total_amount")),
        toStringValue(row.get("note")),
        toOffsetDateTime(row.get("created_at")));
  }

  private long toLong(Object value) {
    return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
  }

  private int toInt(Object value) {
    return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
  }

  private String toStringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private OffsetDateTime toOffsetDateTime(Object value) {
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().atZone(TOKYO_ZONE).toOffsetDateTime();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(TOKYO_ZONE).toOffsetDateTime();
    }
    return value == null ? null : OffsetDateTime.parse(String.valueOf(value));
  }
}
