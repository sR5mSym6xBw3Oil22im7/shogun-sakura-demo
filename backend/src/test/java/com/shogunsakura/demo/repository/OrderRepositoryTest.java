package com.shogunsakura.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.model.OrderReceipt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrderRepositoryTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @TempDir
  Path tempDir;

  @Test
  void saveAppendsJsonLineAndReturnsIncrementingId() throws Exception {
    Path storagePath = tempDir.resolve("orders.txt");
    OrderRepository repository = new OrderRepository(OBJECT_MAPPER, storagePath.toString());

    OrderReceipt first = repository.save(
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

    OrderReceipt second = repository.save(
        "SAKURA_SHOGUN_SET",
        "Test Product",
        "Hanako Yamada",
        "hanako@example.com",
        null,
        "Tokyo 2-2-2",
        1,
        4800,
        4800,
        null);

    List<String> lines = Files.readAllLines(storagePath);

    assertThat(first.id()).isEqualTo(1L);
    assertThat(second.id()).isEqualTo(2L);
    assertThat(lines).hasSize(2);
    assertThat(lines.get(0)).contains("\"customerName\":\"Taro Yamada\"");
    assertThat(lines.get(1)).contains("\"customerName\":\"Hanako Yamada\"");
  }

  @Test
  void saveContinuesFromExistingFile() throws Exception {
    Path storagePath = tempDir.resolve("orders.txt");
    Files.writeString(storagePath, """
        {"id":7,"createdAt":"2026-07-08T00:00:00+09:00","productCode":"A"}
        {"id":8,"createdAt":"2026-07-08T00:01:00+09:00","productCode":"B"}
        """.trim() + System.lineSeparator());

    OrderRepository repository = new OrderRepository(OBJECT_MAPPER, storagePath.toString());
    OrderReceipt receipt = repository.save(
        "SAKURA_SHOGUN_SET",
        "Test Product",
        "Taro Yamada",
        "test@example.com",
        null,
        "Tokyo 1-1-1",
        1,
        4800,
        4800,
        null);

    assertThat(receipt.id()).isEqualTo(9L);
  }
}
