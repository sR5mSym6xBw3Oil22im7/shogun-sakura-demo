package com.shogunsakura.demo.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.model.OrderReceipt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

  private static final ZoneId TOKYO_ZONE = ZoneId.of("Asia/Tokyo");

  private final ObjectMapper objectMapper;
  private final Path storagePath;
  private final AtomicLong nextId;

  public OrderRepository(
      ObjectMapper objectMapper,
      @Value("${app.order-storage.path:./data/orders.txt}") String storagePath) {
    this.objectMapper = objectMapper;
    this.storagePath = Paths.get(storagePath);
    this.nextId = new AtomicLong(loadNextId());
  }

  public synchronized OrderReceipt save(String productCode,
                                        String productName,
                                        String customerName,
                                        String email,
                                        String postalCode,
                                        String address,
                                        int quantity,
                                        int unitPrice,
                                        int totalAmount,
                                        String note) {
    long id = nextId.getAndIncrement();
    OffsetDateTime createdAt = OffsetDateTime.now(TOKYO_ZONE);
    OrderEntry entry = new OrderEntry(
        id,
        createdAt,
        productCode,
        productName,
        customerName,
        email,
        postalCode,
        address,
        quantity,
        unitPrice,
        totalAmount,
        note);

    appendEntry(entry);
    return new OrderReceipt(id, createdAt);
  }

  private long loadNextId() {
    if (!Files.exists(storagePath)) {
      return 1L;
    }

    long maxId = 0L;
    try {
      for (String line : Files.readAllLines(storagePath, StandardCharsets.UTF_8)) {
        if (!line.isBlank()) {
          try {
            JsonNode node = objectMapper.readTree(line);
            maxId = Math.max(maxId, node.path("id").asLong(0L));
          } catch (IOException ignored) {
            // Skip malformed lines and keep loading the rest of the file.
          }
        }
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to read order storage", ex);
    }

    return maxId + 1L;
  }

  private void appendEntry(OrderEntry entry) {
    try {
      Path parent = storagePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      String json = objectMapper.writeValueAsString(entry);
      Files.writeString(
          storagePath,
          json + System.lineSeparator(),
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.APPEND);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write order storage", ex);
    }
  }

  private record OrderEntry(
      long id,
      OffsetDateTime createdAt,
      String productCode,
      String productName,
      String customerName,
      String email,
      String postalCode,
      String address,
      int quantity,
      int unitPrice,
      int totalAmount,
      String note) {
  }
}
