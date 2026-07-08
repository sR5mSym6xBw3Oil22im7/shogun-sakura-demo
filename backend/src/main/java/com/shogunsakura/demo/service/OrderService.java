package com.shogunsakura.demo.service;

import com.shogunsakura.demo.dto.CreateOrderRequest;
import com.shogunsakura.demo.dto.OrderResponse;
import com.shogunsakura.demo.model.OrderReceipt;
import com.shogunsakura.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private static final String PRODUCT_CODE = "SAKURA_SHOGUN_SET";
  private static final String PRODUCT_NAME = "桜の押し花と白扇「将軍」セット";
  private static final int UNIT_PRICE = 4800;

  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public OrderResponse createOrder(CreateOrderRequest request) {
    int totalAmount = UNIT_PRICE * request.quantity();
    OrderReceipt receipt = orderRepository.save(
        PRODUCT_CODE,
        PRODUCT_NAME,
        request.name().trim(),
        request.email().trim(),
        normalizeOptional(request.postalCode()),
        request.address().trim(),
        request.quantity(),
        UNIT_PRICE,
        totalAmount,
        normalizeOptional(request.note()));

    return new OrderResponse(receipt.id(), "注文を受け付けました。", receipt.createdAt());
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
