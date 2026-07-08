package com.shogunsakura.demo.repository;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

  private static final String INSERT_SQL = """
      INSERT INTO orders (
        product_code,
        product_name,
        customer_name,
        email,
        postal_code,
        address,
        quantity,
        unit_price,
        total_amount,
        note,
        demo_order
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
      RETURNING id, created_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public OrderRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public OrderReceipt save(String productCode,
                           String productName,
                           String customerName,
                           String email,
                           String postalCode,
                           String address,
                           int quantity,
                           int unitPrice,
                           int totalAmount,
                           String note) {
    Object[] params = {
        productCode,
        productName,
        customerName,
        email,
        postalCode,
        address,
        quantity,
        unitPrice,
        totalAmount,
        note
    };

    return jdbcTemplate.queryForObject(INSERT_SQL, this::mapReceipt, params);
  }

  private OrderReceipt mapReceipt(ResultSet rs, int rowNum) throws SQLException {
    OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
    return new OrderReceipt(rs.getLong("id"), createdAt);
  }
}
