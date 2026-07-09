package com.shogunsakura.demo.repository;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

  private static final ZoneId TOKYO_ZONE = ZoneId.of("Asia/Tokyo");
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
        demo_order,
        created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?)
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
    OffsetDateTime createdAt = OffsetDateTime.now(TOKYO_ZONE);
    KeyHolder keyHolder = new GeneratedKeyHolder();

    int updated = jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, productCode);
      statement.setString(2, productName);
      statement.setString(3, customerName);
      statement.setString(4, email);
      statement.setString(5, postalCode);
      statement.setString(6, address);
      statement.setInt(7, quantity);
      statement.setInt(8, unitPrice);
      statement.setInt(9, totalAmount);
      statement.setString(10, note);
      statement.setObject(11, createdAt);
      return statement;
    }, keyHolder);

    if (updated != 1 || keyHolder.getKey() == null) {
      throw new IllegalStateException("Failed to save order.");
    }

    return new OrderReceipt(keyHolder.getKey().longValue(), createdAt);
  }
}
