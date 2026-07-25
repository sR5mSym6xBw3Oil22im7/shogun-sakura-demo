package com.shogunsakura.demo.repository;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
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
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, productCode);
      ps.setString(2, productName);
      ps.setString(3, customerName);
      ps.setString(4, email);
      ps.setString(5, postalCode);
      ps.setString(6, address);
      ps.setInt(7, quantity);
      ps.setInt(8, unitPrice);
      ps.setInt(9, totalAmount);
      ps.setString(10, note);
      return ps;
    }, keyHolder);

    Map<String, Object> generatedKeys = keyHolder.getKeys();
    Object generatedId = generatedKeys == null ? null : generatedKeys.get("id");
    if (!(generatedId instanceof Number number)) {
      throw new IllegalStateException("Failed to retrieve generated order id.");
    }
    long id = number.longValue();

    OffsetDateTime createdAt = jdbcTemplate.queryForObject(
        "SELECT created_at FROM orders WHERE id = ?",
        (rs, rowNum) -> rs.getObject("created_at", OffsetDateTime.class),
        id);
    return new OrderReceipt(id, Objects.requireNonNull(createdAt, "created_at is required"));
  }
}
