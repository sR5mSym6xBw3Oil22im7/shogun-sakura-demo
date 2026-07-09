package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.shogunsakura.demo.model.OrderReceipt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class OrderRepositoryTest {

  @Test
  void saveReturnsGeneratedId() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(new NoOpDataSource()) {
      @Override
      public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) {
        ((GeneratedKeyHolder) generatedKeyHolder).getKeyList().add(Map.of("id", 7L));
        return 1;
      }
    };

    OrderRepository repository = new OrderRepository(jdbcTemplate);
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

    assertThat(receipt.id()).isEqualTo(7L);
    assertThat(receipt.createdAt()).isNotNull();
  }

  private static final class NoOpDataSource implements DataSource {
    @Override
    public Connection getConnection() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection(String username, String password) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }

    @Override
    public java.io.PrintWriter getLogWriter() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout(int seconds) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
      throw new UnsupportedOperationException();
    }
  }
}
